package io.github.valajar.cryosat.datagen;

import io.github.valajar.cryosat.datagen.plant.PlantRecipes;
import io.github.valajar.cryosat.registries.CryoBlocks;
import java.util.concurrent.CompletableFuture;
import mekanism.api.MekanismAPI;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Recipes reference Mekanism items by id and common tags, because only Mekanism's API is on the compile classpath. Data
 * generation runs with the full Mekanism mod loaded, so the ids resolve.
 */
public class CryoRecipeProvider extends RecipeProvider {
    public CryoRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        Item steelCasing = mekanism("steel_casing");
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CryoBlocks.ATMOSPHERIC_COMPRESSOR.get())
              .pattern("SCS")
              .pattern("OMO")
              .pattern("SAS")
              .define('S', common("ingots/steel"))
              .define('C', common("circuits/basic"))
              .define('O', common("ingots/osmium"))
              .define('M', steelCasing)
              .define('A', common("alloys/advanced"))
              .unlockedBy("has_steel_casing", has(steelCasing))
              .save(output);

        PlantRecipes.build(output);
        // Cargo Receiver and Uplink Card (space workstream A)
        io.github.valajar.cryosat.datagen.receiver.ReceiverRecipes.build(output);
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
}
