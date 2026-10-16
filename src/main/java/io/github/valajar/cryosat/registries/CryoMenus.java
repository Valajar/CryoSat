package io.github.valajar.cryosat.registries;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.machine.MachineMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CryoMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, CryoSat.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> MACHINE = MENUS.register("machine",
          () -> IMenuTypeExtension.create(MachineMenu::fromNetwork));

    private CryoMenus() {
    }
}
