package io.github.valajar.cryosat.client.gui;

import io.github.valajar.cryosat.registries.CryoMenus;
import io.github.valajar.cryosat.space.SpaceLang;
import io.github.valajar.cryosat.space.survey.ImagingAction;
import io.github.valajar.cryosat.space.survey.ImagingActionPayload;
import io.github.valajar.cryosat.space.survey.SurveyLang;
import io.github.valajar.cryosat.space.survey.SurveyResult;
import io.github.valajar.cryosat.space.survey.TerminalView.CoverageInfo;
import io.github.valajar.cryosat.space.survey.TerminalView.PassInfo;
import io.github.valajar.cryosat.space.survey.TerminalView.TargetInfo;
import io.github.valajar.cryosat.space.survey.TileEntityImagingTerminal;
import java.util.ArrayList;
import java.util.List;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.bar.GuiHorizontalPowerBar;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.button.TranslationButton;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.client.gui.element.tab.window.GuiSideConfigurationTab;
import mekanism.common.MekanismLang;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.util.text.EnergyDisplay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

/**
 * Imaging Terminal screen: survey targets on the left with the request button, the constellation's figures and the passes in
 * progress top right, the log under them, and along the bottom the Survey Locator slot with its load button and the energy
 * slot and bar. Lists and figures arrive through container sync; buttons send an {@link ImagingActionPayload}.
 */
public class GuiImagingTerminal extends GuiMekanismTile<TileEntityImagingTerminal, MekanismTileContainer<TileEntityImagingTerminal>> {
    private static final int WIDTH = 176 + 2 * CryoMenus.IMAGING_TERMINAL_INVENTORY_X;
    private static final int SHOWN_PASSES = 2;

    private GuiSideConfigurationTab<TileEntityImagingTerminal> sideConfigTab;
    private GuiSurveyTargetList targets;
    private GuiSurveyLog log;
    private MekanismButton requestButton;
    private MekanismButton loadButton;

    public GuiImagingTerminal(MekanismTileContainer<TileEntityImagingTerminal> container, Inventory inv, Component title) {
        super(container, inv, title);
        imageWidth = WIDTH;
        imageHeight += CryoMenus.IMAGING_TERMINAL_INVENTORY_Y;
        inventoryLabelX += CryoMenus.IMAGING_TERMINAL_INVENTORY_X;
        inventoryLabelY = imageHeight - 94;
        titleLabelY = 5;
        dynamicSlots = true;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        sideConfigTab = addRenderableWidget(new GuiSideConfigurationTab<>(this, tile, () -> sideConfigTab));

        targets = addRenderableWidget(new GuiSurveyTargetList(this, 7, 17, 96, 110, tile::getClientTargets, target -> updateButtons()));
        requestButton = addRenderableWidget(new TranslationButton(this, 7, 129, 96, 18, SurveyLang.BUTTON_REQUEST, (element, mouseX, mouseY) -> {
            TargetInfo target = targets.getSelected();
            if (target != null) {
                PacketDistributor.sendToServer(ImagingActionPayload.request(target.id()));
            }
            return true;
        }));

        addRenderableWidget(new GuiInnerScreen(this, 106, 17, 135, 50, this::status).clearFormat().padding(3).clearSpacing().textScale(0.75F));
        log = addRenderableWidget(new GuiSurveyLog(this, 106, 70, 135, 56, tile::getLog, tile.getBlockPos(), this::gameTime,
              entry -> PacketDistributor.sendToServer(ImagingActionPayload.onEntry(ImagingAction.DISMISS, entry)), this::updateButtons));

        loadButton = addRenderableWidget(new TranslationButton(this, 126, 129, 46, 18, SurveyLang.BUTTON_LOAD, (element, mouseX, mouseY) -> {
            SurveyResult selected = log.getSelected();
            if (selected != null) {
                PacketDistributor.sendToServer(ImagingActionPayload.onEntry(ImagingAction.LOAD, selected.id()));
            }
            return true;
        })).setTooltip(SurveyLang.LOAD_TOOLTIP);
        addRenderableWidget(new GuiHorizontalPowerBar(this, tile.getEnergyContainer(), 176, 136, 44));
        addRenderableWidget(new GuiEnergyTab(this, () -> List.of(
              SurveyLang.ENERGY_PER_REQUEST.translate(EnergyDisplay.of(tile.getClientCoverage().cost())),
              MekanismLang.STORING.translate(EnergyDisplay.of(tile.getEnergyContainer())))));
        updateButtons();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        updateButtons();
    }

    private void updateButtons() {
        if (requestButton == null || loadButton == null) {
            return;
        }
        TargetInfo target = targets.getSelected();
        CoverageInfo coverage = tile.getClientCoverage();
        requestButton.active = target != null && target.available() && coverage.free() > 0;
        SurveyResult entry = log.getSelected();
        loadButton.active = entry != null && entry.found() && TileEntityImagingTerminal.isLocator(tile.getLocatorSlot().getStack());
    }

    private List<Component> status() {
        CoverageInfo coverage = tile.getClientCoverage();
        List<Component> lines = new ArrayList<>();
        if (coverage.satellites() == 0) {
            lines.add(SurveyLang.NO_SATELLITES.translate());
            return lines;
        }
        lines.add(SurveyLang.SATELLITES.translate(coverage.satellites(), coverage.free()));
        lines.add(SurveyLang.COVERAGE.translate(Math.round(coverage.coverage() * 100) + "%", coverage.range()));
        lines.add(coverage.uncertainty() > 0 ? SurveyLang.UNCERTAINTY.translate(coverage.uncertainty()) : SurveyLang.EXACT.translate());
        List<PassInfo> passes = tile.passes();
        for (int i = 0; i < passes.size() && i < SHOWN_PASSES; i++) {
            PassInfo pass = passes.get(i);
            lines.add(SurveyLang.PASS.translate(SpaceLang.surveyTargetName(pass.target()), timeUntil(pass.completesAt())));
        }
        if (passes.size() > SHOWN_PASSES) {
            lines.add(SurveyLang.MORE_PASSES.translate(passes.size() - SHOWN_PASSES));
        }
        return lines;
    }

    /** Every dimension shares the overworld's game time, so the client level's clock works wherever the terminal is. */
    private long gameTime() {
        return tile.getLevel() == null ? 0 : tile.getLevel().getGameTime();
    }

    private Component timeUntil(long time) {
        long seconds = (Math.max(0, time - gameTime()) + 19) / 20;
        return SurveyLang.DURATION.translate(seconds / 60, String.format("%02d", seconds % 60));
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderTitleText(guiGraphics);
        renderInventoryText(guiGraphics);
        super.drawForegroundText(guiGraphics, mouseX, mouseY);
    }
}
