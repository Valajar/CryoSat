package io.github.valajar.cryosat.space.mission;

import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;

/**
 * What a player can do to one of their assets from Mission Control.
 */
public enum MissionAction {
    /** Sets the name; an empty name goes back to the default name. */
    RENAME,
    /** Links the asset to the receiver stored on the Uplink Card in the Mission Control slot. */
    RELINK,
    /** Clears the receiver, so cargo waits in orbit. */
    UNLINK,
    /** Removes the asset for good, with any cargo still in orbit. */
    DECOMMISSION;

    public static final IntFunction<MissionAction> BY_ID = ByIdMap.continuous(MissionAction::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    public static final StreamCodec<ByteBuf, MissionAction> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, MissionAction::ordinal);
}
