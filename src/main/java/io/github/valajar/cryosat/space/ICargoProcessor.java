package io.github.valajar.cryosat.space;

import java.util.List;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

/**
 * A block that works on cargo batches as they arrive at an adjacent {@link ICargoReceiver}, like the Superconducting
 * Separator. Each batch is offered once, when the miner produces it, and only if the receiver's chunk is loaded.
 */
public interface ICargoProcessor {
    /**
     * @return the processed batch, or {@code null} if this block is not working right now (the next processor gets a turn)
     */
    @Nullable
    List<CargoEntry> processBatch(List<CargoEntry> batch, OrbitalAsset from, RandomSource random);
}
