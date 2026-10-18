package io.github.valajar.cryosat.space;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Game event hooks of the space features: datapack loading and orbital ticking.
 */
public final class SpaceEvents {
    public static void register(IEventBus gameBus) {
        gameBus.addListener(SpaceEvents::addReloadListeners);
        gameBus.addListener(SpaceEvents::serverTick);
    }

    private static void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(SpaceData.DESTINATIONS);
        event.addListener(SpaceData.ASTEROIDS);
    }

    private static void serverTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % OrbitalRegistry.TICK_INTERVAL == 0) {
            OrbitalRegistry.get(event.getServer()).tick(event.getServer());
        }
    }

    private SpaceEvents() {
    }
}
