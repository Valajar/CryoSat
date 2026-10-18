package io.github.valajar.cryosat.space;

import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * A block entity that catches cargo from orbit (the Cargo Receiver). The {@link OrbitalRegistry} delivers to the linked
 * position while its chunk is loaded; whatever does not fit stays in orbit and is retried. Blocks that only expose an
 * item handler capability are delivered to as well, which the game tests use with a chest.
 */
public interface ICargoReceiver {
    /**
     * @param stack one stack of cargo, at most its max stack size
     * @param from  the asset delivering it
     *
     * @return what did not fit
     */
    ItemStack receiveCargo(ItemStack stack, OrbitalAsset from);

    /**
     * Called once per batch when a miner produces it, while this receiver is loaded, before the batch is delivered. Receivers
     * pass it to adjacent {@link ICargoProcessor}s.
     */
    default List<CargoEntry> processBatch(List<CargoEntry> batch, OrbitalAsset from, RandomSource random) {
        return batch;
    }
}
