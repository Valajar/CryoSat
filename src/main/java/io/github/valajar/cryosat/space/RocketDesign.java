package io.github.valajar.cryosat.space;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.valajar.cryosat.space.RocketPartItem.Kind;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * An assembled rocket: what the Launch Pad stores, renders and launches.
 *
 * @param tier        rocket tier, the lowest of the engine's and the fuel tank's; decides fuel, reach and the model colour
 * @param payload     what it carries
 * @param payloadTier tier of the payload, never above the rocket tier
 */
public record RocketDesign(RocketTier tier, PayloadType payload, RocketTier payloadTier) {
    public static final Codec<RocketDesign> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          RocketTier.CODEC.fieldOf("tier").forGetter(RocketDesign::tier),
          PayloadType.CODEC.fieldOf("payload").forGetter(RocketDesign::payload),
          RocketTier.CODEC.fieldOf("payload_tier").forGetter(RocketDesign::payloadTier)
    ).apply(instance, RocketDesign::new));
    public static final StreamCodec<ByteBuf, RocketDesign> STREAM_CODEC = StreamCodec.composite(
          RocketTier.STREAM_CODEC, RocketDesign::tier,
          PayloadType.STREAM_CODEC, RocketDesign::payload,
          RocketTier.STREAM_CODEC, RocketDesign::payloadTier,
          RocketDesign::new
    );

    /**
     * Builds a design from the four Rocket Assembly slots. Does not consume anything.
     *
     * @return the design, or the first problem found
     */
    public static Either<RocketDesign, MissionProblem> assemble(ItemStack engine, ItemStack fuelTank, ItemStack guidance, ItemStack payload) {
        if (!RocketPartItem.is(engine, Kind.ENGINE)) {
            return Either.right(MissionProblem.MISSING_ENGINE);
        } else if (!RocketPartItem.is(fuelTank, Kind.FUEL_TANK)) {
            return Either.right(MissionProblem.MISSING_FUEL_TANK);
        } else if (!RocketPartItem.is(guidance, Kind.GUIDANCE_COMPUTER)) {
            return Either.right(MissionProblem.MISSING_GUIDANCE);
        } else if (!RocketPartItem.is(payload, Kind.PAYLOAD)) {
            return Either.right(MissionProblem.MISSING_PAYLOAD);
        }
        RocketTier engineTier = ((RocketPartItem) engine.getItem()).tier();
        RocketTier tankTier = ((RocketPartItem) fuelTank.getItem()).tier();
        RocketPartItem payloadItem = (RocketPartItem) payload.getItem();
        RocketTier tier = engineTier.ordinal() <= tankTier.ordinal() ? engineTier : tankTier;
        if (!tier.atLeast(payloadItem.tier())) {
            return Either.right(MissionProblem.PAYLOAD_TOO_ADVANCED);
        } else if (tier.fuel().isEmpty()) {
            return Either.right(MissionProblem.TIER_CANNOT_LAUNCH);
        }
        return Either.left(new RocketDesign(tier, payloadItem.payload(), payloadItem.tier()));
    }
}
