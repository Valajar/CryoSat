package io.github.valajar.cryosat.registries;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.machine.compressor.AtmosphericCompressorBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CryoBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CryoSat.MODID);

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AtmosphericCompressorBlockEntity>> ATMOSPHERIC_COMPRESSOR =
          BLOCK_ENTITIES.register("atmospheric_compressor",
                () -> BlockEntityType.Builder.of(AtmosphericCompressorBlockEntity::new, CryoBlocks.ATMOSPHERIC_COMPRESSOR.get()).build(null));

    private CryoBlockEntities() {
    }
}
