package io.github.valajar.cryosat.space.receiver;

import io.github.valajar.cryosat.registries.CryoDataComponents;
import java.util.List;
import mekanism.api.security.IBlockSecurityUtils;
import mekanism.api.text.EnumColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Points a mission at a Cargo Receiver, the same gesture as Mekanism's Configuration Card: right click a receiver to
 * store it as {@link CryoDataComponents#UPLINK_TARGET}, sneak right click the air to clear it. The Launch Pad and Mission
 * Control read the target from the card.
 */
public class ItemUplinkCard extends Item {
    public ItemUplinkCard(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Nullable
    public static GlobalPos getTarget(ItemStack stack) {
        return stack.get(CryoDataComponents.UPLINK_TARGET);
    }

    /**
     * Runs before the block is used, so a right click on a receiver links instead of opening its GUI.
     */
    @NotNull
    @Override
    public InteractionResult onItemUseFirst(@NotNull ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null || !(level.getBlockEntity(pos) instanceof TileEntityCargoReceiver)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (!IBlockSecurityUtils.INSTANCE.canAccessOrDisplayError(player, level, pos)) {
                return InteractionResult.FAIL;
            }
            stack.set(CryoDataComponents.UPLINK_TARGET, GlobalPos.of(level.dimension(), pos.immutable()));
            player.displayClientMessage(ReceiverLang.UPLINK_SET.translateColored(EnumColor.GRAY, EnumColor.INDIGO, position(pos)), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && stack.has(CryoDataComponents.UPLINK_TARGET)) {
            if (!level.isClientSide) {
                stack.remove(CryoDataComponents.UPLINK_TARGET);
                player.displayClientMessage(ReceiverLang.UPLINK_CLEARED.translateColored(EnumColor.GRAY), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        GlobalPos target = getTarget(stack);
        if (target == null) {
            tooltip.add(ReceiverLang.UPLINK_NO_TARGET.translateColored(EnumColor.GRAY));
        } else {
            tooltip.add(ReceiverLang.UPLINK_TARGET.translateColored(EnumColor.GRAY, EnumColor.INDIGO, position(target.pos()), EnumColor.INDIGO,
                  target.dimension().location().toString()));
        }
        tooltip.add(ReceiverLang.UPLINK_USAGE.translateColored(EnumColor.DARK_GRAY));
    }

    private static Component position(BlockPos pos) {
        return Component.literal(pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
    }
}
