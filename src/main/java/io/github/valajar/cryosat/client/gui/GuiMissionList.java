package io.github.valajar.cryosat.client.gui;

import io.github.valajar.cryosat.space.OrbitalAsset;
import io.github.valajar.cryosat.space.mission.MissionSummary;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.scroll.GuiScrollList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Mekanism scroll list of orbital assets, two lines per row: the name, and the state underneath in a colour per state.
 * Keeps the selection by asset id, so it survives the list being resynced or reordered.
 */
public class GuiMissionList extends GuiScrollList {
    private static final int ROW_HEIGHT = 18;
    private static final float NAME_SCALE = 0.8F;
    private static final float STATE_SCALE = 0.7F;

    private final Supplier<List<MissionSummary>> missions;
    private final Consumer<@Nullable MissionSummary> onSelect;
    @Nullable
    private UUID selected;

    public GuiMissionList(IGuiWrapper gui, int x, int y, int width, int height, Supplier<List<MissionSummary>> missions,
          Consumer<@Nullable MissionSummary> onSelect) {
        super(gui, x, y, width, height, ROW_HEIGHT, GuiInnerScreen.SCREEN, GuiInnerScreen.SCREEN_SIZE);
        this.missions = missions;
        this.onSelect = onSelect;
    }

    @Override
    protected int getMaxElements() {
        return missions.get().size();
    }

    @Override
    public boolean hasSelection() {
        return getSelected() != null;
    }

    @Nullable
    public MissionSummary getSelected() {
        if (selected != null) {
            for (MissionSummary mission : missions.get()) {
                if (mission.id().equals(selected)) {
                    return mission;
                }
            }
        }
        return null;
    }

    private int selectedIndex() {
        List<MissionSummary> list = missions.get();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(selected)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void setSelected(int index) {
        List<MissionSummary> list = missions.get();
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
        if (selected != null && selectedIndex() == -1) {
            // Decommissioned, possibly from another Mission Control
            clearSelection();
        }
    }

    @Override
    public void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderForeground(guiGraphics, mouseX, mouseY);
        List<MissionSummary> list = missions.get();
        int first = getCurrentSelection();
        for (int i = 0; i < getFocusedElements() && first + i < list.size(); i++) {
            MissionSummary mission = list.get(first + i);
            int y = 2 + ROW_HEIGHT * i;
            drawScaledScrollingString(guiGraphics, mission.displayName(), 0, y, TextAlignment.LEFT, screenTextColor(), barXShift, 2, false, NAME_SCALE);
            drawScaledScrollingString(guiGraphics, stateText(mission.state()), 0, y + 8, TextAlignment.LEFT, screenTextColor(), barXShift, 2, false, STATE_SCALE);
        }
    }

    private static Component stateText(OrbitalAsset.State state) {
        ChatFormatting color = switch (state) {
            case TRAVELLING -> ChatFormatting.YELLOW;
            case MINING -> ChatFormatting.AQUA;
            case RETIRED -> ChatFormatting.GRAY;
        };
        return state.getTextComponent().copy().withStyle(color);
    }

    @Override
    protected void renderElements(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // Selection highlight, as in Mekanism's text scroll list
        int index = selectedIndex();
        int first = getCurrentSelection();
        if (index >= first && index < first + getFocusedElements()) {
            guiGraphics.blit(getResource(), relativeX + 1, relativeY + 1 + (index - first) * ROW_HEIGHT, barXShift - 2, ROW_HEIGHT,
                  4, 2, 2, 2, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }
    }
}
