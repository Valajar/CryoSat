package io.github.valajar.cryosat.gametest;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.recipe.LiquefactionRecipe;
import io.github.valajar.cryosat.registries.CryoChemicals;
import io.github.valajar.cryosat.space.AsteroidClass;
import io.github.valajar.cryosat.space.CargoEntry;
import io.github.valajar.cryosat.space.SpaceData;
import io.github.valajar.cryosat.superconductor.Cryostat;
import io.github.valajar.cryosat.superconductor.CryostatState;
import java.util.List;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The shared superconductor pieces: the cryostat's coolant rules, nitrogen boil off and the Separator extras data.
 */
@GameTestHolder(CryoSat.MODID)
@PrefixGameTestTemplate(false)
public class SuperconductorTests {

    @GameTest(template = "empty")
    public static void cryostatQuenchesAndRecovers(GameTestHelper helper) {
        Cryostat cryostat = new Cryostat(1, () -> {});
        cryostat.tick();
        helper.assertTrue(cryostat.getState() == CryostatState.NO_COOLANT, "worked without coolant: " + cryostat.getState());
        cryostat.coolantTank.setStack(new ChemicalStack(CryoChemicals.LIQUID_NITROGEN, 1_000));
        cryostat.tick();
        helper.assertTrue(cryostat.getState() == CryostatState.COOLING_DOWN, "no cool down after refilling: " + cryostat.getState());
        for (int i = 0; i < Cryostat.COOLDOWN_TICKS; i++) {
            cryostat.tick();
        }
        helper.assertTrue(cryostat.isSuperconducting(), "not superconducting after the cool down: " + cryostat.getState());
        helper.assertTrue(cryostat.use(100), "could not use 100 mB for work");
        helper.assertFalse(cryostat.use(10_000), "used more coolant than the tank holds");
        helper.assertTrue(cryostat.boilOffTank.getStack().is(CryoChemicals.NITROGEN) && cryostat.boilOffTank.getStored() > 0,
              "no nitrogen boil off: " + cryostat.boilOffTank.getStack());
        cryostat.coolantTank.setEmpty();
        cryostat.tick();
        helper.assertTrue(cryostat.getState() == CryostatState.NO_COOLANT && cryostat.getCooldown() == Cryostat.COOLDOWN_TICKS,
              "running dry did not quench: " + cryostat.getState());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nitrogenLiquefiesBack(GameTestHelper helper) {
        LiquefactionRecipe recipe = LiquefactionRecipe.find(helper.getLevel(), CryoChemicals.NITROGEN);
        helper.assertTrue(recipe != null && recipe.outputs().getFirst().is(CryoChemicals.LIQUID_NITROGEN), "no nitrogen to liquid nitrogen recipe");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chondriteExtrasResolve(GameTestHelper helper) {
        AsteroidClass chondrite = SpaceData.ASTEROIDS.get(CryoSat.rl("chondrite"));
        helper.assertTrue(chondrite != null && chondrite.bonusYields().size() >= 2, "chondrite has no bonus yields");
        for (AsteroidClass.BonusYield bonus : chondrite.bonusYields()) {
            helper.assertTrue(bonus.yield().resolve().isPresent(), "bonus " + bonus.yield().item() + " did not resolve");
        }
        boolean diamonds = false;
        RandomSource random = RandomSource.create(7);
        for (int i = 0; i < 50 && !diamonds; i++) {
            List<CargoEntry> extras = chondrite.rollBonus(random);
            diamonds = extras.stream().anyMatch(entry -> entry.item().value() == Items.DIAMOND);
        }
        helper.assertTrue(diamonds, "no diamonds in 50 separated chondrite batches");
        helper.succeed();
    }
}
