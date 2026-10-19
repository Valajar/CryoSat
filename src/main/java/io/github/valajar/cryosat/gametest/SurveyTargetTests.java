package io.github.valajar.cryosat.gametest;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.datagen.squid.SurveyTargetLangData;
import io.github.valajar.cryosat.space.SpaceData;
import io.github.valajar.cryosat.space.SurveyTarget;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The vanilla survey targets CryoSat ships as datapack files: every file loads, names real structures in this world, has a
 * real icon item and an English name, and exactly the four underground targets need a deep scan.
 */
@GameTestHolder(CryoSat.MODID)
@PrefixGameTestTemplate(false)
public class SurveyTargetTests {
    private static final Set<String> DEEP = Set.of("ancient_city", "stronghold", "trial_chambers", "mineshaft");

    private static Map<String, SurveyTarget> shipped() {
        return SpaceData.SURVEY_TARGETS.all().entrySet().stream()
              .filter(entry -> entry.getKey().getNamespace().equals(CryoSat.MODID))
              .collect(Collectors.toMap(entry -> entry.getKey().getPath(), Map.Entry::getValue));
    }

    @GameTest(template = "empty")
    public static void everyTargetLoadsAndResolves(GameTestHelper helper) {
        Map<String, SurveyTarget> targets = shipped();
        // One file per named target: a file that failed to parse is missing here
        helper.assertTrue(targets.keySet().equals(SurveyTargetLangData.NAMES.keySet()),
              "survey target files " + targets.keySet() + " do not match the named targets " + SurveyTargetLangData.NAMES.keySet());
        targets.forEach((id, target) -> {
            HolderSet<Structure> structures = target.resolve(helper.getLevel().registryAccess()).orElse(null);
            helper.assertTrue(structures != null && structures.size() > 0, id + ": " + target.structures() + " does not resolve to any structure");
            ResourceLocation icon = target.icon();
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(icon), id + ": icon " + icon + " is not an item");
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void exactlyUndergroundTargetsNeedDeepScan(GameTestHelper helper) {
        Set<String> deep = shipped().entrySet().stream().filter(entry -> entry.getValue().deepScan()).map(Map.Entry::getKey).collect(Collectors.toSet());
        helper.assertTrue(deep.equals(DEEP), "deep scan targets " + deep + ", expected " + DEEP);
        helper.succeed();
    }
}
