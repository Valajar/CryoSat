package io.github.valajar.cryosat.client.locator;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.registries.CryoItems;
import io.github.valajar.cryosat.space.LocatorData;
import io.github.valajar.cryosat.space.locator.ItemSurveyLocator;
import mekanism.client.ClientRegistrationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.GlobalPos;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Client side of the Survey Locator: the compass needle and the waypoint beam.
 * <ul>
 *     <li>The needle is vanilla's compass logic ({@link CompassItemPropertyFunction}) aimed at the selected target, exposed as
 *     the {@code cryosat:angle} item property that the item model's overrides switch frames on. It spins when there is no
 *     target or the target is in another dimension.</li>
 *     <li>While the player holds a locator whose target is in this dimension, a beacon style Cryo Cobalt beam stands at the
 *     target. It is drawn without fog, and a target past twice the render distance is drawn at that distance in its true
 *     direction, so it can be seen from anywhere. A target with an uncertainty radius also gets its search circle traced
 *     faintly on the ground, only where the client already has the chunk: nothing here loads chunks.</li>
 * </ul>
 */
@EventBusSubscriber(modid = CryoSat.MODID, value = Dist.CLIENT)
public final class SurveyLocatorClient {
    /** Cryo glow, {@code CRYO_GLOW[1]} in the texture palette. */
    private static final int BEAM_RGB = 0x6D8CFF;
    private static final int CORE_ALPHA = 0xB0;
    private static final int GLOW_ALPHA = 0x30;
    private static final int CIRCLE_ALPHA = 0x60;
    /** Beam radius in blocks up close, and how it widens with distance so it stays a few pixels wide far away. */
    private static final float MIN_RADIUS = 0.25F;
    private static final float RADIUS_PER_BLOCK = 0.004F;
    private static final float GLOW_SCALE = 1.6F;
    /** Height above the beam's top of the world the beam reaches, like a beacon's 1024 blocks. */
    private static final int ABOVE_WORLD = 512;
    private static final float CIRCLE_HEIGHT = 1.5F;
    private static final float CIRCLE_SEGMENT = 2;
    private static final int FULL_BRIGHT = 0xF000F0;

    /** Called from {@code CryoSatClient}'s client setup. */
    public static void registerNeedle() {
        ClientRegistrationUtil.setPropertyOverride(CryoItems.SURVEY_LOCATOR, ItemSurveyLocator.NEEDLE_ANGLE, new CompassItemPropertyFunction((level, stack, entity) -> target(stack)));
    }

    @Nullable
    private static GlobalPos target(ItemStack stack) {
        LocatorData.Entry entry = ItemSurveyLocator.getData(stack).current();
        return entry == null ? null : entry.pos();
    }

    @Nullable
    private static LocatorData.Entry heldTarget(LocalPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(CryoItems.SURVEY_LOCATOR)) {
                LocatorData.Entry entry = ItemSurveyLocator.getData(stack).current();
                if (entry != null) {
                    return entry;
                }
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        // After block entities and before translucent terrain, where vanilla draws beacon beams
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            return;
        }
        LocatorData.Entry entry = heldTarget(player);
        if (entry == null || entry.pos().dimension() != level.dimension()) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        double centreX = entry.pos().pos().getX() + 0.5;
        double centreZ = entry.pos().pos().getZ() + 0.5;
        float viewDistance = minecraft.gameRenderer.getRenderDistance();
        float time = Math.floorMod(level.getGameTime(), 40) + event.getPartialTick().getGameTimeDeltaPartialTick(false);

        RenderType type = RenderType.beaconBeam(BeaconRenderer.BEAM_LOCATION, true);
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer buffer = buffers.getBuffer(type);
        PoseStack poseStack = event.getPoseStack();

        // Beam, pulled in to twice the view distance when further, in the same direction
        double dx = centreX - camera.x;
        double dz = centreZ - camera.z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        double maxDistance = viewDistance * 2;
        if (distance > maxDistance) {
            dx *= maxDistance / distance;
            dz *= maxDistance / distance;
        }
        float radius = Math.max(MIN_RADIUS, (float) Math.min(distance, maxDistance) * RADIUS_PER_BLOCK);
        poseStack.pushPose();
        poseStack.translate(dx, -camera.y, dz);
        beam(poseStack, buffer, level.getMinBuildHeight(), level.getMaxBuildHeight() + ABOVE_WORLD, radius, time);
        poseStack.popPose();

        if (entry.uncertainty() > 0) {
            poseStack.pushPose();
            poseStack.translate(-camera.x, -camera.y, -camera.z);
            searchCircle(poseStack, buffer, level, camera, centreX, centreZ, entry.uncertainty(), viewDistance);
            poseStack.popPose();
        }

