package io.github.valajar.cryosat.client.gui;

import io.github.valajar.cryosat.CryoLang;
import io.github.valajar.cryosat.machine.MachineStatus;
import io.github.valajar.cryosat.machine.compressor.AtmosphericCompressorBlockEntity;
import java.util.List;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiChemicalGauge;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.common.MekanismLang;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.inventory.warning.WarningTracker.WarningType;
import mekanism.common.util.UnitDisplayUtils;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * Laid out like Mekanism's Electric Pump: output gauge on the left with its drain slot, a status screen in the middle,
 * energy slot and bar on the right. Side config, upgrade, redstone, security and energy tabs come from Mekanism.
 */
public class GuiAtmosphericCompressor extends GuiConfigurableTile<AtmosphericCompressorBlockEntity, MekanismTileContainer<AtmosphericCompressorBlockEntity>> {

    public GuiAtmosphericCompressor(MekanismTileContainer<AtmosphericCompressorBlockEntity> container, Inventory inv, Component title) {
        super(container, inv, title);
        titleLabelY = 5;
        inventoryLabelY += 2;
        dynamicSlots = true;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        addRenderableWidget(new GuiInnerScreen(this, 50, 15, 88, 54, () -> List.of(
              MekanismLang.STATUS.translate(tile.getStatus()),
              CryoLang.OUTPUT_RATE.translate(String.format("%.1f", tile.getCurrentRate())),
              CryoLang.ENERGY_PER_MB.translate(energy(tile.getEnergyPerMb()))
        )));
        addRenderableWidget(new GuiChemicalGauge(tile::getOutputTank, () -> tile.getChemicalTanks(null), GaugeType.STANDARD, this, 6, 13))
              .warning(WarningType.NO_SPACE_IN_OUTPUT, () -> tile.getStatus() == MachineStatus.OUTPUT_FULL || tile.getStatus() == MachineStatus.OUTPUT_MISMATCH);
        addRenderableWidget(new GuiVerticalPowerBar(this, tile.getEnergyContainer(), 164, 15))
              .warning(WarningType.NOT_ENOUGH_ENERGY, () -> tile.getStatus() == MachineStatus.NO_POWER);
        addRenderableWidget(new GuiEnergyTab(this, tile.getEnergyContainer(), tile::getLastEnergyUsed));
    }

    private static Component energy(double joules) {
        EnergyUnit unit = EnergyUnit.getConfigured();
        return UnitDisplayUtils.getDisplayShort(joules / unit.getConversion(), unit);
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderTitleText(guiGraphics);
        renderInventoryText(guiGraphics);
        super.drawForegroundText(guiGraphics, mouseX, mouseY);
    }
}
