package io.github.valajar.cryosat.space.mission;

import io.github.valajar.cryosat.space.AsteroidRoll;
import io.github.valajar.cryosat.space.AsteroidSize;
import io.github.valajar.cryosat.space.CargoEntry;
import io.github.valajar.cryosat.space.OrbitalAsset;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * What Mission Control shows about one {@link OrbitalAsset}, built on the server and synced to the viewing player. The
 * asteroid (and so the batch count, which gives its size away) is left out while the miner is still travelling.
 *
 * @param customName      the player given name, empty for the default name; prefills the rename field
 * @param asteroid        the surveyed asteroid, empty while travelling
 * @param totalBatches    batches the miner splits its asteroid into, 0 while travelling
 * @param nextEventAt     overworld game time of the arrival or next batch
 * @param cargoInOrbit    items mined but not delivered yet, summed over every item type
 */
public record MissionSummary(UUID id, Component displayName, String customName, OrbitalAsset.State state, ResourceLocation destination,
                             Optional<Asteroid> asteroid, int batchesDelivered, int totalBatches, long nextEventAt, long cargoInOrbit,
                             Optional<GlobalPos> receiver) {

    public static final StreamCodec<RegistryFriendlyByteBuf, MissionSummary> STREAM_CODEC = StreamCodec.ofMember(MissionSummary::write, MissionSummary::read);

    public static MissionSummary of(OrbitalAsset asset) {
        AsteroidRoll roll = asset.asteroid;
        boolean surveyed = roll != null && asset.state != OrbitalAsset.State.TRAVELLING;
        Optional<Asteroid> asteroid = surveyed ? Optional.of(new Asteroid(roll.asteroidClass(), roll.size(), roll.hardness(), roll.extracted())) : Optional.empty();
        long cargo = asset.pending.stream().mapToLong(CargoEntry::count).sum();
        return new MissionSummary(asset.id, asset.displayName(), asset.name, asset.state, asset.destination, asteroid, asset.batchesDelivered,
              surveyed ? roll.batches() : 0, asset.nextEventAt, cargo, Optional.ofNullable(asset.receiver));
    }

    private void write(RegistryFriendlyByteBuf buf) {
        UUIDUtil.STREAM_CODEC.encode(buf, id);
        ComponentSerialization.STREAM_CODEC.encode(buf, displayName);
        buf.writeUtf(customName);
        OrbitalAsset.State.STREAM_CODEC.encode(buf, state);
        buf.writeResourceLocation(destination);
        buf.writeOptional(asteroid, (b, a) -> a.write(b));
        buf.writeVarInt(batchesDelivered);
        buf.writeVarInt(totalBatches);
        buf.writeVarLong(nextEventAt);
        buf.writeVarLong(cargoInOrbit);
        ByteBufCodecs.optional(GlobalPos.STREAM_CODEC).encode(buf, receiver);
    }

    private static MissionSummary read(RegistryFriendlyByteBuf buf) {
        return new MissionSummary(UUIDUtil.STREAM_CODEC.decode(buf), ComponentSerialization.STREAM_CODEC.decode(buf), buf.readUtf(),
              OrbitalAsset.State.STREAM_CODEC.decode(buf), buf.readResourceLocation(), buf.readOptional(Asteroid::read), buf.readVarInt(), buf.readVarInt(),
              buf.readVarLong(), buf.readVarLong(), ByteBufCodecs.optional(GlobalPos.STREAM_CODEC).decode(buf));
    }

    /**
     * @param asteroidClass id of the asteroid class
     * @param extracted     fraction of the asteroid the miner extracts
     */
    public record Asteroid(ResourceLocation asteroidClass, AsteroidSize size, float hardness, float extracted) {
        private void write(FriendlyByteBuf buf) {
            buf.writeResourceLocation(asteroidClass);
            AsteroidSize.STREAM_CODEC.encode(buf, size);
            buf.writeFloat(hardness);
            buf.writeFloat(extracted);
        }

        private static Asteroid read(FriendlyByteBuf buf) {
            return new Asteroid(buf.readResourceLocation(), AsteroidSize.STREAM_CODEC.decode(buf), buf.readFloat(), buf.readFloat());
        }
    }
}
