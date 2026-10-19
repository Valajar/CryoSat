package io.github.valajar.cryosat.client.rocket;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.space.PayloadType;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * The rocket's three stacked segment models ({@code assets/cryosat/models/rocket/*.json}, generated with their textures by
 * {@code tools/textures/gen/rocket.py}). Each is 2 blocks tall; one block model cannot hold the whole 6 block rocket. The top
 * segment depends on the payload, see {@link #stack(PayloadType)}.
 */
@EventBusSubscriber(modid = CryoSat.MODID, value = Dist.CLIENT)
public final class RocketModels {
    /** Model height of one segment, in blocks. */
    static final float SEGMENT_HEIGHT = 2;

    /** Every segment model; {@link #stack(PayloadType)} picks the three a rocket is drawn from. */
    enum Segment {
        /** Nozzle, aft body and fins; accents take the rocket tier. */
        ENGINE,
        /** Propellant tank with the tier band; accents take the rocket tier. */
        TANK,
        /** Fairing with an MLI gold panel and nose cone, for asteroid miners; the nose tip takes the payload tier. */
        PAYLOAD,
        /** Light steel satellite fairing with an ogive nose, for Survey Satellites; the nose tip takes the payload tier. */
        SATELLITE_FAIRING;

        final ModelResourceLocation model = ModelResourceLocation.standalone(CryoSat.rl("rocket/" + name().toLowerCase()));

        BakedModel baked() {
            return Minecraft.getInstance().getModelManager().getModel(model);
        }

        /** Whether the tinted faces take the payload tier's colour rather than the rocket tier's. */
        boolean payload() {
            return this == PAYLOAD || this == SATELLITE_FAIRING;
        }
    }

    private static final List<Segment> MINER_STACK = List.of(Segment.ENGINE, Segment.TANK, Segment.PAYLOAD);
    private static final List<Segment> SATELLITE_STACK = List.of(Segment.ENGINE, Segment.TANK, Segment.SATELLITE_FAIRING);

    /** The segments of a rocket carrying {@code payload}, bottom to top. */
    static List<Segment> stack(PayloadType payload) {
        return switch (payload) {
            case ASTEROID_MINER -> MINER_STACK;
            case SURVEY_SATELLITE -> SATELLITE_STACK;
        };
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
