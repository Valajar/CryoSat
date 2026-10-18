package io.github.valajar.cryosat.space;

import io.github.valajar.cryosat.CryoSat;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.function.IntFunction;
import mekanism.api.tier.BaseTier;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * Rocket tiers follow Mekanism's four tiers and borrow their colours, so a rocket's tier reads at a glance.
 */
public enum RocketTier implements StringRepresentable {
    BASIC(BaseTier.BASIC, 1, Fuel.solid(CryoSat.rl("solid_propellant_grain"), 16)),
    ADVANCED(BaseTier.ADVANCED, 2, Fuel.liquid(CryoSat.rl("liquid_oxygen"), 4_000, ResourceLocation.fromNamespaceAndPath("mekanism", "ethene"), 4_000)),
    ELITE(BaseTier.ELITE, 4, Fuel.liquid(CryoSat.rl("liquid_oxygen"), 4_000, CryoSat.rl("liquid_hydrogen"), 8_000)),
    // Cryogenic solid grain (frozen oxygen plus metallic hydrogen) comes with the gas giants
    ULTIMATE(BaseTier.ULTIMATE, 8, null);

    public static final StringRepresentable.EnumCodec<RocketTier> CODEC = StringRepresentable.fromEnum(RocketTier::values);
    public static final IntFunction<RocketTier> BY_ID = ByIdMap.continuous(RocketTier::ordinal, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);
    public static final StreamCodec<ByteBuf, RocketTier> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, RocketTier::ordinal);

    private final BaseTier baseTier;
    private final int minerCapacity;
    private final Fuel fuel;

    RocketTier(BaseTier baseTier, int minerCapacity, Fuel fuel) {
        this.baseTier = baseTier;
        this.minerCapacity = minerCapacity;
        this.fuel = fuel;
    }

    public BaseTier baseTier() {
        return baseTier;
    }

    /** Mekanism's colour for this tier, as 0xAARRGGBB. Used to tint rocket models and item names. */
    public int color() {
        return baseTier.getPackedColor();
    }

    /**
     * How much of an asteroid an Asteroid Miner of this tier can extract, in multiples of a small asteroid. A miner on a
     * bigger asteroid than its capacity extracts only part of it.
     */
    public int minerCapacity() {
        return minerCapacity;
    }

    /** What a rocket of this tier burns per launch, empty while the tier cannot launch yet. */
    public Optional<Fuel> fuel() {
        return Optional.ofNullable(fuel);
    }

    public boolean atLeast(RocketTier other) {
        return ordinal() >= other.ordinal();
    }

    @NotNull
    @Override
    public String getSerializedName() {
        return baseTier.getLowerName();
    }

    /**
     * Fuel for one launch: either a number of solid propellant items, or an oxidiser plus a fuel chemical (ids, so a
     * missing chemical never breaks loading).
     */
    public record Fuel(Optional<ResourceLocation> solidItem, int solidCount, Optional<ResourceLocation> oxidiser, long oxidiserAmount,
                       Optional<ResourceLocation> propellant, long propellantAmount) {

        static Fuel solid(ResourceLocation item, int count) {
            return new Fuel(Optional.of(item), count, Optional.empty(), 0, Optional.empty(), 0);
        }

        static Fuel liquid(ResourceLocation oxidiser, long oxidiserAmount, ResourceLocation propellant, long propellantAmount) {
            return new Fuel(Optional.empty(), 0, Optional.of(oxidiser), oxidiserAmount, Optional.of(propellant), propellantAmount);
        }

        public boolean isSolid() {
            return solidItem.isPresent();
        }
    }
}
