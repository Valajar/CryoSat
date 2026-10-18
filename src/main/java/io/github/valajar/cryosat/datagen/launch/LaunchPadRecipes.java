package io.github.valajar.cryosat.datagen.launch;

import io.github.valajar.cryosat.registries.CryoBlocks;
import mekanism.api.MekanismAPI;
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
 * Recipes for the Launch Pad's blocks, all early game so a first Basic launch needs no cryogenics.
 */
public final class LaunchPadRecipes {
    /** Called from the main recipe provider. */
    public static void build(RecipeOutput output) {
        Item steelCasing = mekanism("steel_casing");
        Item plating = CryoBlocks.LAUNCH_PAD_PLATING.asItem();
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, plating, 8)
              .pattern("SSS").pattern("SMS").pattern("SSS")
              .define('S', common("ingots/steel"))
              .define('M', steelCasing)
              .unlockedBy("has_steel_casing", has(steelCasing))
              .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CryoBlocks.LAUNCH_PAD_FUEL_PORT)
              .pattern(" H ").pattern("TXT")
              .define('H', Items.HOPPER)
              .define('T', mekanism("basic_pressurized_tube"))
              .define('X', plating)
              .unlockedBy("has_launch_pad_plating", has(plating))
              .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CryoBlocks.LAUNCH_CONTROLLER)
              .pattern(" C ").pattern("GXG").pattern(" C ")
              .define('C', common("circuits/basic"))
              .define('G', Items.GLASS_PANE)
              .define('X', plating)
              .unlockedBy("has_launch_pad_plating", has(plating))
              .save(output);
    }

    private static Item mekanism(String path) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MekanismAPI.MEKANISM_MODID, path));
        if (item == Items.AIR) {
            throw new IllegalStateException("Mekanism item " + path + " not found; is Mekanism on the data run classpath?");
        }
        return item;
    }

    private static TagKey<Item> common(String path) {
        return ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", path));
    }

    private static Criterion<InventoryChangeTrigger.TriggerInstance> has(ItemLike item) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(item);
    }

    private LaunchPadRecipes() {
    }
}
