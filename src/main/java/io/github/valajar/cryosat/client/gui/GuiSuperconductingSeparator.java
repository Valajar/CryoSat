package io.github.valajar.cryosat.client.gui;

import io.github.valajar.cryosat.superconductor.Cryostat;
import io.github.valajar.cryosat.superconductor.CryostatState;
import io.github.valajar.cryosat.superconductor.SuperconductorLang;
import io.github.valajar.cryosat.superconductor.separator.SeparatorLang;
import io.github.valajar.cryosat.superconductor.separator.TileEntitySuperconductingSeparator;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.text.EnumColor;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiChemicalGauge;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.inventory.warning.WarningTracker.WarningType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * Liquid nitrogen gauge on the left, nitrogen boil off gauge on the right and a status screen between them: cryostat
 * state (or the cool down left), coolant use, batches separated and a hint when nothing offers it batches. Side config,
 * redstone and security tabs come from Mekanism.
 */
public class GuiSuperconductingSeparator extends GuiConfigurableTile<TileEntitySuperconductingSeparator, MekanismTileContainer<TileEntitySuperconductingSeparator>> {

    public GuiSuperconductingSeparator(MekanismTileContainer<TileEntitySuperconductingSeparator> container, Inventory inv, Component title) {
        super(container, inv, title);
        titleLabelY = 5;
        inventoryLabelY += 2;
        dynamicSlots = true;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        Cryostat cryostat = tile.getCryostat();
        addRenderableWidget(new GuiChemicalGauge(() -> cryostat.coolantTank, () -> tile.getChemicalTanks(null), GaugeType.STANDARD, this, 6, 13));
        addRenderableWidget(new GuiInnerScreen(this, 26, 15, 124, 54, this::screenLines).tooltip(this::tooltipLines));
        addRenderableWidget(new GuiChemicalGauge(() -> cryostat.boilOffTank, () -> tile.getChemicalTanks(null), GaugeType.STANDARD, this, 152, 13))
              .warning(WarningType.NO_SPACE_IN_OUTPUT, () -> cryostat.boilOffTank.getNeeded() == 0);
    }

    private List<Component> screenLines() {
        Cryostat cryostat = tile.getCryostat();
        List<Component> lines = new ArrayList<>();
        if (cryostat.getState() == CryostatState.COOLING_DOWN) {
            lines.add(SuperconductorLang.COOLDOWN_LEFT.translate((cryostat.getCooldown() + 19) / 20));
        } else {
            lines.add(cryostat.getState().getTextComponent());
        }
        lines.add(SuperconductorLang.COOLANT_USE.translate(cryostat.getLastUsed()));
        lines.add(SeparatorLang.BATCHES.translate(tile.getBatchesProcessed()));
        if (!tile.isReceiverAdjacent()) {
            lines.add(SeparatorLang.NO_RECEIVER.translate());
        } else if (!tile.isRedstoneActivated()) {
            lines.add(SeparatorLang.REDSTONE_OFF.translate());
        } else if (cryostat.isSuperconducting()) {
            lines.add(SeparatorLang.READY.translate());
        }
        return lines;
    }

    private List<Component> tooltipLines() {
        return List.of(
              SeparatorLang.TOOLTIP_BONUS.translateColored(EnumColor.GRAY),
              SeparatorLang.TOOLTIP_PER_BATCH.translateColored(EnumColor.GRAY, EnumColor.INDIGO, TileEntitySuperconductingSeparator.COOLANT_PER_BATCH),
              SeparatorLang.TOOLTIP_TRICKLE.translateColored(EnumColor.GRAY, EnumColor.INDIGO, TileEntitySuperconductingSeparator.TRICKLE)
        );
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderTitleText(guiGraphics);
        renderInventoryText(guiGraphics);
        super.drawForegroundText(guiGraphics, mouseX, mouseY);
    }
}
