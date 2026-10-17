package io.github.valajar.cryosat.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.valajar.cryosat.registries.CryoRecipes;
import java.util.List;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * A Liquefaction Plant recipe ({@code cryosat:liquefaction}, datapack driven). Consuming {@code input.amount} mB of the input
 * chemical produces each output's amount, in output tank order, once the plant is at or below {@link #maxTemperature()} Kelvin.
 * Liquefying releases {@link #latentHeat()} heat per mB consumed back into the plant.
 * <p>
 * Example:
 * <pre>{@code
 * {
 *   "type": "cryosat:liquefaction",
 *   "input": {"chemical": "cryosat:compressed_air", "amount": 100},
 *   "outputs": [{"id": "cryosat:liquid_nitrogen", "amount": 78}, {"id": "cryosat:liquid_oxygen", "amount": 21}],
 *   "max_temperature": 77,
 *   "latent_heat": 2
 * }
 * }</pre>
 */
public class LiquefactionRecipe extends MekanismRecipe<SingleRecipeInput> {
    public static final int MAX_OUTPUTS = 3;

    public static final MapCodec<LiquefactionRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
          ChemicalStackIngredient.CODEC.fieldOf("input").forGetter(LiquefactionRecipe::input),
          ChemicalStack.CODEC.listOf(1, MAX_OUTPUTS).fieldOf("outputs").forGetter(LiquefactionRecipe::outputs),
          Codec.doubleRange(0, 100_000).fieldOf("max_temperature").forGetter(LiquefactionRecipe::maxTemperature),
          Codec.doubleRange(0, 100_000).optionalFieldOf("latent_heat", 1.0).forGetter(LiquefactionRecipe::latentHeat)
    ).apply(instance, LiquefactionRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, LiquefactionRecipe> STREAM_CODEC = StreamCodec.composite(
          ChemicalStackIngredient.STREAM_CODEC, LiquefactionRecipe::input,
          ChemicalStack.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_OUTPUTS)), LiquefactionRecipe::outputs,
          ByteBufCodecs.DOUBLE, LiquefactionRecipe::maxTemperature,
          ByteBufCodecs.DOUBLE, LiquefactionRecipe::latentHeat,
          LiquefactionRecipe::new
    );

    private final ChemicalStackIngredient input;
    private final List<ChemicalStack> outputs;
    private final double maxTemperature;
    private final double latentHeat;

    public LiquefactionRecipe(ChemicalStackIngredient input, List<ChemicalStack> outputs, double maxTemperature, double latentHeat) {
        this.input = input;
        this.outputs = List.copyOf(outputs);
        this.maxTemperature = maxTemperature;
        this.latentHeat = latentHeat;
    }

    public ChemicalStackIngredient input() {
        return input;
    }

    public List<ChemicalStack> outputs() {
        return outputs;
    }

    public double maxTemperature() {
        return maxTemperature;
    }

    public double latentHeat() {
        return latentHeat;
    }

    /** Share of each consumed mB that becomes output {@code index}. */
    public double fraction(int index) {
        return outputs.get(index).getAmount() / (double) input.amount();
    }

    public boolean matches(Holder<Chemical> chemical) {
        return input.testType(chemical);
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        // Not an item recipe; the plant looks recipes up by chemical via find()
        return false;
    }

    @Override
    public boolean isIncomplete() {
        return input.hasNoMatchingInstances();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CryoRecipes.LIQUEFACTION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return CryoRecipes.LIQUEFACTION.get();
    }

    public static List<RecipeHolder<LiquefactionRecipe>> all(Level level) {
        return level.getRecipeManager().getAllRecipesFor(CryoRecipes.LIQUEFACTION.get());
    }

    @Nullable
    public static LiquefactionRecipe find(Level level, Holder<Chemical> chemical) {
        for (RecipeHolder<LiquefactionRecipe> holder : all(level)) {
            if (holder.value().matches(chemical)) {
                return holder.value();
            }
        }
        return null;
    }
}
