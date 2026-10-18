package io.github.valajar.cryosat.datagen.space;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.registries.CryoBlocks;
import io.github.valajar.cryosat.registries.CryoItems;
import io.github.valajar.cryosat.space.RocketPartItem;
import io.github.valajar.cryosat.space.RocketTier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import mekanism.api.MekanismAPI;
import mekanism.api.datagen.recipe.builder.ItemStackToItemStackRecipeBuilder;
import mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess;
import mekanism.common.registration.impl.ItemRegistryObject;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;

/**
 * Recipes for the space items: the Basic solid propellant chain and the rocket parts. Basic parts need only early
 * Mekanism materials (steel, osmium, basic circuits), so the first launch comes before the Liquefaction Plant. Advanced
 * and Elite parts upgrade the previous tier's part with that tier's alloy and circuit, like Mekanism's tier upgrades.
 * Ultimate parts have no recipes until the gas giants. Mekanism items are referenced by id or tag only.
 */
public final class SpaceRecipes {
    /** Propellant mix pressed into grains, in Mekanism's Enrichment Chamber. */
    public static final ResourceLocation GRAIN_ENRICHING = CryoSat.rl("enriching/solid_propellant_grain");
    /** How much Propellant Mix one craft makes. */
    public static final int MIX_PER_CRAFT = 4;
    /** Tiers whose parts can be crafted. Ultimate parts wait for the gas giants. */
    public static final List<RocketTier> CRAFTABLE = List.of(RocketTier.BASIC, RocketTier.ADVANCED, RocketTier.ELITE);

    /** Every crafting recipe id this class writes, so tests can check that each one loads. */
    public static List<ResourceLocation> craftingIds() {
        List<ResourceLocation> ids = new ArrayList<>();
        ids.add(CryoItems.PROPELLANT_MIX.getId());
        ids.add(CryoItems.GUIDANCE_COMPUTER.getId());
        for (RocketTier tier : CRAFTABLE) {
            ids.add(CryoItems.ROCKET_ENGINES.get(tier).getId());
            ids.add(CryoItems.FUEL_TANKS.get(tier).getId());
            ids.add(CryoItems.ASTEROID_MINERS.get(tier).getId());
        }
        return ids;
    }

    /** Called from the main recipe provider. */
    public static void build(RecipeOutput output) {
        propellant(output);
        basicParts(output);
        upgrade(output, CryoItems.ROCKET_ENGINES, RocketTier.ADVANCED, common("alloys/advanced"), common("circuits/advanced"));
        upgrade(output, CryoItems.FUEL_TANKS, RocketTier.ADVANCED, common("alloys/advanced"), common("circuits/advanced"));
        upgrade(output, CryoItems.ASTEROID_MINERS, RocketTier.ADVANCED, common("alloys/advanced"), common("circuits/advanced"));
        upgrade(output, CryoItems.ROCKET_ENGINES, RocketTier.ELITE, common("alloys/elite"), common("circuits/elite"));
        eliteFuelTank(output);
        upgrade(output, CryoItems.ASTEROID_MINERS, RocketTier.ELITE, common("alloys/elite"), common("circuits/elite"));
    }

