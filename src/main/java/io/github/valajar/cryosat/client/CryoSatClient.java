package io.github.valajar.cryosat.client;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.client.gui.GuiAtmosphericCompressor;
import io.github.valajar.cryosat.registries.CryoMenus;
import mekanism.client.ClientRegistrationUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = CryoSat.MODID, value = Dist.CLIENT)
public final class CryoSatClient {
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        ClientRegistrationUtil.registerScreen(event, CryoMenus.ATMOSPHERIC_COMPRESSOR, GuiAtmosphericCompressor::new);
    }

    private CryoSatClient() {
    }
}
