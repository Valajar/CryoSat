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
          .icon(() -> CryoBlocks.ATMOSPHERIC_COMPRESSOR.asItem().getDefaultInstance())
          .displayItems((parameters, output) -> {
              CryoBlocks.BLOCKS.getSecondaryEntries().forEach(item -> output.accept(item.get()));
              CryoItems.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
          })
          .build());

    private CryoCreativeTabs() {
    }
}
