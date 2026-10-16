package io.github.valajar.cryosat.machine;

import io.github.valajar.cryosat.registries.CryoMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

/**
 * Menu shared by all simple machines. It has no slots yet; the screen reads the block entity, which syncs itself.
 */
public class MachineMenu extends AbstractContainerMenu {
    private final MachineBlockEntity machine;
    private final ContainerLevelAccess access;

    public MachineMenu(int id, Inventory inventory, MachineBlockEntity machine) {
        super(CryoMenus.MACHINE.get(), id);
        this.machine = machine;
        this.access = ContainerLevelAccess.create(machine.getLevel(), machine.getBlockPos());
    }

    public static MachineMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        if (inventory.player.level().getBlockEntity(buf.readBlockPos()) instanceof MachineBlockEntity machine) {
            return new MachineMenu(id, inventory, machine);
        }
        throw new IllegalStateException("No CryoSat machine at the position sent by the server");
    }

    public MachineBlockEntity getMachine() {
        return machine;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, machine.getBlockState().getBlock());
    }
}
