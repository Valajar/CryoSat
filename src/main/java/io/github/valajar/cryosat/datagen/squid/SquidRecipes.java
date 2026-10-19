package io.github.valajar.cryosat.datagen.squid;

import io.github.valajar.cryosat.registries.CryoBlocks;
import io.github.valajar.cryosat.registries.CryoItems;
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

/**
 * SQUID Array: four Superconducting Coils round a compass (the magnetometer) on a steel casing, run by an elite circuit.
 */
public final class SquidRecipes {
    /** Called from the main recipe provider. */
    public static void build(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CryoBlocks.SQUID_ARRAY)
              .pattern("KCK").pattern("SMS").pattern("KEK")
              .define('K', CryoItems.SUPERCONDUCTING_COIL)
              .define('C', Items.COMPASS)
              .define('S', common("ingots/steel"))
              .define('M', mekanism("steel_casing"))
              .define('E', common("circuits/elite"))
              .unlockedBy("has_superconducting_coil", InventoryChangeTrigger.TriggerInstance.hasItems(CryoItems.SUPERCONDUCTING_COIL))
              .save(output);
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

    private SquidRecipes() {
    }
}
