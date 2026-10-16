package io.github.valajar.cryosat.registries;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.machine.MachineBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CryoBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CryoSat.MODID);

    public static final DeferredBlock<MachineBlock> ATMOSPHERIC_COMPRESSOR = BLOCKS.register("atmospheric_compressor",
          () -> new MachineBlock(machineProperties(), CryoBlockEntities.ATMOSPHERIC_COMPRESSOR));

    private static BlockBehaviour.Properties machineProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5f, 9f).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    private CryoBlocks() {
    }
}
