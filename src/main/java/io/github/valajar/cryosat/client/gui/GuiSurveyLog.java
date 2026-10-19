package io.github.valajar.cryosat.client.gui;

import io.github.valajar.cryosat.space.SpaceLang;
import io.github.valajar.cryosat.space.survey.SurveyLang;
import io.github.valajar.cryosat.space.survey.SurveyResult;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.scroll.GuiScrollList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * The Imaging Terminal's log, newest first, two lines per entry: the target and how long ago, then the reported position
 * with its uncertainty and distance. The x at the right end of a row dismisses that entry.
 */
public class GuiSurveyLog extends GuiScrollList {
    private static final int ROW_HEIGHT = 18;
    private static final float NAME_SCALE = 0.75F;
    private static final float DETAIL_SCALE = 0.65F;
    /** Width of the dismiss x at the right of each row. */
    private static final int DISMISS_WIDTH = 9;

    private final Supplier<List<SurveyResult>> log;
    private final BlockPos terminal;
    private final LongSupplier gameTime;
    private final Consumer<UUID> onDismiss;
    private final Runnable onSelect;
    @Nullable
    private UUID selected;

    public GuiSurveyLog(IGuiWrapper gui, int x, int y, int width, int height, Supplier<List<SurveyResult>> log, BlockPos terminal, LongSupplier gameTime,
          Consumer<UUID> onDismiss, Runnable onSelect) {
        super(gui, x, y, width, height, ROW_HEIGHT, GuiInnerScreen.SCREEN, GuiInnerScreen.SCREEN_SIZE);
        this.log = log;
        this.terminal = terminal;
        this.gameTime = gameTime;
        this.onDismiss = onDismiss;
        this.onSelect = onSelect;
    }

    @Override
    protected int getMaxElements() {
        return log.get().size();
    }

    @Override
    public boolean hasSelection() {
        return getSelected() != null;
    }

    @Nullable
    public SurveyResult getSelected() {
        if (selected != null) {
            for (SurveyResult result : log.get()) {
                if (result.id().equals(selected)) {
                    return result;
                }
            }
        }
        return null;
    }

    private int selectedIndex() {
        List<SurveyResult> list = log.get();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(selected)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        // The x at the end of a row dismisses it instead of selecting it
        if (mouseX >= getX() + barXShift - DISMISS_WIDTH && mouseX < getX() + barXShift - 1 && mouseY >= getY() + 1 && mouseY < getBottom() - 1) {
            int row = (int) ((mouseY - getY() - 1) / ROW_HEIGHT);
            List<SurveyResult> list = log.get();
            int index = getCurrentSelection() + row;
            if (row < getFocusedElements() && index < list.size()) {
                onDismiss.accept(list.get(index).id());
                return;
            }
        }
        super.onClick(mouseX, mouseY, button);
    }

    @Override
    protected void setSelected(int index) {
        List<SurveyResult> list = log.get();
        if (index >= 0 && index < list.size()) {
            selected = list.get(index).id();
            onSelect.run();
        }
    }

    @Override
    public void clearSelection() {
        selected = null;
        onSelect.run();
    }

    @Override
    public void tick() {
        super.tick();
        if (!needsScrollBars()) {
            scroll = 0;
        }
        if (selected != null && selectedIndex() == -1) {
            clearSelection();
        }
    }

    @Override
    public void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderForeground(guiGraphics, mouseX, mouseY);
        List<SurveyResult> list = log.get();
        if (list.isEmpty()) {
            drawScaledScrollingString(guiGraphics, SurveyLang.LOG_EMPTY.translate(), 0, 4, TextAlignment.LEFT, screenTextColor(), barXShift, 2, false, NAME_SCALE);
            return;
        }
        int textWidth = barXShift - DISMISS_WIDTH;
        int first = getCurrentSelection();
        for (int i = 0; i < getFocusedElements() && first + i < list.size(); i++) {
            SurveyResult result = list.get(first + i);
            int y = 2 + ROW_HEIGHT * i;
            Component title = SurveyLang.LOG_TITLE.translate(SpaceLang.surveyTargetName(result.target()), age(result.time()));
            drawScaledScrollingString(guiGraphics, title, 0, y, TextAlignment.LEFT, screenTextColor(), textWidth, 2, false, NAME_SCALE);
            drawScaledScrollingString(guiGraphics, details(result), 0, y + 8, TextAlignment.LEFT, screenTextColor(), textWidth, 2, false, DETAIL_SCALE);
            drawScaledScrollingString(guiGraphics, Component.literal("x").withStyle(ChatFormatting.RED), textWidth, y + 4, TextAlignment.LEFT,
                  screenTextColor(), DISMISS_WIDTH, 2, false, NAME_SCALE);
        }
    }

    private Component details(SurveyResult result) {
        return switch (result.outcome()) {
            case FOUND -> {
                BlockPos pos = result.pos();
                int distance = result.distanceFrom(terminal);
                yield result.uncertainty() > 0 ? SurveyLang.LOG_FOUND.translate(pos.getX(), pos.getZ(), result.uncertainty(), distance)
                                               : SurveyLang.LOG_FOUND_EXACT.translate(pos.getX(), pos.getZ(), distance);
            }
            case NOT_FOUND -> SurveyLang.LOG_NOT_FOUND.translate().copy().withStyle(ChatFormatting.GRAY);
            case NO_DEEP_SCAN -> SurveyLang.LOG_NO_DEEP_SCAN.translate().copy().withStyle(ChatFormatting.RED);
        };
    }

    private Component age(long time) {
        long minutes = Math.max(0, gameTime.getAsLong() - time) / 1_200;
        return minutes < 60 ? SurveyLang.AGE_MINUTES.translate(minutes) : SurveyLang.AGE_HOURS.translate(minutes / 60);
    }

    @Override
    protected void renderElements(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        int index = selectedIndex();
        int first = getCurrentSelection();
        if (index >= first && index < first + getFocusedElements()) {
            guiGraphics.blit(getResource(), relativeX + 1, relativeY + 1 + (index - first) * ROW_HEIGHT, barXShift - 2, ROW_HEIGHT,
                  4, 2, 2, 2, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }
    }
}
