package io.github.valajar.cryosat.gametest;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.registries.CryoItems;
import io.github.valajar.cryosat.space.RocketTier;
import java.util.List;
import java.util.Optional;
import mekanism.api.MekanismAPI;
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
 * Survey Satellite recipes, crafted from the items a player would put in the grid: Basic from early materials, Advanced and
 * Elite as tier upgrades of the previous satellite, Ultimate not craftable yet. That every recipe loads is checked with the
 * other space recipes in {@link SpaceItemTests}.
 */
@GameTestHolder(CryoSat.MODID)
@PrefixGameTestTemplate(false)
public class SurveySatelliteRecipeTests {
    private static ItemStack mekanism(String path) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MekanismAPI.MEKANISM_MODID, path));
        return new ItemStack(item);
    }

    private static ItemStack satellite(RocketTier tier) {
        return new ItemStack(CryoItems.SURVEY_SATELLITES.get(tier).get());
    }

    private static ItemStack craft(GameTestHelper helper, CraftingInput grid) {
        Optional<RecipeHolder<CraftingRecipe>> recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel());
        return recipe.map(holder -> holder.value().assemble(grid, helper.getLevel().registryAccess())).orElse(ItemStack.EMPTY);
    }

    @GameTest(template = "empty")
    public static void basicSatelliteCrafts(GameTestHelper helper) {
        ItemStack steel = mekanism("ingot_steel");
        CraftingInput grid = CraftingInput.of(3, 3, List.of(
              steel, new ItemStack(Items.REDSTONE), steel,
              new ItemStack(Items.DAYLIGHT_DETECTOR), mekanism("basic_control_circuit"), new ItemStack(Items.DAYLIGHT_DETECTOR),
              steel, new ItemStack(Items.GLASS), steel));
        ItemStack result = craft(helper, grid);
        helper.assertTrue(ItemStack.isSameItem(result, satellite(RocketTier.BASIC)), "basic satellite ingredients craft " + result);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void satelliteTierUpgrades(GameTestHelper helper) {
        List<List<String>> materials = List.of(List.of("alloy_infused", "advanced_control_circuit"), List.of("alloy_reinforced", "elite_control_circuit"));
        for (RocketTier tier : List.of(RocketTier.ADVANCED, RocketTier.ELITE)) {
            List<String> tierMaterials = materials.get(tier.ordinal() - 1);
            ItemStack alloy = mekanism(tierMaterials.get(0));
            CraftingInput grid = CraftingInput.of(3, 2, List.of(
                  alloy, mekanism(tierMaterials.get(1)), alloy,
                  alloy, satellite(RocketTier.values()[tier.ordinal() - 1]), alloy));
            ItemStack result = craft(helper, grid);
            helper.assertTrue(ItemStack.isSameItem(result, satellite(tier)), tier.getSerializedName() + " upgrade crafts " + result);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ultimateSatelliteHasNoRecipe(GameTestHelper helper) {
        Item ultimate = CryoItems.SURVEY_SATELLITES.get(RocketTier.ULTIMATE).get();
        for (RecipeHolder<CraftingRecipe> holder : helper.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            Item result = holder.value().getResultItem(helper.getLevel().registryAccess()).getItem();
            helper.assertFalse(result == ultimate, "ultimate survey satellite is craftable through " + holder.id());
        }
        helper.succeed();
    }
}
