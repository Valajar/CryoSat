package io.github.valajar.cryosat.client;

// Fusion Field Coils (superconductor workstream C)
import io.github.valajar.cryosat.client.gui.GuiFieldCoilController;

// Cargo Receiver and Uplink Card (space workstream A)
import io.github.valajar.cryosat.client.gui.GuiCargoReceiver;
import io.github.valajar.cryosat.registries.CryoDataComponents;
import io.github.valajar.cryosat.registries.CryoItems;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

// Survey Locator (satellite workstream B)
import io.github.valajar.cryosat.client.locator.SurveyLocatorClient;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.client.gui.GuiAtmosphericCompressor;
import io.github.valajar.cryosat.client.gui.GuiMissionControl;
import io.github.valajar.cryosat.client.gui.GuiRocketAssembly;
import io.github.valajar.cryosat.client.render.RenderLaunchController;
import io.github.valajar.cryosat.registries.CryoBlockEntities;
import io.github.valajar.cryosat.registries.CryoMenus;
import mekanism.client.ClientRegistrationUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = CryoSat.MODID, value = Dist.CLIENT)
public final class CryoSatClient {
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        ClientRegistrationUtil.registerScreen(event, CryoMenus.ATMOSPHERIC_COMPRESSOR, GuiAtmosphericCompressor::new);
        ClientRegistrationUtil.registerScreen(event, CryoMenus.LIQUEFACTION_PLANT, GuiLiquefactionPlant::new);
        // Cargo Receiver (space workstream A)
        ClientRegistrationUtil.registerScreen(event, CryoMenus.CARGO_RECEIVER, GuiCargoReceiver::new);
        // Mission Control (workstream D)
        ClientRegistrationUtil.registerScreen(event, CryoMenus.MISSION_CONTROL, GuiMissionControl::new);
        // Launch Pad (workstream B)
        ClientRegistrationUtil.registerScreen(event, CryoMenus.LAUNCH_CONTROLLER, GuiRocketAssembly::new);
        // Superconducting Separator (superconductor workstream A)
        ClientRegistrationUtil.registerScreen(event, CryoMenus.SUPERCONDUCTING_SEPARATOR, io.github.valajar.cryosat.client.gui.GuiSuperconductingSeparator::new);
        // Fusion Field Coils (superconductor workstream C)
        ClientRegistrationUtil.registerScreen(event, CryoMenus.FIELD_COIL_CONTROLLER, GuiFieldCoilController::new);
        // SQUID Array (satellite workstream C)
        ClientRegistrationUtil.registerScreen(event, CryoMenus.SQUID_ARRAY, io.github.valajar.cryosat.client.gui.GuiSquidArray::new);
        // Imaging Terminal (satellite workstream A)
        ClientRegistrationUtil.registerScreen(event, CryoMenus.IMAGING_TERMINAL, io.github.valajar.cryosat.client.gui.GuiImagingTerminal::new);
    }

    // Uplink Card (space workstream A): the "linked" model override shows whether the card holds a target
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ClientRegistrationUtil.setPropertyOverride(CryoItems.UPLINK_CARD, CryoSat.rl("linked"),
              (stack, level, entity, seed) -> stack.has(CryoDataComponents.UPLINK_TARGET) ? 1 : 0));
        // Survey Locator (satellite workstream B): the compass needle
        event.enqueueWork(SurveyLocatorClient::registerNeedle);
    }

    // Launch Pad (workstream B): the rocket on the pad
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CryoBlockEntities.LAUNCH_CONTROLLER.get(), RenderLaunchController::new);
    }

    private CryoSatClient() {
    }
}
