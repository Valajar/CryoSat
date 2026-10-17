package io.github.valajar.cryosat.client.jei;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.valajar.cryosat.recipe.LiquefactionRecipe;
import mekanism.client.recipe_viewer.emi.INamedRVRecipe;
import net.minecraft.resources.ResourceLocation;

/**
 * A liquefaction recipe as the recipe viewer sees it: the recipe plus its id.
 */
public record LiquefactionRVRecipe(ResourceLocation id, LiquefactionRecipe recipe) implements INamedRVRecipe {
    public static final Codec<LiquefactionRVRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          ResourceLocation.CODEC.fieldOf("id").forGetter(LiquefactionRVRecipe::id),
          LiquefactionRecipe.CODEC.fieldOf("recipe").forGetter(LiquefactionRVRecipe::recipe)
    ).apply(instance, LiquefactionRVRecipe::new));
}
