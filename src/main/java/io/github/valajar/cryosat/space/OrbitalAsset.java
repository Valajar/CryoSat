package io.github.valajar.cryosat.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
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
    public static final Codec<OrbitalAsset> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          UUIDUtil.CODEC.fieldOf("id").forGetter(asset -> asset.id),
          UUIDUtil.CODEC.fieldOf("owner").forGetter(asset -> asset.owner),
          Codec.STRING.optionalFieldOf("name", "").forGetter(asset -> asset.name),
          Codec.INT.fieldOf("number").forGetter(asset -> asset.number),
          RocketDesign.CODEC.fieldOf("design").forGetter(asset -> asset.design),
          ResourceLocation.CODEC.fieldOf("destination").forGetter(asset -> asset.destination),
          GlobalPos.CODEC.optionalFieldOf("receiver").forGetter(asset -> Optional.ofNullable(asset.receiver)),
          AsteroidRoll.CODEC.optionalFieldOf("asteroid").forGetter(asset -> Optional.ofNullable(asset.asteroid)),
          State.CODEC.fieldOf("state").forGetter(asset -> asset.state),
          Codec.LONG.fieldOf("launched_at").forGetter(asset -> asset.launchedAt),
          Codec.LONG.fieldOf("next_event_at").forGetter(asset -> asset.nextEventAt),
          Codec.INT.fieldOf("batches_delivered").forGetter(asset -> asset.batchesDelivered),
          CargoEntry.CODEC.listOf().optionalFieldOf("pending", List.of()).forGetter(asset -> asset.pending)
    ).apply(instance, OrbitalAsset::new));

    public final UUID id;
    public final UUID owner;
    /** Player given name, empty for the default name. */
    public String name;
    /** Launch number, for the default name. */
    public final int number;
    public final RocketDesign design;
    public final ResourceLocation destination;
    @Nullable
    public GlobalPos receiver;
    /** The asteroid an Asteroid Miner works on; rolled at launch, only show it once {@link #state} is past travelling. */
    @Nullable
    public final AsteroidRoll asteroid;
    public State state;
    public final long launchedAt;
    /** Game time (overworld) of the next arrival or batch. */
    public long nextEventAt;
    public int batchesDelivered;
    /** Cargo that arrived but did not fit into the receiver yet. */
    public final List<CargoEntry> pending;

    public OrbitalAsset(UUID id, UUID owner, String name, int number, RocketDesign design, ResourceLocation destination, @Nullable GlobalPos receiver,
          @Nullable AsteroidRoll asteroid, State state, long launchedAt, long nextEventAt, int batchesDelivered, List<CargoEntry> pending) {
        this.id = id;
        this.owner = owner;
        this.name = name;
        this.number = number;
        this.design = design;
        this.destination = destination;
        this.receiver = receiver;
        this.asteroid = asteroid;
        this.state = state;
        this.launchedAt = launchedAt;
        this.nextEventAt = nextEventAt;
        this.batchesDelivered = batchesDelivered;
        this.pending = new ArrayList<>(pending);
    }

    private OrbitalAsset(UUID id, UUID owner, String name, int number, RocketDesign design, ResourceLocation destination, Optional<GlobalPos> receiver,
          Optional<AsteroidRoll> asteroid, State state, long launchedAt, long nextEventAt, int batchesDelivered, List<CargoEntry> pending) {
        this(id, owner, name, number, design, destination, receiver.orElse(null), asteroid.orElse(null), state, launchedAt, nextEventAt, batchesDelivered, pending);
    }

    /** @return the player given name, or the default name such as "Basic Miner #3" */
    public Component displayName() {
        return name.isEmpty() ? SpaceLang.DEFAULT_MINER_NAME.translate(design.payloadTier().baseTier().getSimpleName(), number) : Component.literal(name);
    }

    /** @return whether the asset has nothing left to do and nothing left to deliver */
    public boolean isDone() {
        return state == State.RETIRED && pending.isEmpty();
    }

    public enum State implements StringRepresentable, IHasTextComponent {
        TRAVELLING("travelling", SpaceLang.STATE_TRAVELLING),
        MINING("mining", SpaceLang.STATE_MINING),
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
