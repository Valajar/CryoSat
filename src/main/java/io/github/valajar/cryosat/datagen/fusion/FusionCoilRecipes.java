package io.github.valajar.cryosat.datagen.fusion;

import io.github.valajar.cryosat.registries.CryoBlocks;
import io.github.valajar.cryosat.registries.CryoItems;
import io.github.valajar.cryosat.superconductor.fusion.FusionReactorAccess;
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
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;

/**
 * Recipes for the Field Coil and its controller. Both are fusion era: superconducting coils wrapped in HDPE on a steel
 * casing, and a controller with elite circuits and a Laser Focus Matrix. They only load with Mekanism Generators, since
 * the blocks do nothing without its fusion reactor (and the controller needs a Generators part).
 */
public final class FusionCoilRecipes {
    /** Called from the main recipe provider. */
    public static void build(RecipeOutput output) {
        RecipeOutput withGenerators = output.withConditions(new ModLoadedCondition(FusionReactorAccess.GENERATORS_MODID));
        Item steelCasing = byId(MekanismAPI.MEKANISM_MODID, "steel_casing");
        // Eight blocks for two coils: a three loop ring of 40 blocks per loop takes 30 coils
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CryoBlocks.FIELD_COIL, 8)
              .pattern("IHI").pattern("SCS").pattern("IHI")
              .define('I', common("ingots/steel"))
              .define('H', byId(MekanismAPI.MEKANISM_MODID, "hdpe_sheet"))
              .define('S', CryoItems.SUPERCONDUCTING_COIL)
              .define('C', steelCasing)
              .unlockedBy("has_superconducting_coil", has(CryoItems.SUPERCONDUCTING_COIL))
              .save(withGenerators);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CryoBlocks.FIELD_COIL_CONTROLLER)
              .pattern("ELE").pattern("SFS").pattern("EKE")
              .define('E', common("circuits/elite"))
              .define('L', byId("mekanismgenerators", "laser_focus_matrix"))
              .define('S', CryoItems.SUPERCONDUCTING_COIL)
              .define('F', CryoBlocks.FIELD_COIL)
              .define('K', CryoBlocks.CRYO_CASING)
              .unlockedBy("has_field_coil", has(CryoBlocks.FIELD_COIL))
              .save(withGenerators);
    }

    private static Criterion<InventoryChangeTrigger.TriggerInstance> has(ItemLike item) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(item);
    }

    private static Item byId(String namespace, String path) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(namespace, path));
        if (item == Items.AIR) {
            throw new IllegalStateException(namespace + ":" + path + " not found; is it on the data run classpath?");
        }
        return item;
    }

    private static TagKey<Item> common(String path) {
        return ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", path));
    }

    private FusionCoilRecipes() {
    }
}