        // A waypoint, not scenery: no fog, so it shows past the render distance
        float fogStart = RenderSystem.getShaderFogStart();
        RenderSystem.setShaderFogStart(Float.MAX_VALUE);
        buffers.endBatch(type);
        RenderSystem.setShaderFogStart(fogStart);
    }

    /** Vanilla's beacon beam made translucent: a slowly turning core inside a wider, fainter glow. */
    private static void beam(PoseStack poseStack, VertexConsumer buffer, int minY, int maxY, float radius, float time) {
        int height = maxY - minY;
        float scroll = Mth.frac(-time * 0.2F - Mth.floor(-time * 0.1F));
        float minV = scroll - 1;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 2.25F - 45));
        prism(poseStack.last(), buffer, FastColor.ARGB32.color(CORE_ALPHA, BEAM_RGB), minY, maxY, radius, minV, height * (0.5F / radius) + minV);
        poseStack.popPose();
        prism(poseStack.last(), buffer, FastColor.ARGB32.color(GLOW_ALPHA, BEAM_RGB), minY, maxY, radius * GLOW_SCALE, minV, height + minV);
    }

    /** The four sides of a square prism round the origin, corners on the axes, as vanilla's beam draws them. */
    private static void prism(PoseStack.Pose pose, VertexConsumer buffer, int color, int minY, int maxY, float r, float minV, float maxV) {
        quad(pose, buffer, color, color, minY, maxY, 0, r, r, 0, minV, maxV);
        quad(pose, buffer, color, color, minY, maxY, 0, -r, -r, 0, minV, maxV);
        quad(pose, buffer, color, color, minY, maxY, r, 0, 0, -r, minV, maxV);
        quad(pose, buffer, color, color, minY, maxY, -r, 0, 0, r, minV, maxV);
    }

    /**
     * Traces the search circle as a low wall that fades upward, following the ground where the chunk is loaded on the client
     * and within view; segments over chunks the client does not have are left out.
     */
    private static void searchCircle(PoseStack poseStack, VertexConsumer buffer, ClientLevel level, Vec3 camera, double centreX, double centreZ, int radius,
          float viewDistance) {
        PoseStack.Pose pose = poseStack.last();
        int bottom = FastColor.ARGB32.color(CIRCLE_ALPHA, BEAM_RGB);
        int top = FastColor.ARGB32.color(0, BEAM_RGB);
        int segments = Mth.clamp(Mth.ceil(Mth.TWO_PI * radius / CIRCLE_SEGMENT), 24, 2048);
        double viewSqr = (double) viewDistance * viewDistance;
        double prevX = centreX + radius;
        double prevZ = centreZ;
        for (int i = 1; i <= segments; i++) {
            double angle = Mth.TWO_PI * i / segments;
            double x = centreX + radius * Math.cos(angle);
            double z = centreZ + radius * Math.sin(angle);
            double midX = (prevX + x) / 2 - camera.x;
            double midZ = (prevZ + z) / 2 - camera.z;
            if (midX * midX + midZ * midZ < viewSqr && level.hasChunkAt(Mth.floor(prevX), Mth.floor(prevZ)) && level.hasChunkAt(Mth.floor(x), Mth.floor(z))) {
                float y0 = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(prevX), Mth.floor(prevZ));
                float y1 = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
                // Both windings, since the beam's render type culls back faces
                wall(pose, buffer, bottom, top, (float) prevX, (float) prevZ, y0, (float) x, (float) z, y1);
                wall(pose, buffer, bottom, top, (float) x, (float) z, y1, (float) prevX, (float) prevZ, y0);
            }
            prevX = x;
            prevZ = z;
        }
    }

    private static void wall(PoseStack.Pose pose, VertexConsumer buffer, int bottom, int top, float x0, float z0, float y0, float x1, float z1, float y1) {
        vertex(pose, buffer, top, x0, y0 + CIRCLE_HEIGHT, z0, 1, 0);
        vertex(pose, buffer, bottom, x0, y0, z0, 1, 0.25F);
        vertex(pose, buffer, bottom, x1, y1, z1, 0, 0.25F);
        vertex(pose, buffer, top, x1, y1 + CIRCLE_HEIGHT, z1, 0, 0);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer buffer, int bottomColor, int topColor, float minY, float maxY, float x0, float z0, float x1, float z1,
          float minV, float maxV) {
        vertex(pose, buffer, topColor, x0, maxY, z0, 1, minV);
        vertex(pose, buffer, bottomColor, x0, minY, z0, 1, maxV);
        vertex(pose, buffer, bottomColor, x1, minY, z1, 0, maxV);
        vertex(pose, buffer, topColor, x1, maxY, z1, 0, minV);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, int color, float x, float y, float z, float u, float v) {
        buffer.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, 0, 1, 0);
    }

    private SurveyLocatorClient() {
    }
}
