package io.github.valajar.cryosat.registries;

import io.github.valajar.cryosat.CryoSat;
import mekanism.common.registration.impl.ItemDeferredRegister;

/**
 * Standalone items, registered through Mekanism's {@link ItemDeferredRegister}. Block items are registered together
 * with their blocks in {@link CryoBlocks}.
 */
public final class CryoItems {
    public static final ItemDeferredRegister ITEMS = new ItemDeferredRegister(CryoSat.MODID);

    private CryoItems() {
    }
}
