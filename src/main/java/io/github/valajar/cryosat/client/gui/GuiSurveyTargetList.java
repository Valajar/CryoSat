package io.github.valajar.cryosat.client.gui;

import io.github.valajar.cryosat.space.SpaceLang;
import io.github.valajar.cryosat.space.survey.SurveyLang;
import io.github.valajar.cryosat.space.survey.TerminalView.TargetInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.scroll.GuiScrollList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Scroll list of survey targets: icon, name, and for underground targets a second line saying they need a SQUID Array.
 * Targets that cannot be requested here right now are greyed out. Keeps the selection by target id across resyncs.
 */
public class GuiSurveyTargetList extends GuiScrollList {
    private static final int ROW_HEIGHT = 18;
    private static final float NAME_SCALE = 0.8F;
    private static final float NOTE_SCALE = 0.65F;
    private static final int TEXT_X = 19;

    private final Supplier<List<TargetInfo>> targets;
    private final Consumer<@Nullable TargetInfo> onSelect;
    private final Map<ResourceLocation, ItemStack> icons = new HashMap<>();
    @Nullable
    private ResourceLocation selected;

    public GuiSurveyTargetList(IGuiWrapper gui, int x, int y, int width, int height, Supplier<List<TargetInfo>> targets, Consumer<@Nullable TargetInfo> onSelect) {
        super(gui, x, y, width, height, ROW_HEIGHT, GuiInnerScreen.SCREEN, GuiInnerScreen.SCREEN_SIZE);
        this.targets = targets;
        this.onSelect = onSelect;
    }

    @Override
    protected int getMaxElements() {
        return targets.get().size();
    }

    @Override
    public boolean hasSelection() {
        return getSelected() != null;
    }

    @Nullable
    public TargetInfo getSelected() {
        if (selected != null) {
            for (TargetInfo target : targets.get()) {
                if (target.id().equals(selected)) {
                    return target;
                }
            }
        }
        return null;
    }

    private int selectedIndex() {
        List<TargetInfo> list = targets.get();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(selected)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void setSelected(int index) {
        List<TargetInfo> list = targets.get();
        if (index >= 0 && index < list.size()) {
            selected = list.get(index).id();
            onSelect.accept(list.get(index));
        }
    }

    @Override
    public void clearSelection() {
        selected = null;
        onSelect.accept(null);
    }

    @Override
    public void tick() {
        super.tick();
        if (!needsScrollBars()) {
            scroll = 0;
        }
    }

    @Override
    public void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderForeground(guiGraphics, mouseX, mouseY);
        List<TargetInfo> list = targets.get();
        if (list.isEmpty()) {
            drawScaledScrollingString(guiGraphics, SurveyLang.NO_TARGETS.translate(), 0, 4, TextAlignment.LEFT, screenTextColor(), barXShift, 2, false, NAME_SCALE);
            return;
        }
        int first = getCurrentSelection();
        for (int i = 0; i < getFocusedElements() && first + i < list.size(); i++) {
            TargetInfo target = list.get(first + i);
            int y = 2 + ROW_HEIGHT * i;
            int color = target.available() ? screenTextColor() : 0x707070;
            Component name = SpaceLang.surveyTargetName(target.id());
            if (target.deep()) {
                drawScaledScrollingString(guiGraphics, name, TEXT_X, y, TextAlignment.LEFT, color, barXShift - TEXT_X, 2, false, NAME_SCALE);
                Component note = SurveyLang.DEEP_TARGET.translate().copy().withStyle(target.available() ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY);
                drawScaledScrollingString(guiGraphics, note, TEXT_X, y + 8, TextAlignment.LEFT, color, barXShift - TEXT_X, 2, false, NOTE_SCALE);
            } else {
                drawScaledScrollingString(guiGraphics, name, TEXT_X, y + 4, TextAlignment.LEFT, color, barXShift - TEXT_X, 2, false, NAME_SCALE);
            }
        }
    }

    @Override
    protected void renderElements(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        int first = getCurrentSelection();
        // Selection highlight, as in Mekanism's text scroll list
        int index = selectedIndex();
        if (index >= first && index < first + getFocusedElements()) {
            guiGraphics.blit(getResource(), relativeX + 1, relativeY + 1 + (index - first) * ROW_HEIGHT, barXShift - 2, ROW_HEIGHT,
                  4, 2, 2, 2, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }
        List<TargetInfo> list = targets.get();
        for (int i = 0; i < getFocusedElements() && first + i < list.size(); i++) {
            ItemStack icon = icons.computeIfAbsent(list.get(first + i).icon(), id -> new ItemStack(BuiltInRegistries.ITEM.get(id)));
            gui().renderItem(guiGraphics, icon, relativeX + 2, relativeY + 2 + ROW_HEIGHT * i);
        }
    }
}
