package io.github.valajar.cryosat.datagen.mission;

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
import net.neoforged.neoforge.common.Tags;

/**
 * Mission Control: a steel casing behind a screen, with a Guidance Computer to track the missions. Reachable as early as
 * the Basic rocket parts.
 */
public final class MissionRecipes {
    public static void build(RecipeOutput output) {
        Item casing = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MekanismAPI.MEKANISM_MODID, "steel_casing"));
        if (casing == Items.AIR) {
            throw new IllegalStateException("Mekanism steel casing not found");
        }
        TagKey<Item> steel = common("ingots/steel");
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CryoBlocks.MISSION_CONTROL)
              .pattern("GGG").pattern("CMC").pattern("SPS")
              .define('G', Tags.Items.GLASS_PANES)
              .define('C', common("circuits/basic"))
              .define('M', casing)
              .define('S', steel)
              .define('P', CryoItems.GUIDANCE_COMPUTER)
              .unlockedBy("has_guidance_computer", InventoryChangeTrigger.TriggerInstance.hasItems(CryoItems.GUIDANCE_COMPUTER))
              .save(output);
    }

    private static TagKey<Item> common(String path) {
        return ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", path));
    }

    private MissionRecipes() {
    }
}
