package io.github.valajar.cryosat.client.rocket;

import io.github.valajar.cryosat.CryoSat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * The rocket's three stacked segment models ({@code assets/cryosat/models/rocket/*.json}, generated with their textures by
 * {@code tools/textures/gen/rocket.py}). Each is 2 blocks tall; one block model cannot hold the whole 6 block rocket.
 */
@EventBusSubscriber(modid = CryoSat.MODID, value = Dist.CLIENT)
public final class RocketModels {
    /** Model height of one segment, in blocks. */
    static final float SEGMENT_HEIGHT = 2;

    /** Bottom to top. */
    enum Segment {
        /** Nozzle, aft body and fins; accents take the rocket tier. */
        ENGINE,
        /** Propellant tank with the tier band; accents take the rocket tier. */
        TANK,
        /** Fairing and nose cone; the nose tip takes the payload tier. */
        PAYLOAD;

        final ModelResourceLocation model = ModelResourceLocation.standalone(CryoSat.rl("rocket/" + name().toLowerCase()));

        BakedModel baked() {
            return Minecraft.getInstance().getModelManager().getModel(model);
        }
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        for (Segment segment : Segment.values()) {
            event.register(segment.model);
        }
    }

    private RocketModels() {
    }
}
