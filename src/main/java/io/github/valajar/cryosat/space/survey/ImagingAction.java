package io.github.valajar.cryosat.space.survey;

import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;

/**
 * What a player can do at an Imaging Terminal.
 */
public enum ImagingAction {
    /** Requests an imaging pass for the target. */
    REQUEST,
    /** Removes the log entry. */
    DISMISS,
    /** Writes the log entry onto the Survey Locator in the slot. */
    LOAD;

    public static final IntFunction<ImagingAction> BY_ID = ByIdMap.continuous(ImagingAction::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    public static final StreamCodec<ByteBuf, ImagingAction> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, ImagingAction::ordinal);
}
