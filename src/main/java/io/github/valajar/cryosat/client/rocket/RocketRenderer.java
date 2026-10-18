package io.github.valajar.cryosat.client.rocket;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.valajar.cryosat.client.rocket.RocketModels.Segment;
import io.github.valajar.cryosat.space.RocketDesign;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Draws an assembled rocket and its exhaust. The Launch Pad's block entity renderer calls this; it owns nothing in the
 * world. The rocket fits a column {@link #RADIUS} blocks either side of its axis and {@link #HEIGHT} blocks tall.
 * <p>
 * The model is Mekanism steel with tier coloured accents: the nozzle lip, the fin edges and the tank band take the rocket
 * tier's colour, the nose tip the payload tier's. The colour is a tint on those faces only (see {@link RocketModels}).
 */
public final class RocketRenderer {
    /** Height of the rocket model in blocks, from the nozzle to the tip. */
    public static final float HEIGHT = 6;
    /** Furthest any part of the model (fins included) reaches from the rocket's axis, in blocks. */
    public static final float RADIUS = 1.5f;

    /**
     * Renders the rocket standing on the pad.
     *
     * @param poseStack origin at the bottom centre of the rocket (the nozzle exit), y up
     */
    public static void render(RocketDesign design, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        ModelBlockRenderer renderer = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
        VertexConsumer consumer = buffers.getBuffer(Sheets.cutoutBlockSheet());
        poseStack.pushPose();
        // Models are built around the block centre (8, 8)
        poseStack.translate(-0.5, 0, -0.5);
        for (Segment segment : Segment.values()) {
            int color = segment == Segment.PAYLOAD ? design.payloadTier().color() : design.tier().color();
            // Only faces with a tint index take the colour; the steel stays as textured
            renderer.renderModel(poseStack.last(), consumer, null, segment.baked(), red(color), green(color), blue(color), light, overlay,
                  ModelData.EMPTY, RenderType.cutout());
            poseStack.translate(0, RocketModels.SEGMENT_HEIGHT, 0);
        }
        poseStack.popPose();
    }

    /**
     * Spawns exhaust particles for one tick of a launch.
     *
     * @param nozzle   world position of the nozzle exit
     * @param progress launch progress from 0 (ignition) to 1 (out of sight)
     */
    public static void exhaust(ClientLevel level, Vec3 nozzle, RocketDesign design, float progress) {
        RocketExhaust.tick(level, nozzle, design, progress);
    }

    private static float red(int argb) {
        return (argb >> 16 & 0xFF) / 255F;
    }

    private static float green(int argb) {
        return (argb >> 8 & 0xFF) / 255F;
    }

    private static float blue(int argb) {
        return (argb & 0xFF) / 255F;
    }

    private RocketRenderer() {
    }
}
