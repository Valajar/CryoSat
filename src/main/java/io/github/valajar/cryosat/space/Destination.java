package io.github.valajar.cryosat.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.Level;

/**
 * Somewhere a rocket can go. Loaded from {@code data/<namespace>/cryosat/destination/*.json}; the file id is the
 * destination id. Destinations are abstract: nobody visits them, assets there live in the {@link OrbitalRegistry}.
 *
 * @param minTier     lowest rocket tier that reaches it
 * @param travelTicks time from launch until the payload starts working
 * @param payloads    payloads that can do something there
 * @param launchFrom  dimensions a rocket can launch from to get there; empty means any
 */
public record Destination(RocketTier minTier, int travelTicks, List<PayloadType> payloads, List<ResourceKey<Level>> launchFrom) {
    public static final Codec<Destination> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          RocketTier.CODEC.optionalFieldOf("min_tier", RocketTier.BASIC).forGetter(Destination::minTier),
          ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("travel_ticks", 2_400).forGetter(Destination::travelTicks),
          PayloadType.CODEC.listOf().fieldOf("payloads").forGetter(Destination::payloads),
          Level.RESOURCE_KEY_CODEC.listOf().optionalFieldOf("launch_from", List.of()).forGetter(Destination::launchFrom)
    ).apply(instance, Destination::new));

    public boolean canLaunchFrom(ResourceKey<Level> dimension) {
        return launchFrom.isEmpty() || launchFrom.contains(dimension);
    }
}
