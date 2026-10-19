package io.github.valajar.cryosat.space;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import io.github.valajar.cryosat.CryoSat;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * Every orbital asset of the world, saved with the overworld. Ticks each asset's {@link PayloadState} on the server. Times
 * are overworld game time.
 */
public class OrbitalRegistry extends SavedData {
    private static final String DATA_NAME = CryoSat.MODID + "_orbital_registry";
    /** Time per mining batch at hardness 1: a quarter of a Minecraft day. */
    public static final int BATCH_TICKS = 6_000;
    /** How often missions are processed. */
    public static final int TICK_INTERVAL = 20;

    private static final Factory<OrbitalRegistry> FACTORY = new Factory<>(OrbitalRegistry::new, OrbitalRegistry::load, null);

    private final Map<UUID, OrbitalAsset> assets = new LinkedHashMap<>();
    private int launches;

    public static OrbitalRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    // Queries

    public Collection<OrbitalAsset> all() {
        return assets.values();
    }

    public List<OrbitalAsset> ownedBy(UUID owner) {
        return assets.values().stream().filter(asset -> asset.owner.equals(owner)).toList();
    }

    @Nullable
    public OrbitalAsset get(UUID id) {
        return assets.get(id);
    }

    // Launching

    /**
     * @return why this design cannot go to the destination, or {@code null} if it can
     */
    @Nullable
    public static MissionProblem check(RocketDesign design, ResourceLocation destinationId) {
        return check(design, destinationId, null);
    }

    /**
     * @param from dimension the rocket launches from, or {@code null} to skip that check
     *
     * @return why this design cannot go to the destination from there, or {@code null} if it can
     */
    @Nullable
    public static MissionProblem check(RocketDesign design, ResourceLocation destinationId, @Nullable ResourceKey<Level> from) {
        Destination destination = SpaceData.DESTINATIONS.get(destinationId);
        if (destination == null) {
            return MissionProblem.UNKNOWN_DESTINATION;
        } else if (from != null && !destination.canLaunchFrom(from)) {
            return MissionProblem.WRONG_DIMENSION;
        } else if (!design.tier().atLeast(destination.minTier())) {
            return MissionProblem.DESTINATION_OUT_OF_REACH;
        } else if (!destination.payloads().contains(design.payload())) {
            return MissionProblem.PAYLOAD_NOT_USABLE;
        } else if (design.payload() == PayloadType.ASTEROID_MINER && reachableClasses(design, destinationId).isEmpty()) {
            return MissionProblem.NOTHING_TO_MINE;
        }
        return null;
    }

    /**
     * Puts a payload into orbit. The caller (the Launch Pad) has already consumed the rocket and its fuel.
     *
     * @param receiver where cargo goes, or {@code null} to keep it in orbit until a receiver is linked
     *
     * @return the new asset, or why it could not launch
     */
    public Either<OrbitalAsset, MissionProblem> launch(MinecraftServer server, UUID owner, RocketDesign design, ResourceLocation destinationId,
          @Nullable GlobalPos receiver) {
        return launch(server, owner, design, destinationId, receiver, Level.OVERWORLD);
    }

    /**
     * @param from dimension the rocket launches from; satellites orbit it
     */
    public Either<OrbitalAsset, MissionProblem> launch(MinecraftServer server, UUID owner, RocketDesign design, ResourceLocation destinationId,
          @Nullable GlobalPos receiver, ResourceKey<Level> from) {
        MissionProblem problem = check(design, destinationId, from);
        if (problem != null) {
            return Either.right(problem);
        }
        Destination destination = SpaceData.DESTINATIONS.get(destinationId);
        RandomSource random = server.overworld().getRandom();
        PayloadState payload = switch (design.payload()) {
            case ASTEROID_MINER -> new MinerState(rollAsteroid(design, destinationId, random), 0, List.of());
            case SURVEY_SATELLITE -> new SatelliteState(from, Orbit.roll(random), null);
        };
        long now = server.overworld().getGameTime();
        launches++;
        OrbitalAsset asset = new OrbitalAsset(UUID.randomUUID(), owner, "", launches, design, destinationId, receiver, payload, OrbitalAsset.State.TRAVELLING,
              now, now + destination.travelTicks());
        assets.put(asset.id, asset);
        setDirty();
        return Either.left(asset);
    }

