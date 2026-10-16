package io.github.valajar.cryosat;

import com.mojang.logging.LogUtils;
import io.github.valajar.cryosat.atmosphere.AtmosphereManager;
import io.github.valajar.cryosat.multiblock.CryoMultiblocks;
import io.github.valajar.cryosat.registries.CryoBlockEntities;
import io.github.valajar.cryosat.registries.CryoBlocks;
import io.github.valajar.cryosat.registries.CryoChemicals;
import io.github.valajar.cryosat.registries.CryoCreativeTabs;
import io.github.valajar.cryosat.registries.CryoItems;
import io.github.valajar.cryosat.registries.CryoMenus;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;

@Mod(CryoSat.MODID)
public class CryoSat {
    public static final String MODID = "cryosat";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CryoSat(IEventBus modEventBus, ModContainer modContainer) {
        CryoChemicals.CHEMICALS.register(modEventBus);
        // Mekanism's registers also register the capabilities and item attachments of what they hold
        CryoItems.ITEMS.register(modEventBus);
        CryoBlocks.BLOCKS.register(modEventBus);
        CryoBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        CryoMenus.MENUS.register(modEventBus);
        CryoCreativeTabs.TABS.register(modEventBus);
        CryoMultiblocks.init();
        NeoForge.EVENT_BUS.addListener(CryoSat::addReloadListeners);
    }

    private static void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(AtmosphereManager.INSTANCE);
    }

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
