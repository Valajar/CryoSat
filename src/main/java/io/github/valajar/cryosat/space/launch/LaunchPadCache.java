package io.github.valajar.cryosat.space.launch;

import io.github.valajar.cryosat.space.AsteroidClass;
import io.github.valajar.cryosat.space.RocketDesign;
import java.util.List;
import mekanism.common.lib.multiblock.MultiblockCache;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Keeps the assembled rocket, its parts, the destination and a launch in progress across unloads and reforms. Slots and
 * tanks are handled by the base cache.
 */
public class LaunchPadCache extends MultiblockCache<LaunchPadMultiblockData> {
    @Nullable
    private RocketDesign design;
    private List<ItemStack> parts = List.of();
    private ResourceLocation destination = AsteroidClass.ASTEROID_BELT;
    private long launchStartedAt = -1;

    @Override
    public void merge(MultiblockCache<LaunchPadMultiblockData> mergeCache, RejectContents rejectContents) {
        super.merge(mergeCache, rejectContents);
        LaunchPadCache other = (LaunchPadCache) mergeCache;
        if (design == null && other.design != null) {
            design = other.design;
            parts = other.parts;
            destination = other.destination;
            launchStartedAt = other.launchStartedAt;
        } else if (other.design != null) {
            // Two rockets cannot share a pad: hand the other one's parts back as rejected contents
            other.parts.forEach(part -> rejectContents.rejectedItems.add(part.copy()));
        }
    }

    @Override
    public void apply(HolderLookup.Provider provider, LaunchPadMultiblockData data) {
        super.apply(provider, data);
        data.loadLaunchState(design, parts, destination, launchStartedAt);
    }

    @Override
    public void sync(LaunchPadMultiblockData data) {
        super.sync(data);
        design = data.getDesign();
        parts = data.getParts();
        destination = data.getDestination();
        launchStartedAt = data.getLaunchStartedAt();
    }

    @Override
    public void load(HolderLookup.Provider provider, CompoundTag nbt) {
        super.load(provider, nbt);
        design = nbt.contains("design") ? RocketDesign.CODEC.parse(NbtOps.INSTANCE, nbt.get("design")).result().orElse(null) : null;
        parts = nbt.contains("parts", Tag.TAG_LIST)
                ? ItemStack.OPTIONAL_CODEC.listOf().parse(provider.createSerializationContext(NbtOps.INSTANCE), nbt.get("parts")).result().orElse(List.of())
                : List.of();
        ResourceLocation id = ResourceLocation.tryParse(nbt.getString("destination"));
        destination = id == null ? AsteroidClass.ASTEROID_BELT : id;
        launchStartedAt = nbt.contains("launch_started_at") ? nbt.getLong("launch_started_at") : -1;
    }

    @Override
    public void save(HolderLookup.Provider provider, CompoundTag nbt) {
        super.save(provider, nbt);
        if (design != null) {
            RocketDesign.CODEC.encodeStart(NbtOps.INSTANCE, design).ifSuccess(encoded -> nbt.put("design", encoded));
        }
        ItemStack.OPTIONAL_CODEC.listOf().encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), parts).ifSuccess(encoded -> nbt.put("parts", encoded));
        nbt.putString("destination", destination.toString());
        nbt.putLong("launch_started_at", launchStartedAt);
    }
}
