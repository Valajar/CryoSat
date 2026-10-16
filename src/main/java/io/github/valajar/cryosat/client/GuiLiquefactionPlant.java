package io.github.valajar.cryosat.client;

import io.github.valajar.cryosat.multiblock.liquefaction.LiquefactionMultiblockData;
import io.github.valajar.cryosat.CryoLang;
import io.github.valajar.cryosat.tile.liquefaction.TileEntityCryoCasing;
import java.util.List;
import mekanism.api.chemical.IChemicalTank;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiChemicalGauge;
import mekanism.client.gui.element.tab.GuiHeatTab;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.UnitDisplayUtils.TemperatureUnit;
import mekanism.common.util.text.EnergyDisplay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * Liquefaction Plant screen: input gauge on the left, the three output gauges on the right, and a status screen in between.
 */
public class GuiLiquefactionPlant extends GuiMekanismTile<TileEntityCryoCasing, MekanismTileContainer<TileEntityCryoCasing>> {

    public GuiLiquefactionPlant(MekanismTileContainer<TileEntityCryoCasing> container, Inventory inventory, Component title) {
        super(container, inventory, title);
        dynamicSlots = true;
    }

    private LiquefactionMultiblockData plant() {
        return tile.getMultiblock();
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        addRenderableWidget(new GuiChemicalGauge(() -> plant().inputTank, this::allTanks, GaugeType.STANDARD, this, 6, 13));
        for (int i = 0; i < LiquefactionMultiblockData.OUTPUT_TANKS; i++) {
            int index = i;
            addRenderableWidget(new GuiChemicalGauge(() -> plant().getOutputTanks().get(index), this::allTanks, GaugeType.STANDARD, this, 120 + 18 * i, 13));
        }
        addRenderableWidget(new GuiInnerScreen(this, 26, 17, 92, 56, () -> {
            LiquefactionMultiblockData plant = plant();
            return List.of(
                  plant.status.getTextComponent(),
                  CryoLang.PLANT_TEMPERATURE.translate(temperature(plant.coldTemperature)),
                  CryoLang.PLANT_HOT_SIDE.translate(temperature(plant.getHotTemperature())),
                  CryoLang.PLANT_COOLERS.translate(plant.coolers, Math.round(plant.lastCooling)),
                  CryoLang.PLANT_ENERGY.translate(EnergyDisplay.of(plant.lastEnergyUsed)),
                  CryoLang.PLANT_RATE.translate(plant.lastProcessed)
            );
        }).padding(3).clearSpacing());
        addRenderableWidget(new GuiHeatTab(this, () -> List.of(
              CryoLang.PLANT_LEAK.translate(Math.round(plant().lastLeak)),
              CryoLang.PLANT_SIZE.translate(plant().length(), plant().height(), plant().width())
        )));
    }

    private List<IChemicalTank> allTanks() {
        return plant().getChemicalTanks(null);
    }

    private static Component temperature(double kelvin) {
        return MekanismUtils.getTemperatureDisplay(kelvin, TemperatureUnit.KELVIN, true);
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        // Between the input gauge and the first output gauge, like Mekanism's evaporation plant
        renderTitleTextWithOffset(graphics, 24, 120);
        renderInventoryText(graphics);
        super.drawForegroundText(graphics, mouseX, mouseY);
    }
}
