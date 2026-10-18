package io.github.valajar.cryosat.client.rocket;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.valajar.cryosat.space.RocketDesign;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

/**
 * Draws an assembled rocket and its exhaust. The Launch Pad's block entity renderer calls this; it owns nothing in the
 * world. The rocket fits a column {@link #RADIUS} blocks either side of its axis and {@link #HEIGHT} blocks tall.
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
        // Placeholder until the rocket model lands
    }

    /**
     * Spawns exhaust particles for one tick of a launch.
     *
     * @param nozzle   world position of the nozzle exit
     * @param progress launch progress from 0 (ignition) to 1 (out of sight)
     */
    public static void exhaust(ClientLevel level, Vec3 nozzle, RocketDesign design, float progress) {
        // Placeholder until the launch effects land
    }

    private RocketRenderer() {
    }
}
