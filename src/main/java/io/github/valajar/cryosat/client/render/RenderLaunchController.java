package io.github.valajar.cryosat.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.valajar.cryosat.client.rocket.RocketRenderer;
import io.github.valajar.cryosat.space.RocketDesign;
import io.github.valajar.cryosat.space.launch.LaunchPadMultiblockData;
import io.github.valajar.cryosat.space.launch.LaunchSequence;
import io.github.valajar.cryosat.space.launch.TileEntityLaunchController;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Draws the assembled rocket standing on the pad centre, and rising during the launch sequence. The model itself is
 * {@link RocketRenderer}'s.
 */
public class RenderLaunchController implements BlockEntityRenderer<TileEntityLaunchController> {
    /** The rocket is big and climbs far, so keep drawing it from well beyond the usual block entity range. */
    private static final int VIEW_DISTANCE = 256;

    public RenderLaunchController(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(@NotNull TileEntityLaunchController controller, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffers,
          int light, int overlay) {
        LaunchPadMultiblockData pad = controller.getMultiblock();
        RocketDesign design = pad.getDesign();
        if (!pad.isFormed() || design == null || !(controller.getLevel() instanceof ClientLevel level)) {
            return;
        }
        float ticks = LaunchPadClient.ticksSinceLaunch(pad, level, partialTick);
        Vec3 nozzle = LaunchPadClient.nozzle(pad, Math.max(0, ticks));
        BlockPos pos = controller.getBlockPos();
        poseStack.pushPose();
        poseStack.translate(nozzle.x - pos.getX(), nozzle.y - pos.getY(), nozzle.z - pos.getZ());
        RocketRenderer.render(design, poseStack, buffers, LevelRenderer.getLightColor(level, BlockPos.containing(nozzle).above()), overlay);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(@NotNull TileEntityLaunchController controller) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return VIEW_DISTANCE;
    }

    @NotNull
    @Override
    public AABB getRenderBoundingBox(@NotNull TileEntityLaunchController controller) {
        LaunchPadMultiblockData pad = controller.getMultiblock();
        if (!pad.isFormed() || pad.getDesign() == null) {
            return new AABB(controller.getBlockPos());
        }
        BlockPos centre = pad.getCentre();
        double top = centre.getY() + 1 + RocketRenderer.HEIGHT + (pad.isLaunching() ? LaunchSequence.MAX_RISE : 0);
        return new AABB(centre.getX() + 0.5 - RocketRenderer.RADIUS, centre.getY(), centre.getZ() + 0.5 - RocketRenderer.RADIUS,
              centre.getX() + 0.5 + RocketRenderer.RADIUS, top, centre.getZ() + 0.5 + RocketRenderer.RADIUS)
              .minmax(new AABB(controller.getBlockPos()));
    }
}
