package io.github.valajar.cryosat.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * An amount of one item, unbounded by stack size. Asteroid totals and undelivered cargo are kept as these.
 */
public record CargoEntry(Holder<Item> item, int count) {
    public static final Codec<CargoEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("item").forGetter(CargoEntry::item),
          ExtraCodecs.POSITIVE_INT.fieldOf("count").forGetter(CargoEntry::count)
    ).apply(instance, CargoEntry::new));

    public CargoEntry withCount(int count) {
        return new CargoEntry(item, count);
    }

    /** @return one stack of at most the item's max stack size */
    public ItemStack takeStack() {
        return new ItemStack(item, Math.min(count, item.value().getDefaultMaxStackSize()));
    }

    /** Adds an entry to a list, merging it into an existing entry of the same item. */
    public static void merge(List<CargoEntry> list, CargoEntry entry) {
        for (int i = 0; i < list.size(); i++) {
            CargoEntry existing = list.get(i);
            if (existing.item.is(entry.item)) {
                list.set(i, existing.withCount(existing.count + entry.count));
                return;
            }
        }
        list.add(entry);
    }
}
