package io.github.valajar.cryosat.client.gui;

import io.github.valajar.cryosat.space.receiver.LinkedMission;
import io.github.valajar.cryosat.space.receiver.ReceiverLang;
import io.github.valajar.cryosat.space.receiver.TileEntityCargoReceiver;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.text.EnumColor;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * A chest sized grid of cargo slots over a small screen listing the missions linked to this receiver; hovering the screen
 * shows each mission in detail. Side config, redstone and security tabs come from Mekanism.
 */
public class GuiCargoReceiver extends GuiConfigurableTile<TileEntityCargoReceiver, MekanismTileContainer<TileEntityCargoReceiver>> {
    private static final int SCREEN_LINES = 3;

    public GuiCargoReceiver(MekanismTileContainer<TileEntityCargoReceiver> container, Inventory inv, Component title) {
        super(container, inv, title);
        imageHeight += TileEntityCargoReceiver.GUI_EXTRA_HEIGHT;
        inventoryLabelY = imageHeight - 94;
        dynamicSlots = true;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        int screenY = TileEntityCargoReceiver.GRID_Y + TileEntityCargoReceiver.ROWS * 18 + 3;
        addRenderableWidget(new GuiInnerScreen(this, 7, screenY, 162, 34, this::screenLines).tooltip(this::tooltipLines));
    }

    private List<Component> screenLines() {
        List<LinkedMission> missions = tile.getLinkedMissions();
        if (missions.isEmpty()) {
            return List.of(ReceiverLang.NO_MISSIONS.translate());
        }
        List<Component> lines = new ArrayList<>();
        boolean overflow = missions.size() > SCREEN_LINES;
        int shown = overflow ? SCREEN_LINES - 1 : missions.size();
        for (int i = 0; i < shown; i++) {
            LinkedMission mission = missions.get(i);
            lines.add(mission.waiting() > 0 ? ReceiverLang.MISSION_LINE_WAITING.translate(mission.name(), mission.state(), mission.waiting())
                                            : ReceiverLang.MISSION_LINE.translate(mission.name(), mission.state()));
        }
        if (overflow) {
            lines.add(ReceiverLang.MORE_MISSIONS.translate(missions.size() - shown));
        }
        return lines;
    }

    private List<Component> tooltipLines() {
        List<LinkedMission> missions = tile.getLinkedMissions();
        if (missions.isEmpty()) {
            return List.of();
        }
        List<Component> lines = new ArrayList<>();
        lines.add(ReceiverLang.MISSIONS.translateColored(EnumColor.GRAY, EnumColor.INDIGO, missions.size()));
        for (LinkedMission mission : missions) {
            lines.add(ReceiverLang.MISSION_LINE.translateColored(EnumColor.GRAY, EnumColor.INDIGO, mission.name(), EnumColor.GRAY, mission.state()));
            if (mission.batches() > 0) {
                lines.add(ReceiverLang.MISSION_BATCHES.translateColored(EnumColor.DARK_GRAY, mission.delivered(), mission.batches()));
            }
            if (mission.waiting() > 0) {
                lines.add(ReceiverLang.MISSION_WAITING.translateColored(EnumColor.DARK_GRAY, mission.waiting()));
            }
        }
        return lines;
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderTitleText(guiGraphics);
        renderInventoryText(guiGraphics);
        super.drawForegroundText(guiGraphics, mouseX, mouseY);
    }
}
