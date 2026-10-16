package io.github.valajar.cryosat.client;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.machine.MachineBlockEntity;
import io.github.valajar.cryosat.machine.MachineMenu;
import io.github.valajar.cryosat.machine.compressor.AtmosphericCompressorBlockEntity;
import java.text.NumberFormat;
import java.util.List;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Generic machine screen: energy gauge, one gauge per chemical tank, and a status line. Drawn entirely in code with the
 * Mekanism steel palette until proper GUI textures exist.
 */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    private static final int PANEL = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF373737;
    private static final int GAUGE_BG = 0xFF151515;
    private static final int ENERGY = 0xFF3DD16B;
    private static final int TEXT = 0xFF404040;
    private static final int GAUGE_TOP = 20;
    private static final int GAUGE_HEIGHT = 52;
    private static final NumberFormat NUMBERS = NumberFormat.getIntegerInstance();

    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 96;
    }

    private MachineBlockEntity machine() {
        return menu.getMachine();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, PANEL_DARK);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, PANEL_LIGHT);
        graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, PANEL);

        // Energy gauge
        drawGauge(graphics, x + 8, y + GAUGE_TOP, 8, ENERGY, fraction(machine().getEnergy().getEnergy(), machine().getEnergy().getMaxEnergy()));

        // Chemical gauges
        List<IChemicalTank> tanks = machine().getTanks();
        for (int i = 0; i < tanks.size(); i++) {
            IChemicalTank tank = tanks.get(i);
            ChemicalStack stack = tank.getStack();
            int color = stack.isEmpty() ? 0 : 0xFF000000 | stack.getChemical().getTint();
            drawGauge(graphics, x + tankX(i), y + GAUGE_TOP, 16, color, fraction(stack.getAmount(), tank.getCapacity()));
        }
    }

    private static int tankX(int index) {
        return 152 - index * 22;
    }

    private static double fraction(long amount, long capacity) {
        return capacity <= 0 ? 0 : Math.min(1, amount / (double) capacity);
    }

    private static void drawGauge(GuiGraphics graphics, int x, int y, int width, int color, double fill) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + GAUGE_HEIGHT + 1, PANEL_DARK);
        graphics.fill(x, y, x + width, y + GAUGE_HEIGHT, GAUGE_BG);
        int height = (int) Math.round(GAUGE_HEIGHT * fill);
        if (height > 0) {
            graphics.fill(x, y + GAUGE_HEIGHT - height, x + width, y + GAUGE_HEIGHT, color);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, TEXT, false);
        graphics.drawString(font, machine().getStatus().text(), 24, GAUGE_TOP + 2, TEXT, false);
        if (machine() instanceof AtmosphericCompressorBlockEntity compressor && compressor.getCurrentRate() > 0) {
            graphics.drawString(font, Component.translatable("gui." + CryoSat.MODID + ".rate", String.format("%.1f", compressor.getCurrentRate())),
                  24, GAUGE_TOP + 14, TEXT, false);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);
        int relX = mouseX - leftPos, relY = mouseY - topPos;
        if (relY < GAUGE_TOP || relY >= GAUGE_TOP + GAUGE_HEIGHT) {
            return;
        }
        if (relX >= 8 && relX < 16) {
            graphics.renderTooltip(font, Component.translatable("gui." + CryoSat.MODID + ".energy",
                  NUMBERS.format(machine().getEnergy().getEnergy()), NUMBERS.format(machine().getEnergy().getMaxEnergy())), mouseX, mouseY);
            return;
        }
        List<IChemicalTank> tanks = machine().getTanks();
        for (int i = 0; i < tanks.size(); i++) {
            if (relX >= tankX(i) && relX < tankX(i) + 16) {
                IChemicalTank tank = tanks.get(i);
                Component name = tank.isEmpty() ? Component.translatable("gui." + CryoSat.MODID + ".empty") : tank.getStack().getTextComponent();
                graphics.renderComponentTooltip(font, List.of(name, Component.translatable("gui." + CryoSat.MODID + ".amount",
                      NUMBERS.format(tank.getStored()), NUMBERS.format(tank.getCapacity()))), mouseX, mouseY);
            }
        }
    }
}
