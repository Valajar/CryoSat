package io.github.valajar.cryosat.client.jei;

import io.github.valajar.cryosat.CryoLang;
import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.registries.CryoBlocks;
import mekanism.client.recipe_viewer.type.FakeRVRecipeType;

/**
 * Recipe viewer types. The crop rectangle matches the Liquefaction Plant GUI layout, so the page looks like the machine.
 */
public final class CryoRVRecipeTypes {
    public static final FakeRVRecipeType<LiquefactionRVRecipe> LIQUEFACTION = new FakeRVRecipeType<>(CryoSat.rl("liquefaction"),
          CryoBlocks.LIQUEFACTION_CONTROLLER, CryoLang.LIQUEFACTION_PLANT, LiquefactionRVRecipe.class, -6, -13, 168, 60,
          CryoBlocks.CRYO_CASING, CryoBlocks.CRYO_VALVE, CryoBlocks.CRYOCOOLER, CryoBlocks.HEAT_REJECTION_PORT);

    private CryoRVRecipeTypes() {
    }
}
