package io.github.valajar.cryosat.client.render;

import io.github.valajar.cryosat.client.rocket.RocketRenderer;
import io.github.valajar.cryosat.space.RocketDesign;
import io.github.valajar.cryosat.space.launch.LaunchPadMultiblockData;
import io.github.valajar.cryosat.space.launch.LaunchSequence;
import io.github.valajar.cryosat.space.launch.TileEntityLaunchController;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Client side of a launch: where the rocket is during the sequence, and the exhaust it leaves each client tick. Called
 * from the controller's client tick, so it only ever runs on the client.
 */
public final class LaunchPadClient {

    public static void tick(TileEntityLaunchController controller) {
        if (!(controller.getLevel() instanceof ClientLevel level)) {
            return;
        }
        LaunchPadMultiblockData pad = controller.getMultiblock();
        RocketDesign design = pad.getDesign();
        float ticks = ticksSinceLaunch(pad, level, 0);
        if (design == null || ticks < 0 || ticks > LaunchSequence.DURATION_TICKS) {
            return;
        }
        RocketRenderer.exhaust(level, nozzle(pad, ticks), design, LaunchSequence.progress(ticks));
    }

    /** @return ticks since the current launch started, or -1 if the pad is not launching */
    public static float ticksSinceLaunch(LaunchPadMultiblockData pad, @Nullable ClientLevel level, float partialTick) {
        if (level == null || !pad.isFormed() || !pad.isLaunching()) {
            return -1;
        }
        return Math.max(0, level.getGameTime() - pad.getLaunchStartedAt() + partialTick);
    }

    /** World position of the nozzle exit at a point of the launch sequence. */
    public static Vec3 nozzle(LaunchPadMultiblockData pad, float ticksSinceLaunch) {
        BlockPos centre = pad.getCentre();
        return new Vec3(centre.getX() + 0.5, centre.getY() + 1 + LaunchSequence.rise(ticksSinceLaunch), centre.getZ() + 0.5);
    }

    private LaunchPadClient() {
    }
}
