package io.github.valajar.cryosat.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.valajar.cryosat.CryoSat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.ExtraCodecs.TagOrElementLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * A kind of asteroid a miner can be sent to. Loaded from {@code data/<namespace>/cryosat/asteroid/*.json}.
 *
 * @param destination where these asteroids are found
 * @param minTier     lowest rocket tier that can reach this class
 * @param weight      relative chance among the classes a rocket can reach
 * @param hardnessMin lower bound of the rolled hardness; batch time scales with hardness
 * @param hardnessMax upper bound of the rolled hardness
 * @param yields      what a small asteroid of this class gives in total; bigger sizes multiply it
 */
public record AsteroidClass(ResourceLocation destination, RocketTier minTier, int weight, float hardnessMin, float hardnessMax, List<Yield> yields) {
    public static final ResourceLocation ASTEROID_BELT = CryoSat.rl("asteroid_belt");

    public static final Codec<AsteroidClass> CODEC = RecordCodecBuilder.<AsteroidClass>create(instance -> instance.group(
          ResourceLocation.CODEC.optionalFieldOf("destination", ASTEROID_BELT).forGetter(AsteroidClass::destination),
          RocketTier.CODEC.optionalFieldOf("min_tier", RocketTier.BASIC).forGetter(AsteroidClass::minTier),
          ExtraCodecs.POSITIVE_INT.optionalFieldOf("weight", 10).forGetter(AsteroidClass::weight),
          Codec.floatRange(0.1f, 10).optionalFieldOf("hardness_min", 1f).forGetter(AsteroidClass::hardnessMin),
          Codec.floatRange(0.1f, 10).optionalFieldOf("hardness_max", 1f).forGetter(AsteroidClass::hardnessMax),
          Yield.CODEC.listOf().fieldOf("yields").forGetter(AsteroidClass::yields)
    ).apply(instance, AsteroidClass::new)).validate(asteroid -> asteroid.hardnessMin <= asteroid.hardnessMax ? DataResult.success(asteroid)
                                                                : DataResult.error(() -> "hardness_min is above hardness_max"));

    public float rollHardness(RandomSource random) {
        return hardnessMin + random.nextFloat() * (hardnessMax - hardnessMin);
    }

    /**
     * Rolls the total cargo of one asteroid of this class.
     *
     * @param scale size multiplier times the fraction the miner can extract
     */
    public List<CargoEntry> rollCargo(RandomSource random, double scale) {
        List<CargoEntry> cargo = new ArrayList<>();
        for (Yield yield : yields) {
            Optional<Holder<Item>> item = yield.resolve();
            if (item.isEmpty()) {
                // Tag empty in this pack (for example a mod material that is not installed): skip it
                continue;
            }
            int count = (int) Math.round((yield.min + random.nextInt(yield.max - yield.min + 1)) * scale);
            if (count > 0) {
                CargoEntry.merge(cargo, new CargoEntry(item.get(), count));
            }
        }
        return cargo;
    }

    /**
     * One material of an asteroid class.
     *
     * @param item an item id, or {@code #tag} for a tag (the vanilla, then Mekanism, then first entry of the tag is used)
     * @param min  fewest items a small asteroid gives
     * @param max  most items a small asteroid gives
     */
    public record Yield(TagOrElementLocation item, int min, int max) {
        public static final Codec<Yield> CODEC = RecordCodecBuilder.<Yield>create(instance -> instance.group(
              ExtraCodecs.TAG_OR_ELEMENT_ID.fieldOf("item").forGetter(Yield::item),
              ExtraCodecs.NON_NEGATIVE_INT.fieldOf("min").forGetter(Yield::min),
              ExtraCodecs.NON_NEGATIVE_INT.fieldOf("max").forGetter(Yield::max)
        ).apply(instance, Yield::new)).validate(yield -> yield.min <= yield.max ? DataResult.success(yield)
                                                         : DataResult.error(() -> "min is above max for " + yield.item));

        public Optional<Holder<Item>> resolve() {
            if (!item.tag()) {
                return BuiltInRegistries.ITEM.getHolder(item.id()).filter(holder -> holder.value() != Items.AIR).map(holder -> holder);
            }
            Optional<HolderSet.Named<Item>> tag = BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, item.id()));
            if (tag.isEmpty() || tag.get().size() == 0) {
                return Optional.empty();
            }
            Holder<Item> chosen = null;
            for (Holder<Item> holder : tag.get()) {
                String namespace = holder.unwrapKey().map(key -> key.location().getNamespace()).orElse("");
                if (namespace.equals(ResourceLocation.DEFAULT_NAMESPACE)) {
                    return Optional.of(holder);
                } else if (namespace.equals("mekanism") && chosen == null) {
                    chosen = holder;
                }
            }
            return Optional.of(chosen == null ? tag.get().get(0) : chosen);
        }
    }
}
