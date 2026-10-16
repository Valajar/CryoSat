package io.github.valajar.cryosat.client;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.registries.CryoMenus;
import io.github.valajar.cryosat.registries.plant.PlantContainerTypes;
import mekanism.client.ClientRegistrationUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = CryoSat.MODID, value = Dist.CLIENT)
public final class CryoSatClient {
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(CryoMenus.MACHINE.get(), MachineScreen::new);
        ClientRegistrationUtil.registerScreen(event, PlantContainerTypes.LIQUEFACTION_PLANT, GuiLiquefactionPlant::new);
    }

    private CryoSatClient() {
    }
}
