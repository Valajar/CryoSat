package io.github.valajar.cryosat.client;

// Cargo Receiver and Uplink Card (space workstream A)
import io.github.valajar.cryosat.client.gui.GuiCargoReceiver;
import io.github.valajar.cryosat.registries.CryoDataComponents;
import io.github.valajar.cryosat.registries.CryoItems;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

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
        ClientRegistrationUtil.registerScreen(event, CryoMenus.LIQUEFACTION_PLANT, GuiLiquefactionPlant::new);
        // Cargo Receiver (space workstream A)
        ClientRegistrationUtil.registerScreen(event, CryoMenus.CARGO_RECEIVER, GuiCargoReceiver::new);
    }

    // Uplink Card (space workstream A): the "linked" model override shows whether the card holds a target
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ClientRegistrationUtil.setPropertyOverride(CryoItems.UPLINK_CARD, CryoSat.rl("linked"),
              (stack, level, entity, seed) -> stack.has(CryoDataComponents.UPLINK_TARGET) ? 1 : 0));
    }

    private CryoSatClient() {
    }
}
