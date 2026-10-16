package io.github.valajar.cryosat.multiblock.liquefaction;

import io.github.valajar.cryosat.registries.CryoChemicals;
import java.util.List;
import java.util.function.Supplier;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * What the Liquefaction Plant can liquefy. An input chemical splits into up to {@link LiquefactionMultiblockData#OUTPUT_TANKS}
 * outputs by fixed fractions, and only once the plant is at or below {@link #maxTemperature}.
 * <p>
 * Hardcoded for now; this becomes a datapack recipe type with JEI support later.
 *
 * @param input          chemical consumed
 * @param outputs        produced chemicals, in output tank order
 * @param fractions      share of each consumed mB that becomes each output (sums to at most 1)
 * @param maxTemperature plant temperature in Kelvin at or below which the recipe runs
 * @param latentHeat     heat released into the plant per mB liquefied
 */
public record LiquefactionRecipe(Supplier<Holder<Chemical>> input, List<Supplier<Holder<Chemical>>> outputs, double[] fractions,
                                 double maxTemperature, double latentHeat) {

    public static final List<LiquefactionRecipe> RECIPES = List.of(
          new LiquefactionRecipe(() -> CryoChemicals.COMPRESSED_AIR,
                List.of(() -> CryoChemicals.LIQUID_NITROGEN, () -> CryoChemicals.LIQUID_OXYGEN, () -> CryoChemicals.LIQUID_ARGON),
                new double[]{0.78, 0.21, 0.01}, 77, 2),
          new LiquefactionRecipe(() -> mekanism("oxygen"), List.of(() -> CryoChemicals.LIQUID_OXYGEN), new double[]{1}, 90, 2),
          new LiquefactionRecipe(() -> mekanism("hydrogen"), List.of(() -> CryoChemicals.LIQUID_HYDROGEN), new double[]{1}, 20, 1)
    );

    private static Holder<Chemical> mekanism(String path) {
        return MekanismAPI.CHEMICAL_REGISTRY.getHolder(ResourceLocation.fromNamespaceAndPath(MekanismAPI.MEKANISM_MODID, path)).orElseThrow();
    }

    public boolean matches(Holder<Chemical> chemical) {
        return input.get().is(chemical);
    }

    @Nullable
    public static LiquefactionRecipe find(Holder<Chemical> chemical) {
        for (LiquefactionRecipe recipe : RECIPES) {
            if (recipe.matches(chemical)) {
                return recipe;
            }
        }
        return null;
    }
}
