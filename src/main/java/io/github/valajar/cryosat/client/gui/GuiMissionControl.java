package io.github.valajar.cryosat.client.gui;

import io.github.valajar.cryosat.registries.CryoMenus;
import io.github.valajar.cryosat.space.mission.MissionAction;
import io.github.valajar.cryosat.space.mission.MissionActionPayload;
import io.github.valajar.cryosat.space.mission.MissionControl;
import io.github.valajar.cryosat.space.SpaceLang;
import io.github.valajar.cryosat.space.mission.MissionLang;
import io.github.valajar.cryosat.space.mission.MissionSummary;
import io.github.valajar.cryosat.space.mission.TileEntityMissionControl;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.button.TranslationButton;
import mekanism.client.gui.element.text.BackgroundType;
import mekanism.client.gui.element.text.GuiTextField;
import mekanism.client.gui.element.window.GuiConfirmationDialog;
import mekanism.client.gui.element.window.GuiConfirmationDialog.DialogType;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.core.GlobalPos;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mission Control screen: the viewing player's assets in a scroll list on the left, details of the selected one on the
 * right, and below them the Uplink Card slot, the relink, unlink and decommission buttons and the rename field. The list
 * arrives through container sync; buttons send a {@link MissionActionPayload}.
 */
public class GuiMissionControl extends GuiMekanismTile<TileEntityMissionControl, MekanismTileContainer<TileEntityMissionControl>> {
    private static final int WIDTH = 176 + 2 * CryoMenus.MISSION_CONTROL_INVENTORY_X;

    private GuiMissionList list;
    private GuiTextField nameField;
    private MekanismButton relinkButton;
    private MekanismButton unlinkButton;
    private MekanismButton decommissionButton;

    public GuiMissionControl(MekanismTileContainer<TileEntityMissionControl> container, Inventory inv, Component title) {
        super(container, inv, title);
        imageWidth = WIDTH;
        imageHeight += CryoMenus.MISSION_CONTROL_INVENTORY_Y;
        inventoryLabelX += CryoMenus.MISSION_CONTROL_INVENTORY_X;
        inventoryLabelY = imageHeight - 94;
        titleLabelY = 5;
        dynamicSlots = true;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        list = addRenderableWidget(new GuiMissionList(this, 7, 17, 86, 92, tile::getClientMissions, this::onSelect));
        addRenderableWidget(new GuiInnerScreen(this, 96, 17, 127, 92, this::details).clearFormat().padding(4).clearSpacing());

        int buttonY = TileEntityMissionControl.CARD_SLOT_Y - 1;
        relinkButton = addRenderableWidget(new TranslationButton(this, 27, buttonY, 56, 18, MissionLang.BUTTON_RELINK, (element, mouseX, mouseY) -> {
            send(MissionAction.RELINK, "");
            return true;
        })).setTooltip(MissionLang.RELINK_TOOLTIP);
        unlinkButton = addRenderableWidget(new TranslationButton(this, 85, buttonY, 56, 18, MissionLang.BUTTON_UNLINK, (element, mouseX, mouseY) -> {
            send(MissionAction.UNLINK, "");
            return true;
        }));
        decommissionButton = addRenderableWidget(new TranslationButton(this, 143, buttonY, 80, 18, MissionLang.BUTTON_DECOMMISSION, (element, mouseX, mouseY) -> {
            confirmDecommission();
            return true;
        }));

        nameField = addRenderableWidget(new GuiTextField(this, 40, 134, 183, 12));
        nameField.setMaxLength(MissionControl.MAX_NAME_LENGTH);
        nameField.setBackground(BackgroundType.INNER_SCREEN);
        nameField.setEnterHandler(this::rename);
        nameField.addCheckmarkButton(this::rename);
        updateButtons();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        updateButtons();
    }

