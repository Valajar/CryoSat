package io.github.valajar.cryosat.datagen.plant;

import io.github.valajar.cryosat.registries.CryoBlocks;
import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.recipe.LiquefactionRecipe;
import io.github.valajar.cryosat.registries.CryoChemicals;
import java.util.List;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess;
import net.minecraft.core.Holder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * Recipes for the Liquefaction Plant's parts. Insulation is HDPE; the casing wraps a Mekanism steel casing.
 */
public final class PlantRecipes {
    /** Called from the main recipe provider (recipe providers cannot be renamed, so there can only be one). */
    public static void build(RecipeOutput output) {
        Item casing = CryoBlocks.CRYO_CASING.asItem();
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, casing, 4)
              .pattern("SHS").pattern("HMH").pattern("SHS")
              .define('S', common("ingots/steel"))
              .define('H', mekanism("hdpe_sheet"))
              .define('M', mekanism("steel_casing"))
              .unlockedBy("has_steel_casing", has(mekanism("steel_casing")))
              .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CryoBlocks.CRYO_VALVE)
              .pattern(" C ").pattern("TXT")
              .define('C', common("circuits/basic"))
              .define('T', mekanism("basic_pressurized_tube"))
              .define('X', casing)
              .unlockedBy("has_cryo_casing", has(casing))
              .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CryoBlocks.CRYOCOOLER)
              .pattern(" C ").pattern("AXA").pattern(" E ")
              .define('C', common("circuits/advanced"))
              .define('A', common("alloys/advanced"))
              .define('X', casing)
              .define('E', mekanism("basic_universal_cable"))
              .unlockedBy("has_cryo_casing", has(casing))
              .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CryoBlocks.HEAT_REJECTION_PORT)
              .pattern(" H ").pattern("HXH").pattern(" H ")
              .define('H', mekanism("basic_thermodynamic_conductor"))
              .define('X', casing)
              .unlockedBy("has_cryo_casing", has(casing))
              .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CryoBlocks.LIQUEFACTION_CONTROLLER)
              .pattern(" C ").pattern("GXG").pattern(" C ")
              .define('C', common("circuits/advanced"))
              .define('G', Items.GLASS_PANE)
              .define('X', casing)
              .unlockedBy("has_cryo_casing", has(casing))
              .save(output);

        liquefaction(output);
    }

    /** Liquefaction recipes, written as {@code data/cryosat/recipe/liquefaction/*.json}. */
    private static void liquefaction(RecipeOutput output) {
        liquefaction(output, "compressed_air", CryoChemicals.COMPRESSED_AIR, 100, 77, 2,
              CryoChemicals.LIQUID_NITROGEN.get().getStack(78), CryoChemicals.LIQUID_OXYGEN.get().getStack(21), CryoChemicals.LIQUID_ARGON.get().getStack(1));
        liquefaction(output, "oxygen", mekanismChemical("oxygen"), 100, 90, 2, CryoChemicals.LIQUID_OXYGEN.get().getStack(100));
        liquefaction(output, "nitrogen", CryoChemicals.NITROGEN, 100, 77, 2, CryoChemicals.LIQUID_NITROGEN.get().getStack(100));
        liquefaction(output, "hydrogen", mekanismChemical("hydrogen"), 100, 20, 1, CryoChemicals.LIQUID_HYDROGEN.get().getStack(100));
    }

    private static void liquefaction(RecipeOutput output, String name, Holder<Chemical> input, long amount, double maxTemperature, double latentHeat,
          ChemicalStack... outputs) {
        LiquefactionRecipe recipe = new LiquefactionRecipe(IngredientCreatorAccess.chemicalStack().fromHolder(input, amount), List.of(outputs),
              maxTemperature, latentHeat);
        output.accept(CryoSat.rl("liquefaction/" + name), recipe, null);
    }

    private static Holder<Chemical> mekanismChemical(String path) {
        return MekanismAPI.CHEMICAL_REGISTRY.getHolder(ResourceLocation.fromNamespaceAndPath(MekanismAPI.MEKANISM_MODID, path)).orElseThrow();
    }

    private static Criterion<InventoryChangeTrigger.TriggerInstance> has(ItemLike item) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(item);
    }

    private PlantRecipes() {
    }

    private static Item mekanism(String path) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MekanismAPI.MEKANISM_MODID, path));
        if (item == Items.AIR) {
            throw new IllegalStateException("Mekanism item " + path + " not found");
        }
        return item;
    }

    private static TagKey<Item> common(String path) {
        return ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", path));
    }
}
