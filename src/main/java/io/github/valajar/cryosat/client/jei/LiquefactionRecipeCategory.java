package io.github.valajar.cryosat.client.jei;

import com.mojang.serialization.Codec;
import io.github.valajar.cryosat.CryoLang;
import io.github.valajar.cryosat.recipe.LiquefactionRecipe;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.text.EnumColor;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiChemicalGauge;
import mekanism.client.gui.element.gauge.GuiGauge;
import mekanism.client.recipe_viewer.jei.BaseRecipeCategory;
import mekanism.client.recipe_viewer.type.IRecipeViewerRecipeType;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.UnitDisplayUtils.TemperatureUnit;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.ICodecHelper;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * JEI page for a liquefaction recipe, laid out like the Liquefaction Plant GUI: input gauge, conditions screen, three output
 * gauges.
 */
public class LiquefactionRecipeCategory extends BaseRecipeCategory<LiquefactionRVRecipe> {
    private final GuiGauge<?> input;
    private final List<GuiGauge<?>> outputs = new ArrayList<>();
    private LiquefactionRecipe shown;

    public LiquefactionRecipeCategory(IGuiHelper helper, IRecipeViewerRecipeType<LiquefactionRVRecipe> recipeType) {
        super(helper, recipeType);
        input = addElement(GuiChemicalGauge.getDummy(GaugeType.STANDARD, this, 6, 13).setLabel(CryoLang.RECIPE_INPUT.translateColored(EnumColor.GRAY)));
        for (int i = 0; i < LiquefactionRecipe.MAX_OUTPUTS; i++) {
            outputs.add(addElement(GuiChemicalGauge.getDummy(GaugeType.STANDARD, this, 120 + 18 * i, 13)
                  .setLabel(CryoLang.RECIPE_OUTPUT.translateColored(EnumColor.GRAY, i + 1))));
        }
        addElement(new GuiInnerScreen(this, 26, 17, 92, 56, this::conditions).spacing(1));
    }

    private List<Component> conditions() {
        if (shown == null) {
            return List.of();
        }
        return List.of(
              CryoLang.RECIPE_MAX_TEMPERATURE.translate(MekanismUtils.getTemperatureDisplay(shown.maxTemperature(), TemperatureUnit.KELVIN, true)),
              CryoLang.RECIPE_LATENT_HEAT.translate(shown.latentHeat())
        );
    }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, LiquefactionRVRecipe recipe, @NotNull IFocusGroup focusGroup) {
        shown = recipe.recipe();
        initChemical(builder, RecipeIngredientRole.INPUT, input, recipe.recipe().input().getRepresentations());
        List<mekanism.api.chemical.ChemicalStack> results = recipe.recipe().outputs();
        for (int i = 0; i < results.size(); i++) {
            initChemical(builder, RecipeIngredientRole.OUTPUT, outputs.get(i), List.of(results.get(i)));
        }
    }

    @Override
    public void draw(LiquefactionRVRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView,
          net.minecraft.client.gui.GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // The screen text depends on the recipe being drawn, not on the last one laid out
        shown = recipe.recipe();
        super.draw(recipe, recipeSlotsView, guiGraphics, mouseX, mouseY);
    }

    @Nullable
    @Override
    public ResourceLocation getRegistryName(@NotNull LiquefactionRVRecipe recipe) {
        return recipe.id();
    }

    @NotNull
    @Override
    public Codec<LiquefactionRVRecipe> getCodec(@NotNull ICodecHelper codecHelper, @NotNull IRecipeManager recipeManager) {
        return LiquefactionRVRecipe.CODEC;
    }
}
