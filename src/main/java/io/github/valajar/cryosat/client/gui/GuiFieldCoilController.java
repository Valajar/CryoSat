package io.github.valajar.cryosat.client.gui;

import io.github.valajar.cryosat.registries.CryoMenus;
import io.github.valajar.cryosat.superconductor.Cryostat;
import io.github.valajar.cryosat.superconductor.CryostatState;
import io.github.valajar.cryosat.superconductor.SuperconductorLang;
import io.github.valajar.cryosat.superconductor.fusion.FieldCoilRing;
import io.github.valajar.cryosat.superconductor.fusion.FusionCoilLang;
import io.github.valajar.cryosat.superconductor.fusion.RingProblem;
import io.github.valajar.cryosat.superconductor.fusion.TileEntityFieldCoilController;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.text.EnumColor;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiChemicalGauge;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.inventory.warning.WarningTracker.WarningType;
import mekanism.common.util.UnitDisplayUtils;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * Liquid nitrogen gauge on the left, nitrogen boil off gauge on the right, and a status screen between them: cooling state,
 * what is wrong with the ring (and where), loops, the reactor's own output, the bonus and the coolant use. Side config,
 * redstone and security tabs come from Mekanism.
 */
public class GuiFieldCoilController extends GuiConfigurableTile<TileEntityFieldCoilController, MekanismTileContainer<TileEntityFieldCoilController>> {

    public GuiFieldCoilController(MekanismTileContainer<TileEntityFieldCoilController> container, Inventory inv, Component title) {
        super(container, inv, title);
        imageHeight += CryoMenus.FIELD_COIL_EXTRA_HEIGHT;
        titleLabelY = 5;
        inventoryLabelY = imageHeight - 94;
        dynamicSlots = true;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        Cryostat cryostat = tile.getCryostat();
        // No warning on the coolant gauge: Mekanism has no "input missing" warning, and the screen already says quenched
        addRenderableWidget(new GuiChemicalGauge(() -> cryostat.coolantTank, () -> tile.getChemicalTanks(null), GaugeType.STANDARD, this, 6, 13));
        addRenderableWidget(new GuiChemicalGauge(() -> cryostat.boilOffTank, () -> tile.getChemicalTanks(null), GaugeType.STANDARD, this, 152, 13))
              .warning(WarningType.NO_SPACE_IN_OUTPUT, () -> cryostat.boilOffTank.getNeeded() == 0);
        addRenderableWidget(new GuiInnerScreen(this, 26, 13, 124, 96, this::screenLines).tooltip(this::tooltipLines));
    }

    private List<Component> screenLines() {
        Cryostat cryostat = tile.getCryostat();
        List<Component> lines = new ArrayList<>();
        if (cryostat.getState() == CryostatState.COOLING_DOWN) {
            lines.add(cryostat.getState().getTextComponent().copy().append(", ")
                  .append(SuperconductorLang.COOLDOWN_LEFT.translate((cryostat.getCooldown() + 19) / 20)));
        } else {
            lines.add(cryostat.getState().getTextComponent());
        }
        lines.add(ringLine());
        if (tile.getProblem() == RingProblem.NONE) {
            lines.add(FusionCoilLang.LOOPS.translate(tile.getLoops(), FieldCoilRing.MAX_LOOPS, tile.getLoops() * 25));
            lines.add(tile.isReactorBurning() ? FusionCoilLang.REACTOR_OUTPUT.translate(energy(tile.getLastPassive())) : FusionCoilLang.REACTOR_IDLE.translate());
            lines.add(FusionCoilLang.BONUS.translate(energy(tile.getLastBonus())));
        }
        lines.add(SuperconductorLang.COOLANT_USE.translate(tile.getLastCoolant()));
        if (!tile.canFunction()) {
            lines.add(FusionCoilLang.REDSTONE_OFF.translate());
        }
        BlockPos incomplete = tile.getIncompletePos();
        if (incomplete != null && tile.getProblem() == RingProblem.NONE) {
            lines.add(FusionCoilLang.INCOMPLETE_LOOP.translate(incomplete.getX(), incomplete.getY(), incomplete.getZ()));
        }
        lines.add(FusionCoilLang.STEAM_NOTE.translateColored(tile.isWaterCooled() ? EnumColor.YELLOW : EnumColor.GRAY));
        return lines;
    }

    private Component ringLine() {
        RingProblem problem = tile.getProblem();
        BlockPos pos = tile.getProblemPos();
        if (problem == RingProblem.NONE || problem == RingProblem.WRONG_GAP) {
            return problem.lang().translate(tile.getGap());
        } else if (pos != null) {
            return problem.lang().translate(pos.getX(), pos.getY(), pos.getZ());
        }
        return problem.lang().translate();
    }

    private List<Component> tooltipLines() {
        return List.of(
              FusionCoilLang.BOOST_RULE.translateColored(EnumColor.GRAY),
              FusionCoilLang.COOLANT_RULE.translateColored(EnumColor.GRAY)
        );
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
