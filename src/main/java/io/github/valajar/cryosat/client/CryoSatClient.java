package io.github.valajar.cryosat.client;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.client.gui.GuiAtmosphericCompressor;
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
        // Launch Pad (workstream B)
        ClientRegistrationUtil.registerScreen(event, CryoMenus.LAUNCH_CONTROLLER, GuiRocketAssembly::new);
    }

    // Launch Pad (workstream B): the rocket on the pad
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CryoBlockEntities.LAUNCH_CONTROLLER.get(), RenderLaunchController::new);
    }

    private CryoSatClient() {
    }
}
