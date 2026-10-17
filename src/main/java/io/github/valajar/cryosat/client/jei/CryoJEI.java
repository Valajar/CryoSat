package io.github.valajar.cryosat.client.jei;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.recipe.LiquefactionRecipe;
import io.github.valajar.cryosat.registries.CryoBlocks;
import io.github.valajar.cryosat.registries.CryoItems;
import java.util.List;
import mekanism.client.recipe_viewer.jei.CatalystRegistryHelper;
import mekanism.client.recipe_viewer.jei.MekanismJEI;
import mekanism.client.recipe_viewer.jei.RecipeRegistryHelper;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * JEI integration, built on Mekanism's recipe viewer helpers like Mekanism Generators.
 */
@JeiPlugin
public class CryoJEI implements IModPlugin {

    @NotNull
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(CryoSat.MODID, "jei_plugin");
    }

    @Override
    public void registerItemSubtypes(@NotNull ISubtypeRegistration registry) {
        if (MekanismJEI.shouldLoad()) {
            MekanismJEI.registerItemSubtypes(registry, CryoItems.ITEMS.getEntries());
            MekanismJEI.registerItemSubtypes(registry, CryoBlocks.BLOCKS.getSecondaryEntries());
        }
    }

    @Override
    public void registerCategories(@NotNull IRecipeCategoryRegistration registry) {
        if (MekanismJEI.shouldLoad()) {
            registry.addRecipeCategories(new LiquefactionRecipeCategory(registry.getJeiHelpers().getGuiHelper(), CryoRVRecipeTypes.LIQUEFACTION));
        }
    }

    @Override
    public void registerRecipeCatalysts(@NotNull IRecipeCatalystRegistration registry) {
        if (MekanismJEI.shouldLoad()) {
            CatalystRegistryHelper.register(registry, CryoRVRecipeTypes.LIQUEFACTION);
        }
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registry) {
        if (!MekanismJEI.shouldLoad() || Minecraft.getInstance().level == null) {
            return;
        }
        List<LiquefactionRVRecipe> recipes = LiquefactionRecipe.all(Minecraft.getInstance().level).stream()
              .map(holder -> new LiquefactionRVRecipe(holder.id(), holder.value()))
              .toList();
        RecipeRegistryHelper.register(registry, CryoRVRecipeTypes.LIQUEFACTION, recipes);
    }
}
