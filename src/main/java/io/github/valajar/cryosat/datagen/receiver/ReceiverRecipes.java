package io.github.valajar.cryosat.datagen.receiver;

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
import net.neoforged.neoforge.common.Tags;

/**
 * Recipes for the Cargo Receiver and the Uplink Card, both early game: steel, a basic circuit and copper for the antenna.
 */
public final class ReceiverRecipes {
    /** Called from the main recipe provider. */
    public static void build(RecipeOutput output) {
        Item steelCasing = mekanism("steel_casing");
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CryoBlocks.CARGO_RECEIVER)
              .pattern("SCS").pattern("AMA").pattern("SBS")
              .define('S', common("ingots/steel"))
              .define('C', common("circuits/basic"))
              .define('A', common("ingots/copper"))
              .define('M', steelCasing)
              .define('B', Tags.Items.CHESTS_WOODEN)
              .unlockedBy("has_steel_casing", has(steelCasing))
              .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, CryoItems.UPLINK_CARD)
              .pattern(" A ").pattern("SCS").pattern(" S ")
              .define('A', common("ingots/copper"))
              .define('S', common("ingots/steel"))
              .define('C', common("circuits/basic"))
              .unlockedBy("has_cargo_receiver", has(CryoBlocks.CARGO_RECEIVER))
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

    private ReceiverRecipes() {
    }
}
