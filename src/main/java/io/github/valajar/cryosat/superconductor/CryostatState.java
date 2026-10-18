package io.github.valajar.cryosat.superconductor;

import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import mekanism.api.text.IHasTextComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import org.jetbrains.annotations.NotNull;

/**
 * Whether a superconducting block is cold enough to work.
 */
public enum CryostatState implements IHasTextComponent {
    /** Out of coolant: quenched, the effect is off. */
    NO_COOLANT(SuperconductorLang.STATE_NO_COOLANT),
    /** Coolant is back; cooling down again after a quench before the effect returns. */
    COOLING_DOWN(SuperconductorLang.STATE_COOLING_DOWN),
    /** Cold and working. */
    SUPERCONDUCTING(SuperconductorLang.STATE_SUPERCONDUCTING);

    public static final IntFunction<CryostatState> BY_ID = ByIdMap.continuous(CryostatState::ordinal, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);
    public static final StreamCodec<ByteBuf, CryostatState> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, CryostatState::ordinal);

    private final SuperconductorLang lang;

    CryostatState(SuperconductorLang lang) {
        this.lang = lang;
    }

    @NotNull
    @Override
    public Component getTextComponent() {
        return lang.translate();
    }
}
