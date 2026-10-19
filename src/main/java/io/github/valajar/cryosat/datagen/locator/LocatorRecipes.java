package io.github.valajar.cryosat.datagen.locator;

import io.github.valajar.cryosat.registries.CryoItems;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Survey Locator recipe, mid game: a compass in a steel housing with an advanced circuit and an osmium antenna.
 */
public final class LocatorRecipes {
    /** Called from the main recipe provider. */
    public static void build(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, CryoItems.SURVEY_LOCATOR)
              .pattern(" O ").pattern("SKS").pattern("SCS")
              .define('O', common("ingots/osmium"))
              .define('S', common("ingots/steel"))
              .define('K', Items.COMPASS)
              .define('C', common("circuits/advanced"))
              .unlockedBy("has_compass", InventoryChangeTrigger.TriggerInstance.hasItems(Items.COMPASS))
              .save(output);
    }

    private static TagKey<Item> common(String path) {
        return ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", path));
    }

    private LocatorRecipes() {
    }
}
