package io.github.valajar.cryosat.registries;

import io.github.valajar.cryosat.CryoSat;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CryoCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CryoSat.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
          .title(Component.translatable("itemGroup." + CryoSat.MODID))
          .icon(() -> CryoItems.ATMOSPHERIC_COMPRESSOR.get().getDefaultInstance())
          .displayItems((parameters, output) -> CryoItems.ITEMS.getEntries().forEach(item -> output.accept(item.get())))
          .build());

    private CryoCreativeTabs() {
    }
}