    private void updateButtons() {
        MissionSummary selected = list.getSelected();
        relinkButton.active = selected != null && MissionControl.isUplinkCard(tile.getCard());
        unlinkButton.active = selected != null && selected.receiver().isPresent();
        decommissionButton.active = selected != null;
    }

    private void onSelect(@Nullable MissionSummary mission) {
        if (nameField != null) {
            nameField.setText(mission == null ? "" : mission.customName());
        }
        updateButtons();
    }

    private void rename() {
        send(MissionAction.RENAME, nameField.getText());
    }

    private void confirmDecommission() {
        MissionSummary selected = list.getSelected();
        if (selected != null) {
            UUID id = selected.id();
            GuiConfirmationDialog.show(this, MissionLang.DECOMMISSION_CONFIRM.translate(selected.displayName()),
                  () -> PacketDistributor.sendToServer(new MissionActionPayload(MissionAction.DECOMMISSION, id, "")), DialogType.DANGER);
        }
    }

    private void send(MissionAction action, String name) {
        MissionSummary selected = list.getSelected();
        if (selected != null) {
            PacketDistributor.sendToServer(new MissionActionPayload(action, selected.id(), name));
        }
    }

    private List<Component> details() {
        if (tile.getClientMissions().isEmpty()) {
            return List.of(MissionLang.NO_MISSIONS.translate());
        }
        MissionSummary mission = list.getSelected();
        if (mission == null) {
            return List.of(MissionLang.SELECT_MISSION.translate());
        }
        List<Component> lines = new ArrayList<>();
        lines.add(mission.displayName());
        lines.add(MissionLang.DESTINATION.translate(SpaceLang.destinationName(mission.destination())));
        mission.asteroid().ifPresent(asteroid -> {
            lines.add(MissionLang.ASTEROID.translate(SpaceLang.asteroidName(asteroid.asteroidClass()), SpaceLang.sizeName(asteroid.size())));
            lines.add(MissionLang.HARDNESS.translate(String.format("%.1f", asteroid.hardness())));
            lines.add(MissionLang.EXTRACTED.translate(Math.round(asteroid.extracted() * 100) + "%"));
            lines.add(MissionLang.BATCHES.translate(mission.batchesDelivered(), mission.totalBatches()));
        });
        lines.add(switch (mission.state()) {
            case TRAVELLING -> MissionLang.ARRIVES_IN.translate(timeUntil(mission.nextEventAt()));
            case MINING -> MissionLang.NEXT_BATCH_IN.translate(timeUntil(mission.nextEventAt()));
            case RETIRED -> MissionLang.MINING_COMPLETE.translate();
        });
        lines.add(MissionLang.CARGO.translate(mission.cargoInOrbit()));
        lines.add(MissionLang.RECEIVER.translate(mission.receiver().map(GuiMissionControl::position).orElseGet(MissionLang.NOT_LINKED::translate)));
        return lines;
    }

    /**
     * Minutes and seconds until an overworld game time. Every dimension shares the overworld's game time, so the client
     * level's clock works wherever the player is.
     */
    private Component timeUntil(long gameTime) {
        long ticks = tile.getLevel() == null ? 0 : Math.max(0, gameTime - tile.getLevel().getGameTime());
        long seconds = (ticks + 19) / 20;
        return MissionLang.DURATION.translate(seconds / 60, String.format("%02d", seconds % 60));
    }

    private static Component position(GlobalPos pos) {
        ResourceLocation dimension = pos.dimension().location();
        String dimensionName = dimension.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE) ? dimension.getPath() : dimension.toString();
        return MissionLang.RECEIVER_POSITION.translate(pos.pos().getX(), pos.pos().getY(), pos.pos().getZ(), dimensionName);
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderTitleText(guiGraphics);
        renderInventoryText(guiGraphics);
        drawScrollingString(guiGraphics, MissionLang.NAME.translate(), 7, 136, TextAlignment.LEFT, titleTextColor(), 32, 0, false);
        super.drawForegroundText(guiGraphics, mouseX, mouseY);
    }
}
