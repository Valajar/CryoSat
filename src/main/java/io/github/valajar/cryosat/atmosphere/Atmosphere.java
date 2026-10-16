package io.github.valajar.cryosat.atmosphere;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * What an Atmospheric Compressor pulls out of the air in one dimension. Loaded from
 * {@code data/<namespace>/cryosat/atmosphere/*.json}; a rate of 0 disables the dimension.
 *
 * @param dimension       dimension this applies to
 * @param chemical        chemical produced
 * @param rate            multiplier on the compressor's base output
 * @param altitudeScaling whether output thins out above sea level
 */
public record Atmosphere(ResourceKey<Level> dimension, Holder<Chemical> chemical, float rate, boolean altitudeScaling) {
    public static final Codec<Atmosphere> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(Atmosphere::dimension),
          MekanismAPI.CHEMICAL_REGISTRY.holderByNameCodec().fieldOf("chemical").forGetter(Atmosphere::chemical),
          Codec.floatRange(0, 100).optionalFieldOf("rate", 1f).forGetter(Atmosphere::rate),
          Codec.BOOL.optionalFieldOf("altitude_scaling", true).forGetter(Atmosphere::altitudeScaling)
    ).apply(instance, Atmosphere::new));
}
