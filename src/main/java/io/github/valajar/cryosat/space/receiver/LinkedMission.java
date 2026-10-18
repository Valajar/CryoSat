package io.github.valajar.cryosat.space.receiver;

import io.github.valajar.cryosat.space.CargoEntry;
import io.github.valajar.cryosat.space.OrbitalAsset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import mekanism.common.inventory.container.sync.list.SyncableList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

/**
 * What the Cargo Receiver's GUI shows about one mission linked to it, synced from the server's {@link
 * io.github.valajar.cryosat.space.OrbitalRegistry}.
 *
 * @param name      the asset's display name
 * @param state     travelling, mining or retired
 * @param delivered batches mined so far
 * @param batches   batches in total, 0 while the asteroid is not surveyed yet (still travelling)
 * @param waiting   items mined but not yet delivered, waiting in orbit
 */
public record LinkedMission(Component name, OrbitalAsset.State state, int delivered, int batches, int waiting) {
    public static final StreamCodec<RegistryFriendlyByteBuf, LinkedMission> STREAM_CODEC = StreamCodec.composite(
          ComponentSerialization.STREAM_CODEC, LinkedMission::name,
          OrbitalAsset.State.STREAM_CODEC, LinkedMission::state,
          ByteBufCodecs.VAR_INT, LinkedMission::delivered,
          ByteBufCodecs.VAR_INT, LinkedMission::batches,
          ByteBufCodecs.VAR_INT, LinkedMission::waiting,
          LinkedMission::new
    );

    public static LinkedMission of(OrbitalAsset asset) {
        int waiting = 0;
        for (CargoEntry entry : asset.pending) {
            waiting += entry.count();
        }
        boolean surveyed = asset.asteroid != null && asset.state != OrbitalAsset.State.TRAVELLING;
        return new LinkedMission(asset.displayName(), asset.state, asset.batchesDelivered, surveyed ? asset.asteroid.batches() : 0, waiting);
    }

    /** Mekanism container sync of the mission list; resent whenever the list's hash changes. */
    public static class Syncable extends SyncableList<LinkedMission> {
        public Syncable(Supplier<? extends @NotNull Collection<LinkedMission>> getter, Consumer<@NotNull List<LinkedMission>> setter) {
            super(getter, setter);
        }

        @Override
        protected List<LinkedMission> deserializeList(RegistryFriendlyByteBuf buffer) {
            int size = buffer.readVarInt();
            List<LinkedMission> list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                list.add(STREAM_CODEC.decode(buffer));
            }
            return list;
        }

        @Override
        protected void serializeListElement(RegistryFriendlyByteBuf buffer, LinkedMission element) {
            STREAM_CODEC.encode(buffer, element);
        }
    }
}
