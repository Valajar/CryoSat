package io.github.valajar.cryosat.space;

import java.util.List;
import mekanism.api.text.EnumColor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A crafted rocket part that goes into a Rocket Assembly slot. Tiered parts show their name in the tier's colour.
 */
public class RocketPartItem extends Item {
    private final Kind kind;
    @Nullable
    private final RocketTier tier;
    @Nullable
    private final PayloadType payload;

    public RocketPartItem(Properties properties, Kind kind, @Nullable RocketTier tier, @Nullable PayloadType payload) {
        super(properties.stacksTo(kind == Kind.PAYLOAD ? 1 : 16));
        this.kind = kind;
        this.tier = tier;
        this.payload = payload;
    }

    public Kind kind() {
        return kind;
    }

    /** @return the part's tier, or {@code null} for untiered parts (the guidance computer) */
    @Nullable
    public RocketTier tier() {
        return tier;
    }

    /** @return the payload this item is, or {@code null} if it is not a payload */
    @Nullable
    public PayloadType payload() {
        return payload;
    }

    @NotNull
    @Override
    public Component getName(@NotNull ItemStack stack) {
        Component name = super.getName(stack);
        return tier == null ? name : name.copy().withStyle(style -> style.withColor(tier.baseTier().getColor()));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(SpaceLang.PART_TOOLTIP.translateColored(EnumColor.GRAY, kind.lang));
    }

    public static boolean is(ItemStack stack, Kind kind) {
        return stack.getItem() instanceof RocketPartItem part && part.kind == kind;
    }

    public enum Kind {
        ENGINE(SpaceLang.PART_ENGINE),
        FUEL_TANK(SpaceLang.PART_FUEL_TANK),
        GUIDANCE_COMPUTER(SpaceLang.PART_GUIDANCE_COMPUTER),
        PAYLOAD(SpaceLang.PART_PAYLOAD);

        private final SpaceLang lang;

        Kind(SpaceLang lang) {
            this.lang = lang;
        }

        public SpaceLang lang() {
            return lang;
        }
    }
}
