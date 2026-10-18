package io.github.valajar.cryosat.space;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import io.github.valajar.cryosat.CryoSat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

/**
 * Every orbital asset of the world, saved with the overworld. Ticks missions on the server: arrival, mining batches and
 * delivery to the linked receiver. Times are overworld game time.
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
        Destination destination = SpaceData.DESTINATIONS.get(destinationId);
        if (destination == null) {
            return MissionProblem.UNKNOWN_DESTINATION;
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
        MissionProblem problem = check(design, destinationId);
        if (problem != null) {
            return Either.right(problem);
        }
        Destination destination = SpaceData.DESTINATIONS.get(destinationId);
        RandomSource random = server.overworld().getRandom();
        AsteroidRoll asteroid = design.payload() == PayloadType.ASTEROID_MINER ? rollAsteroid(design, destinationId, random) : null;
        long now = server.overworld().getGameTime();
        launches++;
        OrbitalAsset asset = new OrbitalAsset(UUID.randomUUID(), owner, "", launches, design, destinationId, receiver, asteroid, OrbitalAsset.State.TRAVELLING,
              now, now + destination.travelTicks(), 0, List.of());
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
        for (Iterator<OrbitalAsset> iterator = assets.values().iterator(); iterator.hasNext(); ) {
            OrbitalAsset asset = iterator.next();
            changed |= advance(server, asset, now);
            if (!asset.pending.isEmpty()) {
                changed |= deliver(server, asset);
            }
        }
        if (changed) {
            setDirty();
        }
    }

    private static boolean advance(MinecraftServer server, OrbitalAsset asset, long now) {
        AsteroidRoll asteroid = asset.asteroid;
        if (asteroid == null || asset.state == OrbitalAsset.State.RETIRED || now < asset.nextEventAt) {
            return false;
        }
        if (asset.state == OrbitalAsset.State.TRAVELLING) {
            asset.state = OrbitalAsset.State.MINING;
            asset.nextEventAt = now + asteroid.batchTicks();
            return true;
        }
        List<CargoEntry> batch = asteroid.batch(asset.batchesDelivered);
        ICargoReceiver receiver = loadedReceiver(server, asset);
        if (receiver != null) {
            batch = receiver.processBatch(batch, asset, server.overworld().getRandom());
        }
        for (CargoEntry entry : batch) {
            CargoEntry.merge(asset.pending, entry);
        }
        asset.batchesDelivered++;
        if (asset.batchesDelivered >= asteroid.batches()) {
            asset.state = OrbitalAsset.State.RETIRED;
        } else {
            asset.nextEventAt = now + asteroid.batchTicks();
        }
        return true;
    }

    /** @return the asset's linked receiver if its chunk is loaded and it is an {@link ICargoReceiver} */
    @Nullable
    private static ICargoReceiver loadedReceiver(MinecraftServer server, OrbitalAsset asset) {
        GlobalPos receiver = asset.receiver;
        if (receiver == null) {
            return null;
        }
        ServerLevel level = server.getLevel(receiver.dimension());
        if (level == null || !level.isLoaded(receiver.pos())) {
            return null;
        }
        return level.getBlockEntity(receiver.pos()) instanceof ICargoReceiver cargoReceiver ? cargoReceiver : null;
    }

    private static boolean deliver(MinecraftServer server, OrbitalAsset asset) {
        GlobalPos receiver = asset.receiver;
        if (receiver == null) {
            return false;
        }
        ServerLevel level = server.getLevel(receiver.dimension());
        if (level == null || !level.isLoaded(receiver.pos())) {
            return false;
        }
        BlockEntity blockEntity = level.getBlockEntity(receiver.pos());
        ICargoReceiver cargoReceiver = blockEntity instanceof ICargoReceiver r ? r : null;
        IItemHandler handler = cargoReceiver == null ? level.getCapability(Capabilities.ItemHandler.BLOCK, receiver.pos(), null) : null;
        if (cargoReceiver == null && handler == null) {
            return false;
        }
        boolean changed = false;
        List<CargoEntry> remaining = new ArrayList<>();
        for (CargoEntry entry : asset.pending) {
            int left = entry.count();
            while (left > 0) {
                ItemStack stack = entry.withCount(left).takeStack();
                int offered = stack.getCount();
                ItemStack rest = cargoReceiver != null ? cargoReceiver.receiveCargo(stack, asset) : ItemHandlerHelper.insertItemStacked(handler, stack, false);
                int accepted = offered - rest.getCount();
                left -= accepted;
                changed |= accepted > 0;
                if (accepted < offered) {
                    break;
                }
            }
            if (left > 0) {
                remaining.add(entry.withCount(left));
            }
        }
        asset.pending.clear();
        asset.pending.addAll(remaining);
        return changed;
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
