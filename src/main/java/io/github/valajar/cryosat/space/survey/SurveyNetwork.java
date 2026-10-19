package io.github.valajar.cryosat.space.survey;

import io.github.valajar.cryosat.CryoSat;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The Imaging Terminal's network payload. Targets, coverage, passes and the log reach the client through Mekanism container
 * sync; only button presses need a payload of their own.
 */
@EventBusSubscriber(modid = CryoSat.MODID)
public final class SurveyNetwork {
    private static final String VERSION = "1";

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(CryoSat.MODID).versioned(VERSION);
        registrar.playToServer(ImagingActionPayload.TYPE, ImagingActionPayload.STREAM_CODEC, ImagingActionPayload::handle);
    }

    private SurveyNetwork() {
    }
}
