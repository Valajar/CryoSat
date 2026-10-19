package io.github.valajar.cryosat.datagen.survey;

import io.github.valajar.cryosat.registries.CryoBlocks;
import mekanism.api.MekanismAPI;
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
import net.neoforged.neoforge.common.Tags;

/**
 * Imaging Terminal: a screen over a steel casing, advanced circuits to process the imagery and a spyglass for the optics.
 * About as far along as the Basic rocket that puts the first Survey Satellite up.
 */
public final class SurveyRecipes {
    public static void build(RecipeOutput output) {
        Item casing = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MekanismAPI.MEKANISM_MODID, "steel_casing"));
        if (casing == Items.AIR) {
            throw new IllegalStateException("Mekanism steel casing not found");
        }
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CryoBlocks.IMAGING_TERMINAL)
              .pattern("GGG").pattern("CMC").pattern("SYS")
              .define('G', Tags.Items.GLASS_PANES)
              .define('C', common("circuits/advanced"))
              .define('M', casing)
              .define('S', common("ingots/steel"))
              .define('Y', Items.SPYGLASS)
              .unlockedBy("has_steel_casing", InventoryChangeTrigger.TriggerInstance.hasItems(casing))
              .save(output);
    }

    private static TagKey<Item> common(String path) {
        return ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", path));
    }

    private SurveyRecipes() {
    }
}