    private static void propellant(RecipeOutput output) {
        Item mix = CryoItems.PROPELLANT_MIX.get();
        // Sulfur and carbon, bound with slime, like a real composite rocket motor
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, mix, MIX_PER_CRAFT)
              .requires(common("dusts/sulfur"))
              .requires(common("dusts/sulfur"))
              .requires(mekanismTag("enriched/carbon"))
              .requires(Tags.Items.SLIME_BALLS)
              .unlockedBy("has_sulfur", has(common("dusts/sulfur")))
              .save(output);
        ItemStackToItemStackRecipeBuilder.enriching(IngredientCreatorAccess.item().from(mix), new ItemStack(CryoItems.SOLID_PROPELLANT_GRAIN.get()))
              .unlockedBy("has_propellant_mix", has(mix))
              .build(output, GRAIN_ENRICHING);
    }

    private static void basicParts(RecipeOutput output) {
        TagKey<Item> steel = common("ingots/steel");
        TagKey<Item> osmium = common("ingots/osmium");
        TagKey<Item> circuit = common("circuits/basic");
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CryoItems.ROCKET_ENGINES.get(RocketTier.BASIC))
              .pattern(" C ").pattern("SOS").pattern("S S")
              .define('C', circuit)
              .define('S', steel)
              .define('O', osmium)
              .unlockedBy("has_steel", has(steel))
              .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CryoItems.FUEL_TANKS.get(RocketTier.BASIC))
              .pattern("SOS").pattern("S S").pattern("SOS")
              .define('S', steel)
              .define('O', osmium)
              .unlockedBy("has_steel", has(steel))
              .save(output);
        Item casing = mekanism("steel_casing");
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CryoItems.ASTEROID_MINERS.get(RocketTier.BASIC))
              .pattern("SCS").pattern("GMG").pattern("SPS")
              .define('S', steel)
              .define('C', circuit)
              .define('G', Tags.Items.INGOTS_GOLD)
              .define('M', casing)
              .define('P', Items.IRON_PICKAXE)
              .unlockedBy("has_steel_casing", has(casing))
              .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CryoItems.GUIDANCE_COMPUTER)
              .pattern("GGG").pattern("RCR").pattern("SSS")
              .define('G', Tags.Items.GLASS_PANES)
              .define('R', Tags.Items.DUSTS_REDSTONE)
              .define('C', circuit)
              .define('S', steel)
              .unlockedBy("has_basic_circuit", has(circuit))
              .save(output);
    }

    /** A part of {@code tier} from the previous tier's part, wrapped in that tier's alloy and topped with its circuit. */
    private static void upgrade(RecipeOutput output, Map<RocketTier, ItemRegistryObject<RocketPartItem>> parts, RocketTier tier, TagKey<Item> alloy,
          TagKey<Item> circuit) {
        Item previous = parts.get(RocketTier.values()[tier.ordinal() - 1]).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, parts.get(tier).get())
              .pattern("ACA").pattern("APA")
              .define('A', alloy)
              .define('C', circuit)
              .define('P', previous)
              .unlockedBy("has_previous_tier", has(previous))
              .save(output);
    }

    /**
     * The Elite tank holds liquid hydrogen, so it needs the insulation of a Cryo Casing on top of the usual tier upgrade,
     * which ties Elite rockets to the Liquefaction Plant.
     */
    private static void eliteFuelTank(RecipeOutput output) {
        Item previous = CryoItems.FUEL_TANKS.get(RocketTier.ADVANCED).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CryoItems.FUEL_TANKS.get(RocketTier.ELITE).get())
              .pattern("ACA").pattern("APA").pattern(" K ")
              .define('A', common("alloys/elite"))
              .define('C', common("circuits/elite"))
              .define('P', previous)
              .define('K', CryoBlocks.CRYO_CASING.get())
              .unlockedBy("has_previous_tier", has(previous))
              .save(output);
    }

    private static Criterion<InventoryChangeTrigger.TriggerInstance> has(ItemLike item) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(item);
    }

    private static Criterion<InventoryChangeTrigger.TriggerInstance> has(TagKey<Item> tag) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(tag));
    }

    private static Item mekanism(String path) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MekanismAPI.MEKANISM_MODID, path));
        if (item == Items.AIR) {
            throw new IllegalStateException("Mekanism item " + path + " not found");
        }
        return item;
    }

    private static TagKey<Item> common(String path) {
        return ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", path));
    }

    private static TagKey<Item> mekanismTag(String path) {
        return ItemTags.create(ResourceLocation.fromNamespaceAndPath(MekanismAPI.MEKANISM_MODID, path));
    }

    private SpaceRecipes() {
    }
}
