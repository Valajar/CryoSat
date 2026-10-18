package io.github.valajar.cryosat.client.rocket;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.space.PayloadType;
import io.github.valajar.cryosat.space.RocketDesign;
import io.github.valajar.cryosat.space.RocketTier;
import io.github.valajar.cryosat.space.launch.LaunchSequence;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;

/**
 * DEBUG ONLY, for screenshots and for tuning the model and exhaust without a Launch Pad. A client side command, creative
 * mode only, that shows a rocket where the player looks. It exists only on this client and changes nothing in the world:
 * <ul>
 *     <li>{@code /cryosat rocket preview <tier> [<payload tier>]} shows a rocket on the block the player looks at</li>
 *     <li>{@code /cryosat rocket preview launch} plays a launch: ignition, climb and exhaust, then the rocket is gone</li>
 *     <li>{@code /cryosat rocket preview clear} removes it</li>
 * </ul>
 * The launch uses the Launch Pad's timing from {@link LaunchSequence}.
 */
@EventBusSubscriber(modid = CryoSat.MODID, value = Dist.CLIENT)
public final class RocketPreview {
    private static final String PREFIX = "[CryoSat debug] ";
    private static final int LAUNCH_TICKS = LaunchSequence.DURATION_TICKS;
    private static final double REACH = 32;

    @Nullable
    private static Preview preview;

    @SubscribeEvent
    public static void registerCommands(RegisterClientCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> previewNode = Commands.literal("preview");
        for (RocketTier tier : RocketTier.values()) {
            LiteralArgumentBuilder<CommandSourceStack> tierNode = Commands.literal(tier.getSerializedName())
                  .executes(ctx -> show(ctx.getSource(), tier, tier));
            for (RocketTier payloadTier : RocketTier.values()) {
                if (tier.atLeast(payloadTier)) {
                    tierNode.then(Commands.literal(payloadTier.getSerializedName()).executes(ctx -> show(ctx.getSource(), tier, payloadTier)));
                }
            }
            previewNode.then(tierNode);
        }
        previewNode.then(Commands.literal("launch").executes(ctx -> launch(ctx.getSource())));
        previewNode.then(Commands.literal("clear").executes(ctx -> {
            preview = null;
            ctx.getSource().sendSuccess(() -> Component.literal(PREFIX + "Rocket preview cleared"), false);
            return 1;
        }));
        event.getDispatcher().register(Commands.literal(CryoSat.MODID).then(Commands.literal("rocket").then(previewNode)));
    }

    private static int show(CommandSourceStack source, RocketTier tier, RocketTier payloadTier) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || !player.isCreative()) {
            source.sendFailure(Component.literal(PREFIX + "Rocket previews are for creative mode only"));
            return 0;
        }
        HitResult hit = player.pick(REACH, 1, false);
        BlockPos pos = hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK
                       ? blockHit.getBlockPos().relative(blockHit.getDirection())
                       : BlockPos.containing(player.getEyePosition().add(player.getLookAngle().scale(5)));
        preview = new Preview(minecraft.level, Vec3.atBottomCenterOf(pos), new RocketDesign(tier, PayloadType.ASTEROID_MINER, payloadTier));
        source.sendSuccess(() -> Component.literal(PREFIX + "Showing a " + tier.getSerializedName() + " rocket with a " + payloadTier.getSerializedName()
                                                   + " payload at " + pos.toShortString() + ". Use /cryosat rocket preview launch to watch it launch."), false);
        return 1;
    }

    private static int launch(CommandSourceStack source) {
        if (preview == null) {
            source.sendFailure(Component.literal(PREFIX + "No rocket preview to launch, place one with /cryosat rocket preview <tier>"));
            return 0;
        }
        preview.launchAge = 0;
        return 1;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Preview current = preview;
        if (current == null || current.launchAge < 0 || Minecraft.getInstance().isPaused()) {
            return;
        }
        if (current.level != Minecraft.getInstance().level) {
            preview = null;
            return;
        }
        current.launchAge++;
        float progress = current.launchAge / (float) LAUNCH_TICKS;
        if (progress > 1) {
            preview = null;
            return;
        }
        RocketRenderer.exhaust(current.level, current.base.add(0, rise(progress), 0), current.design, progress);
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        Preview current = preview;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || current == null || current.level != Minecraft.getInstance().level) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double rise = current.launchAge < 0 ? 0 : rise((current.launchAge + partialTick) / LAUNCH_TICKS);
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(current.base.x - camera.x, current.base.y + rise - camera.y, current.base.z - camera.z);
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        int light = LevelRenderer.getLightColor(current.level, BlockPos.containing(current.base.add(0, rise + 1, 0)));
        RocketRenderer.render(current.design, poseStack, buffers, light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        buffers.endBatch(Sheets.cutoutBlockSheet());
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        preview = null;
    }

    /** Height above the pad at a point of the launch, using the Launch Pad's timing. */
    private static double rise(float progress) {
        return LaunchSequence.rise(progress * LAUNCH_TICKS);
    }

    private static final class Preview {
        private final ClientLevel level;
        private final Vec3 base;
        private final RocketDesign design;
        /** Ticks since launch, or -1 while standing. */
        private int launchAge = -1;

        private Preview(ClientLevel level, Vec3 base, RocketDesign design) {
            this.level = level;
            this.base = base;
            this.design = design;
        }
    }

    private RocketPreview() {
    }
}
