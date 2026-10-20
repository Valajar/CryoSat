package io.github.valajar.cryosat.client.gui;

import io.github.valajar.cryosat.registries.CryoItems;
import io.github.valajar.cryosat.space.MissionProblem;
import io.github.valajar.cryosat.space.RocketDesign;
import io.github.valajar.cryosat.space.RocketTier.Fuel;
import io.github.valajar.cryosat.space.SpaceLang;
import io.github.valajar.cryosat.space.SpaceLang;
import io.github.valajar.cryosat.space.launch.LaunchLang;
import io.github.valajar.cryosat.space.launch.LaunchPadAction;
import io.github.valajar.cryosat.space.launch.LaunchPadMultiblockData;
import io.github.valajar.cryosat.space.launch.LaunchStatus;
import io.github.valajar.cryosat.space.launch.TileEntityLaunchController;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.text.EnumColor;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.button.TranslationButton;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiChemicalGauge;
import mekanism.client.gui.element.slot.GuiSlot;
import mekanism.client.gui.element.slot.SlotType;
import mekanism.common.inventory.container.slot.InventoryContainerSlot;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

/**
 * Rocket Assembly: fuel gauges and the grain slot on the left, the four part slots and the Uplink Card slot in a column,
 * then the status screen, the destination selector and the Assemble, Disassemble and Launch buttons.
 */
public class GuiRocketAssembly extends GuiMekanismTile<TileEntityLaunchController, MekanismTileContainer<TileEntityLaunchController>> {
    private MekanismButton destinationButton;
    private MekanismButton assembleButton;
    private MekanismButton disassembleButton;
    private MekanismButton launchButton;

    public GuiRocketAssembly(MekanismTileContainer<TileEntityLaunchController> container, Inventory inventory, Component title) {
        super(container, inventory, title);
        // Matches the menu's 50 pixel inventory offset
        imageHeight += 50;
        inventoryLabelY = imageHeight - 94;
        dynamicSlots = true;
    }

    private LaunchPadMultiblockData pad() {
        return tile.getMultiblock();
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        addRenderableWidget(new GuiChemicalGauge(() -> pad().oxidiserTank, () -> pad().getChemicalTanks(null), GaugeType.STANDARD, this, 6, 16))
              .setLabel(LaunchLang.GAUGE_OXIDISER.translateColored(EnumColor.GRAY));
        addRenderableWidget(new GuiChemicalGauge(() -> pad().propellantTank, () -> pad().getChemicalTanks(null), GaugeType.STANDARD, this, 26, 16))
              .setLabel(LaunchLang.GAUGE_PROPELLANT.translateColored(EnumColor.GRAY));
        addRenderableWidget(new GuiInnerScreen(this, 70, 16, 100, 52, this::screenLines).padding(3).clearSpacing());
        destinationButton = addRenderableWidget(new MekanismButton(this, 70, 71, 100, 14, destinationText(),
              (element, mouseX, mouseY) -> send(LaunchPadAction.Action.NEXT_DESTINATION),
              (element, mouseX, mouseY) -> send(LaunchPadAction.Action.PREVIOUS_DESTINATION)))
              .setTooltip(LaunchLang.DESTINATION_TOOLTIP);
        assembleButton = addRenderableWidget(new TranslationButton(this, 70, 88, 46, 14, LaunchLang.BUTTON_ASSEMBLE,
              (element, mouseX, mouseY) -> send(LaunchPadAction.Action.ASSEMBLE)));
        disassembleButton = addRenderableWidget(new TranslationButton(this, 118, 88, 52, 14, LaunchLang.BUTTON_DISASSEMBLE,
              (element, mouseX, mouseY) -> send(LaunchPadAction.Action.DISASSEMBLE)));
        launchButton = addRenderableWidget(new TranslationButton(this, 70, 105, 100, 14, LaunchLang.BUTTON_LAUNCH,
              (element, mouseX, mouseY) -> send(LaunchPadAction.Action.LAUNCH)));
        updateButtons();
    }

    /** Slots with a hint of what goes in them while they are empty. */
    @Override
    protected void addSlots() {
        LaunchPadMultiblockData pad = pad();
        Map<IInventorySlot, Component> hints = new IdentityHashMap<>();
        hints.put(pad.engineSlot, SpaceLang.PART_ENGINE.translate());
        hints.put(pad.fuelTankSlot, SpaceLang.PART_FUEL_TANK.translate());
        hints.put(pad.guidanceSlot, SpaceLang.PART_GUIDANCE_COMPUTER.translate());
        hints.put(pad.payloadSlot, SpaceLang.PART_PAYLOAD.translate());
        hints.put(pad.uplinkSlot, LaunchLang.SLOT_UPLINK.translate());
        hints.put(pad.solidFuelSlot, LaunchLang.SLOT_SOLID_FUEL.translate());
        for (Slot slot : menu.slots) {
            GuiSlot guiSlot = new GuiSlot(SlotType.NORMAL, this, slot.x - 1, slot.y - 1);
            if (slot instanceof InventoryContainerSlot containerSlot && hints.containsKey(containerSlot.getInventorySlot())) {
                Component hint = hints.get(containerSlot.getInventorySlot());
                guiSlot.hover(element -> slot.hasItem() ? List.of() : List.of(hint));
            }
            addRenderableWidget(guiSlot);
        }
    }

    /** @return true, the click is handled */
    private boolean send(LaunchPadAction.Action action) {
        PacketDistributor.sendToServer(new LaunchPadAction(tile.getBlockPos(), action));
        return true;
    }

    private List<Component> screenLines() {
        LaunchPadMultiblockData pad = pad();
        List<Component> lines = new ArrayList<>();
        MissionProblem problem = pad.getProblem();
        lines.add(problem != null ? problem.getTextComponent() : pad.status.getTextComponent());
        RocketDesign design = pad.getDesign();
        if (design == null) {
            lines.add(LaunchLang.NO_ROCKET.translate());
            return lines;
        }
        lines.add(LaunchLang.ROCKET.translate(Component.literal(design.tier().baseTier().getSimpleName())
              .withStyle(style -> style.withColor(design.tier().baseTier().getColor()))));
        lines.add(LaunchLang.PAYLOAD.translate(CryoItems.payload(design.payload(), design.payloadTier()).getDescription()));
        Fuel fuel = design.tier().fuel().orElse(null);
        if (fuel != null && fuel.isSolid()) {
            lines.add(LaunchLang.FUEL_GRAINS.translate(pad.solidFuelSlot.getCount(), fuel.solidCount()));
        } else if (fuel != null) {
            lines.add(LaunchLang.FUEL_OXIDISER.translate(pad.oxidiserTank.getStored(), fuel.oxidiserAmount()));
            lines.add(LaunchLang.FUEL_PROPELLANT.translate(pad.propellantTank.getStored(), fuel.propellantAmount()));
        }
        return lines;
    }

    private Component destinationText() {
        return LaunchLang.DESTINATION.translate(SpaceLang.destinationName(pad().getDestination()));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        updateButtons();
    }

    private void updateButtons() {
        LaunchPadMultiblockData pad = pad();
        boolean launching = pad.status == LaunchStatus.LAUNCHING;
        destinationButton.setMessage(destinationText());
        destinationButton.active = !launching;
        assembleButton.active = pad.getDesign() == null && pad.status == LaunchStatus.READY_TO_ASSEMBLE;
        disassembleButton.active = pad.getDesign() != null && !launching;
        launchButton.active = pad.status == LaunchStatus.READY;
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        renderTitleText(graphics);
        renderInventoryText(graphics);
        super.drawForegroundText(graphics, mouseX, mouseY);
    }
}
