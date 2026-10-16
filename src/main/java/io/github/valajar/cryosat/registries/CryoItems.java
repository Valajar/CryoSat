package io.github.valajar.cryosat.registries;

import io.github.valajar.cryosat.CryoSat;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CryoItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CryoSat.MODID);

    public static final DeferredItem<BlockItem> ATMOSPHERIC_COMPRESSOR = ITEMS.registerSimpleBlockItem(CryoBlocks.ATMOSPHERIC_COMPRESSOR);

    private CryoItems() {
    }
}
