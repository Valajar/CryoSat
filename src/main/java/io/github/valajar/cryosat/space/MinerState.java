package io.github.valajar.cryosat.space;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

/**
 * An Asteroid Miner: travels to its asteroid, mines it in batches and sends them to the linked receiver. Mutable; change it
 * only on the server thread.
 */
public final class MinerState implements PayloadState {
    public static final MapCodec<MinerState> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
          AsteroidRoll.CODEC.optionalFieldOf("asteroid").forGetter(state -> Optional.ofNullable(state.asteroid)),
          com.mojang.serialization.Codec.INT.optionalFieldOf("batches_delivered", 0).forGetter(state -> state.batchesDelivered),
          CargoEntry.CODEC.listOf().optionalFieldOf("pending", List.of()).forGetter(state -> state.pending)
    ).apply(instance, (asteroid, batches, pending) -> new MinerState(asteroid.orElse(null), batches, pending)));

    /** The asteroid it works on; rolled at launch, only show it once the asset is past travelling. */
    @Nullable
    public final AsteroidRoll asteroid;
    public int batchesDelivered;
    /** Cargo that arrived but did not fit into the receiver yet. */
    public final List<CargoEntry> pending;

    public MinerState(@Nullable AsteroidRoll asteroid, int batchesDelivered, List<CargoEntry> pending) {
        this.asteroid = asteroid;
        this.batchesDelivered = batchesDelivered;
        this.pending = new ArrayList<>(pending);
    }

    @Override
    public PayloadType type() {
        return PayloadType.ASTEROID_MINER;
    }

    @Override
    public boolean tick(MinecraftServer server, OrbitalAsset asset, long now) {
        boolean changed = advance(server, asset, now);
        if (!pending.isEmpty()) {
            changed |= deliver(server, asset);
        }
        return changed;
    }

    private boolean advance(MinecraftServer server, OrbitalAsset asset, long now) {
        if (asteroid == null || asset.state == OrbitalAsset.State.RETIRED || now < asset.nextEventAt) {
            return false;
        }
        if (asset.state == OrbitalAsset.State.TRAVELLING) {
            asset.state = OrbitalAsset.State.MINING;
            asset.nextEventAt = now + asteroid.batchTicks();
            return true;
        }
        List<CargoEntry> batch = asteroid.batch(batchesDelivered);
        ICargoReceiver receiver = loadedReceiver(server, asset);
        if (receiver != null) {
            batch = receiver.processBatch(batch, asset, server.overworld().getRandom());
        }
        for (CargoEntry entry : batch) {
            CargoEntry.merge(pending, entry);
        }
        batchesDelivered++;
        if (batchesDelivered >= asteroid.batches()) {
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

    private boolean deliver(MinecraftServer server, OrbitalAsset asset) {
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
        for (CargoEntry entry : pending) {
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
        pending.clear();
        pending.addAll(remaining);
        return changed;
    }

    /** @return whether there is nothing left to deliver */
    public boolean isEmpty() {
        return pending.isEmpty();
    }
}