    private static List<Entry<ResourceLocation, AsteroidClass>> reachableClasses(RocketDesign design, ResourceLocation destination) {
        return SpaceData.ASTEROIDS.all().entrySet().stream()
              .filter(entry -> entry.getValue().destination().equals(destination) && design.tier().atLeast(entry.getValue().minTier()))
              .sorted(Entry.comparingByKey())
              .toList();
    }

    private static AsteroidRoll rollAsteroid(RocketDesign design, ResourceLocation destination, RandomSource random) {
        List<Entry<ResourceLocation, AsteroidClass>> classes = reachableClasses(design, destination);
        Entry<ResourceLocation, AsteroidClass> chosen = classes.getLast();
        int roll = random.nextInt(classes.stream().mapToInt(entry -> entry.getValue().weight()).sum());
        for (Entry<ResourceLocation, AsteroidClass> entry : classes) {
            roll -= entry.getValue().weight();
            if (roll < 0) {
                chosen = entry;
                break;
            }
        }
        AsteroidSize size = rollSize(design.tier(), random);
        float extracted = Math.min(1, design.payloadTier().minerCapacity() / (float) size.multiplier());
        AsteroidClass asteroidClass = chosen.getValue();
        return new AsteroidRoll(chosen.getKey(), size, asteroidClass.rollHardness(random), extracted,
              asteroidClass.rollCargo(random, size.multiplier() * extracted));
    }

    /** Rolls an asteroid size with the odds of the rocket's tier. */
    public static AsteroidSize rollSize(RocketTier tier, RandomSource random) {
        int total = 0;
        for (AsteroidSize size : AsteroidSize.values()) {
            total += tier.sizeWeight(size);
        }
        int roll = random.nextInt(total);
        for (AsteroidSize size : AsteroidSize.values()) {
            roll -= tier.sizeWeight(size);
            if (roll < 0) {
                return size;
            }
        }
        return AsteroidSize.SMALL;
    }

    // Player actions (Mission Control)

    /** @param name the new name, empty to go back to the default name */
    public void rename(OrbitalAsset asset, String name) {
        asset.name = name.strip();
        setDirty();
    }

    public void relink(OrbitalAsset asset, @Nullable GlobalPos receiver) {
        asset.receiver = receiver;
        setDirty();
    }

    /** Removes an asset for good, including cargo it has not delivered yet. */
    public void decommission(OrbitalAsset asset) {
        if (assets.remove(asset.id) != null) {
            setDirty();
        }
    }

    // Ticking

    public void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        boolean changed = false;
        for (OrbitalAsset asset : assets.values()) {
            changed |= asset.payload.tick(server, asset, now);
        }
        if (changed) {
            setDirty();
        }
    }

    // Satellites

    /**
     * Queues an imaging pass on a free satellite of the viewer's constellation over the terminal's dimension.
     *
     * @return the task, or empty if every satellite is busy (or there are none)
     */
    public Optional<OrbitalTask> requestSurvey(MinecraftServer server, UUID requester, GlobalPos terminal, ResourceLocation target) {
        Constellation constellation = Constellation.of(server, requester, terminal.dimension());
        for (OrbitalAsset asset : constellation.satellites()) {
            SatelliteState satellite = asset.satellite();
            if (satellite != null && satellite.isFree()) {
                long now = server.overworld().getGameTime();
                OrbitalTask task = new OrbitalTask(UUID.randomUUID(), requester, terminal, target, now + SatelliteState.passTicks(asset.design.payloadTier()));
                satellite.task = task;
                setDirty();
                return Optional.of(task);
            }
        }
        return Optional.empty();
    }

    // Saving

    private static final Codec<List<OrbitalAsset>> ASSETS_CODEC = OrbitalAsset.CODEC.listOf();

    private static OrbitalRegistry load(CompoundTag tag, HolderLookup.Provider provider) {
        OrbitalRegistry registry = new OrbitalRegistry();
        registry.launches = tag.getInt("launches");
        ASSETS_CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), tag.get("assets"))
              .resultOrPartial(error -> CryoSat.LOGGER.error("Could not load orbital assets: {}", error))
              .ifPresent(list -> list.forEach(asset -> registry.assets.put(asset.id, asset)));
        return registry;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt("launches", launches);
        ASSETS_CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), List.copyOf(assets.values()))
              .resultOrPartial(error -> CryoSat.LOGGER.error("Could not save orbital assets: {}", error))
              .ifPresent(encoded -> tag.put("assets", encoded));
        return tag;
    }
}
