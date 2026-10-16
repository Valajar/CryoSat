package io.github.valajar.cryosat.multiblock.liquefaction;

import io.github.valajar.cryosat.CryoLang;
import mekanism.api.text.IHasTextComponent;
import net.minecraft.network.chat.Component;

/**
 * Why the plant is or is not liquefying right now, shown on the controller screen.
 */
public enum PlantStatus implements IHasTextComponent {
    IDLE(CryoLang.PLANT_STATUS_IDLE),
    RUNNING(CryoLang.PLANT_STATUS_RUNNING),
    COOLING_DOWN(CryoLang.PLANT_STATUS_COOLING_DOWN),
    NO_COOLERS(CryoLang.PLANT_STATUS_NO_COOLERS),
    OUTPUT_FULL(CryoLang.PLANT_STATUS_OUTPUT_FULL),
    OUTPUT_MISMATCH(CryoLang.PLANT_STATUS_OUTPUT_MISMATCH),
    OVERHEATED(CryoLang.PLANT_STATUS_OVERHEATED);

    private final CryoLang lang;

    PlantStatus(CryoLang lang) {
        this.lang = lang;
    }

    @Override
    public Component getTextComponent() {
        return lang.translate();
    }
}
