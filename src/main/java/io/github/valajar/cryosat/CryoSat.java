package io.github.valajar.cryosat;

import com.mojang.logging.LogUtils;
import io.github.valajar.cryosat.registries.CryoChemicals;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(CryoSat.MODID)
public class CryoSat {
    public static final String MODID = "cryosat";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CryoSat(IEventBus modEventBus, ModContainer modContainer) {
        CryoChemicals.CHEMICALS.register(modEventBus);
    }

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
