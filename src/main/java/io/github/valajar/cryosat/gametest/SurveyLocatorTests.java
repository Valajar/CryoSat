package io.github.valajar.cryosat.gametest;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.registries.CryoDataComponents;
import io.github.valajar.cryosat.registries.CryoItems;
import io.github.valajar.cryosat.space.LocatorData;
import io.github.valajar.cryosat.space.locator.ItemSurveyLocator;
import io.github.valajar.cryosat.space.locator.LocatorLang;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Survey Locator: sneak cycling, the stored targets surviving a save and the network, the recipe, and that everything the server
 * does with the item stays clear of client classes.
 */
@GameTestHolder(CryoSat.MODID)
@PrefixGameTestTemplate(false)
public class SurveyLocatorTests {
    private static LocatorData threeTargets(int selected) {
        return new LocatorData(List.of(
              new LocatorData.Entry(GlobalPos.of(Level.OVERWORLD, new BlockPos(120, 0, -340)), 311, ResourceLocation.withDefaultNamespace("village")),
              new LocatorData.Entry(GlobalPos.of(Level.OVERWORLD, new BlockPos(-2000, 0, 75)), 0, CryoSat.rl("ocean_monument")),
              new LocatorData.Entry(GlobalPos.of(Level.NETHER, new BlockPos(16, 64, 16)), 25, CryoSat.rl("fortress"))
        ), selected);
    }

    private static ItemStack locator(LocatorData data) {
        ItemStack stack = new ItemStack(CryoItems.SURVEY_LOCATOR.get());
        stack.set(CryoDataComponents.LOCATOR_DATA, data);
        return stack;
    }

    @GameTest(template = "empty")
    public static void sneakUseCyclesAndWraps(GameTestHelper helper) {
        LocatorData original = threeTargets(0);
        ItemStack stack = locator(original);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        // Plain use does nothing
        helper.assertTrue(!stack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult().consumesAction(), "plain use was consumed");
        helper.assertTrue(ItemSurveyLocator.getData(stack).equals(original), "plain use changed the locator");

        player.setShiftKeyDown(true);
        for (int expected : new int[]{1, 2, 0, 1}) {
            helper.assertTrue(stack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult().consumesAction(), "sneak use was not consumed");
            LocatorData data = ItemSurveyLocator.getData(stack);
            helper.assertTrue(data.selected() == expected, "selected " + data.selected() + ", expected " + expected);
            helper.assertTrue(data.targets().equals(original.targets()), "cycling changed the stored targets: " + data.targets());
        }

        // A selection past the end (written by anything) still cycles to a valid target
        helper.assertTrue(ItemSurveyLocator.cycle(threeTargets(7)).selected() == 2, "out of range selection did not wrap");
        helper.assertTrue(ItemSurveyLocator.cycle(threeTargets(-1)).selected() == 0, "negative selection did not wrap");

        // An empty locator stays empty
        ItemStack empty = new ItemStack(CryoItems.SURVEY_LOCATOR.get());
        player.setItemInHand(InteractionHand.OFF_HAND, empty);
        empty.use(helper.getLevel(), player, InteractionHand.OFF_HAND);
        helper.assertTrue(ItemSurveyLocator.getData(empty).equals(LocatorData.EMPTY), "empty locator changed: " + ItemSurveyLocator.getData(empty));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void targetsSurviveSaving(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        ItemStack fresh = new ItemStack(CryoItems.SURVEY_LOCATOR.get());
        helper.assertTrue(fresh.getMaxStackSize() == 1, "locators stack to " + fresh.getMaxStackSize());
        helper.assertTrue(LocatorData.EMPTY.equals(fresh.get(CryoDataComponents.LOCATOR_DATA)), "new locator holds " + fresh.get(CryoDataComponents.LOCATOR_DATA));

        ItemStack stack = locator(threeTargets(2));
        // Disk
        Tag saved = stack.save(registries);
        Optional<ItemStack> loaded = ItemStack.parse(registries, saved);
        helper.assertTrue(loaded.isPresent() && ItemStack.isSameItemSameComponents(stack, loaded.get()), "saved locator loaded as " + loaded);
        helper.assertTrue(threeTargets(2).equals(ItemSurveyLocator.getData(loaded.get())), "targets after loading: " + ItemSurveyLocator.getData(loaded.get()));
        // Network
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        ItemStack.STREAM_CODEC.encode(buffer, stack);
        ItemStack received = ItemStack.STREAM_CODEC.decode(buffer);
        buffer.release();
        helper.assertTrue(threeTargets(2).equals(ItemSurveyLocator.getData(received)), "targets after syncing: " + ItemSurveyLocator.getData(received));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void locatorRecipeLoads(GameTestHelper helper) {
        Optional<RecipeHolder<?>> recipe = helper.getLevel().getRecipeManager().byKey(CryoItems.SURVEY_LOCATOR.getId());
        helper.assertTrue(recipe.isPresent(), "survey locator recipe missing; does a Mekanism tag it uses exist?");
        helper.assertTrue(recipe.get().value().getResultItem(helper.getLevel().registryAccess()).is(CryoItems.SURVEY_LOCATOR.get()), "recipe makes something else");
        helper.succeed();
    }

    /**
     * The needle and the beam live in {@code client.locator}, behind a client only event subscriber. On this dedicated server,
     * where FML's dist cleaner refuses to load client classes, everything the server does with a locator runs: the item's
     * declared members resolve, use and the tooltip run, and the lang entries resolve. A client class reached from any of these
     * would fail here.
     */
    @GameTest(template = "empty")
    public static void serverSideNeedsNoClientClasses(GameTestHelper helper) {
        helper.assertTrue(FMLEnvironment.dist.isDedicatedServer(), "game tests are expected to run on a dedicated server, not " + FMLEnvironment.dist);
        for (Class<?> type : List.of(ItemSurveyLocator.class, LocatorLang.class, LocatorData.class)) {
            type.getDeclaredMethods();
            type.getDeclaredFields();
            type.getDeclaredConstructors();
        }
        ItemStack stack = locator(threeTargets(0));
        List<Component> tooltip = new ArrayList<>();
        stack.getItem().appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()), tooltip, TooltipFlag.NORMAL);
        // One line per target plus the usage line
        helper.assertTrue(tooltip.size() == 4, "tooltip has " + tooltip.size() + " lines: " + tooltip);
        tooltip.forEach(Component::getString);
        List<Component> emptyTooltip = new ArrayList<>();
        stack.getItem().appendHoverText(new ItemStack(CryoItems.SURVEY_LOCATOR.get()), Item.TooltipContext.of(helper.getLevel()), emptyTooltip, TooltipFlag.NORMAL);
        helper.assertTrue(emptyTooltip.size() == 1, "empty tooltip has " + emptyTooltip.size() + " lines");
        helper.succeed();
    }
}
