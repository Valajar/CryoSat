package io.github.valajar.cryosat.gametest;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.datagen.space.SpaceRecipes;
import io.github.valajar.cryosat.registries.CryoItems;
import io.github.valajar.cryosat.space.RocketTier;
import java.util.List;
import java.util.Optional;
import mekanism.api.MekanismAPI;
import mekanism.api.recipes.ItemStackToItemStackRecipe;
import mekanism.common.recipe.MekanismRecipeType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Space item recipes: every recipe loads with Mekanism's real items and tags, and the Basic propellant chain resolves from
 * raw materials to the grain a Basic rocket burns.
 */
@GameTestHolder(CryoSat.MODID)
@PrefixGameTestTemplate(false)
public class SpaceItemTests {
    private static Item mekanism(String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MekanismAPI.MEKANISM_MODID, path));
    }

    @GameTest(template = "empty")
    public static void spaceRecipesLoad(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        for (ResourceLocation id : SpaceRecipes.craftingIds()) {
            Optional<RecipeHolder<?>> holder = recipes.byKey(id);
            helper.assertTrue(holder.isPresent(), "recipe " + id + " missing; does a Mekanism item or tag it uses exist?");
            Item result = holder.get().value().getResultItem(helper.getLevel().registryAccess()).getItem();
            helper.assertTrue(BuiltInRegistries.ITEM.getKey(result).equals(id), "recipe " + id + " makes " + result);
        }
        helper.assertTrue(recipes.byKey(SpaceRecipes.GRAIN_ENRICHING).isPresent(), "grain enriching recipe missing");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ultimatePartsHaveNoRecipe(GameTestHelper helper) {
        List<Item> ultimate = List.of(CryoItems.ROCKET_ENGINES.get(RocketTier.ULTIMATE).get(), CryoItems.FUEL_TANKS.get(RocketTier.ULTIMATE).get(),
              CryoItems.ASTEROID_MINERS.get(RocketTier.ULTIMATE).get());
        for (RecipeHolder<CraftingRecipe> holder : helper.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            Item result = holder.value().getResultItem(helper.getLevel().registryAccess()).getItem();
            helper.assertFalse(ultimate.contains(result), "ultimate part is craftable through " + holder.id());
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void propellantChainResolves(GameTestHelper helper) {
        // Crafting: two sulfur dust, enriched carbon and a slime ball, matched as a player would put them in the grid
        CraftingInput grid = CraftingInput.of(2, 2, List.of(new ItemStack(mekanism("dust_sulfur")), new ItemStack(mekanism("enriched_carbon")),
              new ItemStack(Items.SLIME_BALL), new ItemStack(mekanism("dust_sulfur"))));
        Optional<RecipeHolder<CraftingRecipe>> mixRecipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel());
        helper.assertTrue(mixRecipe.isPresent() && mixRecipe.get().id().equals(CryoItems.PROPELLANT_MIX.getId()), "mix ingredients craft " + mixRecipe);
        ItemStack mix = mixRecipe.get().value().assemble(grid, helper.getLevel().registryAccess());
        helper.assertTrue(mix.is(CryoItems.PROPELLANT_MIX.get()) && mix.getCount() == SpaceRecipes.MIX_PER_CRAFT, "crafted " + mix);

        // Enrichment Chamber: the same lookup the chamber does for its input slot
        ItemStackToItemStackRecipe enriching = MekanismRecipeType.ENRICHING.getInputCache().findFirstRecipe(helper.getLevel(), mix);
        helper.assertTrue(enriching != null, "no enriching recipe for propellant mix");
        ItemStack grain = enriching.getOutput(mix);
        helper.assertTrue(grain.is(CryoItems.SOLID_PROPELLANT_GRAIN.get()), "propellant mix enriches into " + grain);

        // And the grain is what a Basic rocket burns
        ResourceLocation fuel = RocketTier.BASIC.fuel().orElseThrow().solidItem().orElseThrow();
        helper.assertTrue(fuel.equals(CryoItems.SOLID_PROPELLANT_GRAIN.getId()), "basic rockets burn " + fuel);
        helper.succeed();
    }
}
