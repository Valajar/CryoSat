package io.github.valajar.cryosat.multiblock.liquefaction;

import io.github.valajar.cryosat.CryoLang;
import mekanism.api.text.IHasTextComponent;
import net.minecraft.network.chat.Component;

/**
 * What a Cryo Valve does: feed the input tank, or drain (and auto eject) one of the output tanks.
 */
public enum ValveMode implements IHasTextComponent {
    INPUT(-1),
    OUTPUT_1(0),
    OUTPUT_2(1),
    OUTPUT_3(2);

    private final int outputIndex;

    ValveMode(int outputIndex) {
        this.outputIndex = outputIndex;
    }

    public int outputIndex() {
        return outputIndex;
    }

    public static ValveMode output(int index) {
        return values()[index + 1];
    }

    public ValveMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    @Override
    public Component getTextComponent() {
        return this == INPUT ? CryoLang.VALVE_MODE_INPUT.translate() : CryoLang.VALVE_MODE_OUTPUT.translate(outputIndex + 1);
    }
}
