package io.github.valajar.cryosat.registries;

import io.github.valajar.cryosat.CryoSat;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Item data components.
 */
public final class CryoDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, CryoSat.MODID);

    /** The Cargo Receiver an Uplink Card points at. Read by the Launch Pad (and Mission Control) to link a mission. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> UPLINK_TARGET = COMPONENTS.registerComponentType("uplink_target",
          builder -> builder.persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC));

    private CryoDataComponents() {
    }
}
