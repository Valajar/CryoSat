package io.github.valajar.cryosat.client.model;

import io.github.valajar.cryosat.CryoSat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Registers the {@code cryosat:connected} model loader (connected textures for multiblock casings). Client only. */
@EventBusSubscriber(modid = CryoSat.MODID, value = Dist.CLIENT)
public final class ConnectedModels {
    @SubscribeEvent
    public static void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(ConnectedGeometry.LOADER_ID, ConnectedGeometry.LOADER);
    }

    private ConnectedModels() {
    }
}
