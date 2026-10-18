package io.github.valajar.cryosat.space.launch;

import io.github.valajar.cryosat.registries.CryoBlockTypes;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.EnumSet;
import java.util.Set;
import mekanism.common.MekanismLang;
import mekanism.common.content.blocktype.BlockType;
import mekanism.common.lib.math.voxel.VoxelCuboid;
import mekanism.common.lib.math.voxel.VoxelCuboid.CuboidSide;
import mekanism.common.lib.multiblock.CuboidStructureValidator;
import mekanism.common.lib.multiblock.FormationProtocol;
import mekanism.common.lib.multiblock.FormationProtocol.CasingType;
import mekanism.common.lib.multiblock.FormationProtocol.FormationResult;
import mekanism.common.lib.multiblock.FormationProtocol.StructureRequirement;
import mekanism.common.lib.multiblock.StructureHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * A flat {@value #SIZE}x1x{@value #SIZE} platform: Launch Pad Plating anywhere, exactly one Launch Controller and any number of
 * Fuel Ports on the outer ring.
 * <p>
 * Mekanism's framework handles a one block high structure once the cuboid is fetched from the top and bottom planes only:
 * a flat structure has a full major plane on the Y axis, but its X and Z planes are one block tall and never become major,
 * so the default all six sides fetch would fail. Every position is then a casing position (there is no interior).
 */
public class LaunchPadValidator extends CuboidStructureValidator<LaunchPadMultiblockData> {
    public static final int SIZE = 5;
    private static final VoxelCuboid BOUNDS = new VoxelCuboid(SIZE, 1, SIZE);
    private static final Set<CuboidSide> SIDES = EnumSet.of(CuboidSide.TOP, CuboidSide.BOTTOM);

    private BlockPos controller;
    private int fuelPorts;

    public LaunchPadValidator() {
        super(BOUNDS, BOUNDS);
    }

    @Override
    public boolean precheck() {
        cuboid = StructureHelper.fetchCuboid(structure, BOUNDS, BOUNDS, SIDES, 0);
        return cuboid != null;
    }

    @Override
    protected StructureRequirement getStructureRequirement(BlockPos pos) {
        return StructureRequirement.OTHER;
    }

    @Override
    protected CasingType getCasingType(BlockState state) {
        Block block = state.getBlock();
        if (BlockType.is(block, CryoBlockTypes.LAUNCH_PAD_PLATING)) {
            return CasingType.FRAME;
        } else if (BlockType.is(block, CryoBlockTypes.LAUNCH_CONTROLLER, CryoBlockTypes.LAUNCH_PAD_FUEL_PORT)) {
            return CasingType.OTHER;
        }
        return CasingType.INVALID;
    }

    @Override
    protected FormationResult validateFrame(FormationProtocol<LaunchPadMultiblockData> ctx, BlockPos pos, BlockState state, CasingType type, boolean needsFrame) {
        if (type == CasingType.INVALID) {
            return FormationResult.fail(MekanismLang.MULTIBLOCK_INVALID_FRAME, pos);
        }
        if (type == CasingType.OTHER) {
            if (!onRing(pos)) {
                return FormationResult.fail(LaunchLang.INVALID_INNER_PART, pos);
            }
            if (BlockType.is(state.getBlock(), CryoBlockTypes.LAUNCH_CONTROLLER)) {
                if (controller != null) {
                    return FormationResult.fail(MekanismLang.MULTIBLOCK_INVALID_CONTROLLER_CONFLICT, pos, true);
                }
                controller = pos.immutable();
            } else {
                fuelPorts++;
            }
        }
        return super.validateFrame(ctx, pos, state, type, needsFrame);
    }

    private boolean onRing(BlockPos pos) {
        BlockPos min = cuboid.getMinPos(), max = cuboid.getMaxPos();
        return pos.getX() == min.getX() || pos.getX() == max.getX() || pos.getZ() == min.getZ() || pos.getZ() == max.getZ();
    }

    @Override
    public FormationResult postcheck(LaunchPadMultiblockData structure, Long2ObjectMap<ChunkAccess> chunkMap) {
        if (controller == null) {
            return FormationResult.fail(MekanismLang.MULTIBLOCK_INVALID_NO_CONTROLLER);
        }
        structure.controller = controller;
        structure.fuelPorts = fuelPorts;
        return FormationResult.SUCCESS;
    }
}
