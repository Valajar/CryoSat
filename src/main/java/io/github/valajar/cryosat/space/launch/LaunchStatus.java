package io.github.valajar.cryosat.space.launch;

import java.util.function.IntFunction;
import mekanism.api.text.IHasTextComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ByIdMap;
import org.jetbrains.annotations.NotNull;

/**
 * What the Launch Pad is waiting for, in the order it is checked. The two problem states come with a
 * {@link io.github.valajar.cryosat.space.MissionProblem} that the GUI shows instead of the generic text.
 */
public enum LaunchStatus implements IHasTextComponent {
    /** The assembly slots do not make a rocket yet. */
    ASSEMBLY_PROBLEM(LaunchLang.STATUS_ASSEMBLY_PROBLEM),
    READY_TO_ASSEMBLE(LaunchLang.STATUS_READY_TO_ASSEMBLE),
    /** The assembled rocket cannot go to the selected destination. */
    MISSION_PROBLEM(LaunchLang.STATUS_MISSION_PROBLEM),
    NEEDS_FUEL(LaunchLang.STATUS_NEEDS_FUEL),
    OBSTRUCTED(LaunchLang.STATUS_OBSTRUCTED),
    /** Only returned by a launch attempt: nobody owns the pad and nobody pressed the button. */
    NO_OWNER(LaunchLang.STATUS_NO_OWNER),
    READY(LaunchLang.STATUS_READY),
    LAUNCHING(LaunchLang.STATUS_LAUNCHING);

    public static final IntFunction<LaunchStatus> BY_ID = ByIdMap.continuous(LaunchStatus::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);

    private final LaunchLang lang;

    LaunchStatus(LaunchLang lang) {
        this.lang = lang;
    }

    @NotNull
    @Override
    public Component getTextComponent() {
        return lang.translate();
    }
}
