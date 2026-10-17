package io.github.valajar.cryosat.gametest;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.registries.CryoChemicals;
import mekanism.api.MekanismAPI;
import mekanism.api.MekanismAPITags;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Smoke tests for registration. Run headless with {@code ./gradlew runGameTestServer}.
 */
@GameTestHolder(CryoSat.MODID)
@PrefixGameTestTemplate(false)
public class RegistrationTests {
    @GameTest(template = "empty")
    public static void liquidNitrogenIsMekanismChemical(GameTestHelper helper) {
        helper.assertTrue(MekanismAPI.CHEMICAL_REGISTRY.containsKey(CryoSat.rl("liquid_nitrogen")),
              "cryosat:liquid_nitrogen missing from Mekanism's chemical registry");
        helper.assertFalse(CryoChemicals.LIQUID_NITROGEN.is(MekanismAPI.EMPTY_CHEMICAL_KEY), "liquid nitrogen resolved to the empty chemical");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void compressedAirIsGaseous(GameTestHelper helper) {
        helper.assertTrue(CryoChemicals.COMPRESSED_AIR.is(MekanismAPITags.Chemicals.GASEOUS), "compressed air is not tagged mekanism:gaseous");
        helper.assertFalse(CryoChemicals.LIQUID_NITROGEN.is(MekanismAPITags.Chemicals.GASEOUS), "liquid nitrogen should render as a liquid");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void compressorRecipeLoads(GameTestHelper helper) {
        // Fails if any Mekanism item or tag the recipe refers to does not exist at runtime
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(CryoSat.rl("atmospheric_compressor")).isPresent(),
              "atmospheric compressor recipe missing");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void liquefactionRecipesLoad(GameTestHelper helper) {
        var recipes = io.github.valajar.cryosat.recipe.LiquefactionRecipe.all(helper.getLevel());
        helper.assertTrue(recipes.size() >= 3, "expected at least 3 liquefaction recipes, found " + recipes.size());
        var air = io.github.valajar.cryosat.recipe.LiquefactionRecipe.find(helper.getLevel(), CryoChemicals.COMPRESSED_AIR);
        helper.assertTrue(air != null && air.outputs().size() == 3 && air.maxTemperature() == 77, "compressed air recipe wrong: " + air);
        helper.succeed();
    }
}
