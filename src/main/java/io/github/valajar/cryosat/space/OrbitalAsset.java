package io.github.valajar.cryosat.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.IntFunction;
import mekanism.api.text.IHasTextComponent;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Something in orbit, stored in the {@link OrbitalRegistry} rather than in any chunk. Mutable; change it only on the
 * server thread and through the registry, which marks the saved data dirty.
 */
public class OrbitalAsset {
    /** Written in place of the legacy miner fields, which then encode to nothing. */
    private static final MinerState LEGACY_EMPTY = new MinerState(null, 0, List.of());
    public static final Codec<OrbitalAsset> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          UUIDUtil.CODEC.fieldOf("id").forGetter(asset -> asset.id),
          UUIDUtil.CODEC.fieldOf("owner").forGetter(asset -> asset.owner),
          Codec.STRING.optionalFieldOf("name", "").forGetter(asset -> asset.name),
          Codec.INT.fieldOf("number").forGetter(asset -> asset.number),
          RocketDesign.CODEC.fieldOf("design").forGetter(asset -> asset.design),
          ResourceLocation.CODEC.fieldOf("destination").forGetter(asset -> asset.destination),
          GlobalPos.CODEC.optionalFieldOf("receiver").forGetter(asset -> Optional.ofNullable(asset.receiver)),
          State.CODEC.fieldOf("state").forGetter(asset -> asset.state),
          Codec.LONG.fieldOf("launched_at").forGetter(asset -> asset.launchedAt),
          Codec.LONG.fieldOf("next_event_at").forGetter(asset -> asset.nextEventAt),
          PayloadState.CODEC.optionalFieldOf("payload").forGetter(asset -> Optional.of(asset.payload)),
          // Saves from before payload states kept the miner's fields on the asset itself
          MinerState.CODEC.forGetter(asset -> LEGACY_EMPTY)
    ).apply(instance, (id, owner, name, number, design, destination, receiver, state, launchedAt, nextEventAt, payload, legacy) ->
          new OrbitalAsset(id, owner, name, number, design, destination, receiver.orElse(null), payload.orElse(legacy), state, launchedAt, nextEventAt)));

    public final UUID id;
    public final UUID owner;
    /** Player given name, empty for the default name. */
    public String name;
    /** Launch number, for the default name. */
    public final int number;
    public final RocketDesign design;
    public final ResourceLocation destination;
    /** Where cargo goes, for payloads that send any. */
    @Nullable
    public GlobalPos receiver;
    /** What the payload does in orbit; its type matches {@code design.payload()}. */
    public final PayloadState payload;
    public State state;
    public final long launchedAt;
    /** Game time (overworld) of the next arrival or batch. */
    public long nextEventAt;

    public OrbitalAsset(UUID id, UUID owner, String name, int number, RocketDesign design, ResourceLocation destination, @Nullable GlobalPos receiver,
          PayloadState payload, State state, long launchedAt, long nextEventAt) {
        this.id = id;
        this.owner = owner;
        this.name = name;
        this.number = number;
        this.design = design;
        this.destination = destination;
        this.receiver = receiver;
        this.payload = payload;
        this.state = state;
        this.launchedAt = launchedAt;
        this.nextEventAt = nextEventAt;
    }

    /** @return the miner state, or {@code null} if this is not an Asteroid Miner */
    @Nullable
    public MinerState miner() {
        return payload instanceof MinerState miner ? miner : null;
    }

    /** @return the satellite state, or {@code null} if this is not a satellite */
    @Nullable
    public SatelliteState satellite() {
        return payload instanceof SatelliteState satellite ? satellite : null;
    }

    /** @return the player given name, or the default name such as "Basic Miner #3" */
    public Component displayName() {
        if (!name.isEmpty()) {
            return Component.literal(name);
        }
        SpaceLang lang = payload instanceof SatelliteState ? SpaceLang.DEFAULT_SATELLITE_NAME : SpaceLang.DEFAULT_MINER_NAME;
        return lang.translate(design.payloadTier().baseTier().getSimpleName(), number);
    }

    /** @return whether the asset has nothing left to do and nothing left to deliver */
    public boolean isDone() {
        return state == State.RETIRED && (!(payload instanceof MinerState miner) || miner.isEmpty());
    }

    public enum State implements StringRepresentable, IHasTextComponent {
        TRAVELLING("travelling", SpaceLang.STATE_TRAVELLING),
        MINING("mining", SpaceLang.STATE_MINING),
        ORBITING("orbiting", SpaceLang.STATE_ORBITING),
        RETIRED("retired", SpaceLang.STATE_RETIRED);

        public static final StringRepresentable.EnumCodec<State> CODEC = StringRepresentable.fromEnum(State::values);
        public static final IntFunction<State> BY_ID = ByIdMap.continuous(State::ordinal, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);
        public static final StreamCodec<ByteBuf, State> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, State::ordinal);

        private final String name;
        private final SpaceLang lang;

        State(String name, SpaceLang lang) {
            this.name = name;
            this.lang = lang;
        }

        @NotNull
        @Override
        public String getSerializedName() {
            return name;
        }

        @NotNull
        @Override
        public Component getTextComponent() {
            return lang.translate();
        }
    }
}
