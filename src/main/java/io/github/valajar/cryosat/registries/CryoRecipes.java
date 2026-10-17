package io.github.valajar.cryosat.registries;

import com.mojang.serialization.MapCodec;
import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.recipe.LiquefactionRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Recipe types and serializers.
 */
public final class CryoRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, CryoSat.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, CryoSat.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<LiquefactionRecipe>> LIQUEFACTION = TYPES.register("liquefaction",
          () -> RecipeType.simple(CryoSat.rl("liquefaction")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<LiquefactionRecipe>> LIQUEFACTION_SERIALIZER = SERIALIZERS.register("liquefaction",
          () -> new RecipeSerializer<>() {
              @Override
              public MapCodec<LiquefactionRecipe> codec() {
                  return LiquefactionRecipe.CODEC;
              }

              @Override
              public StreamCodec<RegistryFriendlyByteBuf, LiquefactionRecipe> streamCodec() {
                  return LiquefactionRecipe.STREAM_CODEC;
              }
          });

    private CryoRecipes() {
    }
}
