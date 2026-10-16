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
}
