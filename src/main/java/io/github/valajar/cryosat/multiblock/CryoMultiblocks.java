package io.github.valajar.cryosat.multiblock;

import io.github.valajar.cryosat.multiblock.liquefaction.LiquefactionCache;
import io.github.valajar.cryosat.multiblock.liquefaction.LiquefactionMultiblockData;
import io.github.valajar.cryosat.multiblock.liquefaction.LiquefactionValidator;
import io.github.valajar.cryosat.space.launch.LaunchPadCache;
import io.github.valajar.cryosat.space.launch.LaunchPadMultiblockData;
import io.github.valajar.cryosat.space.launch.LaunchPadValidator;
import mekanism.common.lib.multiblock.MultiblockManager;

/**
 * Multiblock managers. Creating a manager registers it with Mekanism (which saves and loads its caches with the world), so this
 * class must be loaded during mod construction; see {@link #init()}.
 */
public final class CryoMultiblocks {
    public static final MultiblockManager<LiquefactionMultiblockData> LIQUEFACTION_PLANT =
          new MultiblockManager<>("cryosatLiquefactionPlant", LiquefactionCache::new, LiquefactionValidator::new);

    // Launch Pad (workstream B)
    public static final MultiblockManager<LaunchPadMultiblockData> LAUNCH_PAD =
          new MultiblockManager<>("cryosatLaunchPad", LaunchPadCache::new, LaunchPadValidator::new);

    public static void init() {
    }

    private CryoMultiblocks() {
    }
}
