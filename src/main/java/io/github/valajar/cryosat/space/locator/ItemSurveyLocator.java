package io.github.valajar.cryosat.space.locator;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.registries.CryoDataComponents;
import io.github.valajar.cryosat.space.LocatorData;
import io.github.valajar.cryosat.space.SpaceLang;
import java.util.List;
import mekanism.api.text.EnumColor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * A tech compass holding survey results ({@link CryoDataComponents#LOCATOR_DATA}), loaded at an Imaging Terminal. Held in hand
 * its needle points at the selected target and a waypoint beam marks it (both client side, in {@code client/locator}). Sneak
 * use switches to the next stored target. Needs no energy.
 */
public class ItemSurveyLocator extends Item {
    /** Item property the needle frames switch on, registered on the client, values 0 to 1 clockwise from straight ahead. */
    public static final ResourceLocation NEEDLE_ANGLE = CryoSat.rl("angle");

    public ItemSurveyLocator(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.UNCOMMON).component(CryoDataComponents.LOCATOR_DATA, LocatorData.EMPTY));
    }

    public static LocatorData getData(ItemStack stack) {
        return stack.getOrDefault(CryoDataComponents.LOCATOR_DATA, LocatorData.EMPTY);
    }

    /** @return the same targets with the next one selected, wrapping round; unchanged when there are none */
    public static LocatorData cycle(LocatorData data) {
        if (data.targets().isEmpty()) {
            return data;
        }
        return new LocatorData(data.targets(), (Math.floorMod(data.selected(), data.targets().size()) + 1) % data.targets().size());
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            LocatorData data = cycle(getData(stack));
            LocatorData.Entry entry = data.current();
            if (entry == null) {
                player.displayClientMessage(LocatorLang.NO_TARGETS.translateColored(EnumColor.GRAY), true);
            } else {
                stack.set(CryoDataComponents.LOCATOR_DATA, data);
                player.displayClientMessage(LocatorLang.SELECTED.translateColored(EnumColor.GRAY, EnumColor.INDIGO, data.selected() + 1,
                      EnumColor.INDIGO, data.targets().size(), EnumColor.INDIGO, SpaceLang.surveyTargetName(entry.target()), EnumColor.INDIGO,
                      position(entry.pos().pos()), precision(entry)), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        LocatorData data = getData(stack);
        if (data.targets().isEmpty()) {
            tooltip.add(LocatorLang.NO_TARGETS.translateColored(EnumColor.GRAY));
            return;
        }
        int selected = Math.floorMod(data.selected(), data.targets().size());
        for (int i = 0; i < data.targets().size(); i++) {
            LocatorData.Entry entry = data.targets().get(i);
            boolean current = i == selected;
            Component line = LocatorLang.ENTRY.translateColored(current ? EnumColor.WHITE : EnumColor.GRAY, current ? EnumColor.INDIGO : EnumColor.GRAY,
                  SpaceLang.surveyTargetName(entry.target()), position(entry.pos().pos()), precision(entry));
            tooltip.add(Component.literal(current ? "> " : "  ").withStyle(ChatFormatting.BLUE).append(line));
        }
        tooltip.add(LocatorLang.USAGE.translateColored(EnumColor.DARK_GRAY));
    }

    /** Survey results come from the structure search, whose height means nothing, so only x and z are shown (like {@code /locate}). */
    private static Component position(BlockPos pos) {
        return Component.literal(pos.getX() + ", " + pos.getZ());
    }

    private static Component precision(LocatorData.Entry entry) {
        return entry.uncertainty() <= 0 ? LocatorLang.EXACT.translate() : LocatorLang.WITHIN.translate(entry.uncertainty());
    }
}
