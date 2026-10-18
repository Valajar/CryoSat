package io.github.valajar.cryosat.datagen;

import io.github.valajar.cryosat.registries.CryoBlocks;
import java.util.List;
import java.util.Set;
import mekanism.common.registries.MekanismDataComponents;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.CopyNameFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.neoforge.registries.DeferredHolder;

public class CryoBlockLoot extends BlockLootSubProvider {
    /**
     * What a Mekanism machine keeps when broken, as in Mekanism's own loot tables: configuration, upgrades, security
     * and the contents of its energy buffers, slots and tanks.
     */
    private static final List<DeferredHolder<DataComponentType<?>, ? extends DataComponentType<?>>> MACHINE_COMPONENTS = List.of(
          MekanismDataComponents.EJECTOR,
          MekanismDataComponents.OWNER,
          MekanismDataComponents.REDSTONE_CONTROL,
          MekanismDataComponents.SECURITY,
          MekanismDataComponents.SIDE_CONFIG,
          MekanismDataComponents.UPGRADES,
          MekanismDataComponents.ATTACHED_ENERGY,
          MekanismDataComponents.ATTACHED_ITEMS,
          MekanismDataComponents.ATTACHED_CHEMICALS
    );

    public CryoBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        dropMachine(CryoBlocks.ATMOSPHERIC_COMPRESSOR.get());
        // Liquefaction Plant parts keep nothing themselves; the plant's contents live in its multiblock cache
        dropSelf(CryoBlocks.CRYO_CASING.get());
        dropSelf(CryoBlocks.CRYO_VALVE.get());
        dropSelf(CryoBlocks.CRYOCOOLER.get());
        dropSelf(CryoBlocks.HEAT_REJECTION_PORT.get());
        dropSelf(CryoBlocks.LIQUEFACTION_CONTROLLER.get());
        // Cargo Receiver (space workstream A): keeps its cargo, config and security like other Mekanism machines
        dropMachine(CryoBlocks.CARGO_RECEIVER.get());
        // Mission Control (workstream D): keeps security and the Uplink Card in its slot
        dropMachine(CryoBlocks.MISSION_CONTROL.get());
        // Launch Pad (workstream B): the rocket, its parts and fuel live in the pad's multiblock cache
        dropSelf(CryoBlocks.LAUNCH_PAD_PLATING.get());
        dropSelf(CryoBlocks.LAUNCH_PAD_FUEL_PORT.get());
        dropSelf(CryoBlocks.LAUNCH_CONTROLLER.get());
        // Superconducting Separator (superconductor workstream A): keeps both tanks, config and security; it warms up again
        dropMachine(CryoBlocks.SUPERCONDUCTING_SEPARATOR.get());
        // Fusion Field Coils (superconductor workstream C): the controller keeps its coolant, config and security
        dropSelf(CryoBlocks.FIELD_COIL.get());
        dropMachine(CryoBlocks.FIELD_COIL_CONTROLLER.get());
    }

    private void dropMachine(Block block) {
        CopyComponentsFunction.Builder components = CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY);
        MACHINE_COMPONENTS.forEach(component -> components.include(component.get()));
        add(block, LootTable.lootTable().withPool(LootPool.lootPool()
              .name("main")
              .setRolls(ConstantValue.exactly(1))
              .add(LootItem.lootTableItem(block)
                    .apply(CopyNameFunction.copyName(CopyNameFunction.NameSource.BLOCK_ENTITY))
                    .apply(components))));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return CryoBlocks.BLOCKS.getPrimaryEntries().stream().map(DeferredHolder::value).map(Block.class::cast).toList();
    }
}
