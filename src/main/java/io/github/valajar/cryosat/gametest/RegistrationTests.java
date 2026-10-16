package io.github.valajar.cryosat.gametest;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.registries.CryoChemicals;
import mekanism.api.MekanismAPI;
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
        helper.assertFalse(CryoChemicals.LIQUID_NITROGEN.get().isEmptyType(), "liquid nitrogen resolved to the empty chemical");
        helper.succeed();
    }
}
