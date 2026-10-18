package io.github.valajar.cryosat.space.mission;

import io.github.valajar.cryosat.CryoSat;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Mission Control's network payloads. The asset list reaches the client through Mekanism container sync
 * ({@link SyncableMissionList}); only button presses need a payload of their own.
 */
@EventBusSubscriber(modid = CryoSat.MODID)
public final class MissionNetwork {
    private static final String VERSION = "1";

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(CryoSat.MODID).versioned(VERSION);
        registrar.playToServer(MissionActionPayload.TYPE, MissionActionPayload.STREAM_CODEC, MissionActionPayload::handle);
    }

    private MissionNetwork() {
    }
}
