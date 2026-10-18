package io.github.valajar.cryosat.datagen.separator;

import io.github.valajar.cryosat.registries.CryoBlocks;
import io.github.valajar.cryosat.registries.CryoItems;
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
 * Superconducting Coil and Superconducting Separator, mid game: the coil needs Mekanism HDPE (Pressurized Reaction
 * Chamber) and advanced alloy, a step past the Basic rocket parts, and the separator also needs a Cryo Casing from the
 * Liquefaction Plant line.
 */
public final class SeparatorRecipes {
    /** Called from the main recipe provider. */
    public static void build(RecipeOutput output) {
        Item hdpe = mekanism("hdpe_sheet");
        // Copper windings insulated with HDPE around an osmium former, stiffened with advanced alloy
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CryoItems.SUPERCONDUCTING_COIL, 2)
              .pattern("PCP").pattern("AOA").pattern("PCP")
              .define('P', hdpe)
              .define('C', common("ingots/copper"))
              .define('A', common("alloys/advanced"))
              .define('O', common("ingots/osmium"))
              .unlockedBy("has_hdpe_sheet", has(hdpe))
              .save(output);

        Item steelCasing = mekanism("steel_casing");
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CryoBlocks.SUPERCONDUCTING_SEPARATOR)
              .pattern("KCK").pattern("EME").pattern("KSK")
              .define('K', CryoItems.SUPERCONDUCTING_COIL)
              .define('C', CryoBlocks.CRYO_CASING)
              .define('E', common("circuits/advanced"))
              .define('M', steelCasing)
              .define('S', common("ingots/steel"))
              .unlockedBy("has_superconducting_coil", has(CryoItems.SUPERCONDUCTING_COIL))
              .save(output);
    }

    private static Criterion<InventoryChangeTrigger.TriggerInstance> has(ItemLike item) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(item);
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

    private SeparatorRecipes() {
    }
}
