package io.github.valajar.cryosat.datagen;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.datagen.launch.LaunchPadBlockStateProvider;
import io.github.valajar.cryosat.datagen.plant.PlantBlockStateProvider;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = CryoSat.MODID)
public final class CryoDataGen {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper files = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        generator.addProvider(event.includeClient(), new CryoBlockStateProvider(output, files));
        generator.addProvider(event.includeClient(), new CryoLanguageProvider(output));
        generator.addProvider(event.includeServer(), new LootTableProvider(output, Set.of(),
              List.of(new LootTableProvider.SubProviderEntry(CryoBlockLoot::new, LootContextParamSets.BLOCK)), lookup));
        generator.addProvider(event.includeServer(), new CryoBlockTagsProvider(output, lookup, files));
        generator.addProvider(event.includeServer(), new CryoChemicalTagsProvider(output, lookup, files));
        generator.addProvider(event.includeServer(), new CryoRecipeProvider(output, lookup));
        // Liquefaction Plant
        generator.addProvider(event.includeClient(), new PlantBlockStateProvider(output, files));
        // Space items: item models (recipes are added by CryoRecipeProvider through SpaceRecipes)
        generator.addProvider(event.includeClient(), new CryoItemModelProvider(output, files));
        // Cargo Receiver and Uplink Card (space workstream A)
        generator.addProvider(event.includeClient(), new io.github.valajar.cryosat.datagen.receiver.ReceiverModelProvider(output, files));
        // Launch Pad (workstream B)
        generator.addProvider(event.includeClient(), new LaunchPadBlockStateProvider(output, files));
        // Superconducting Separator (superconductor workstream A)
        generator.addProvider(event.includeClient(), new io.github.valajar.cryosat.datagen.separator.SeparatorModelProvider(output, files));
        // Fusion Field Coils (superconductor workstream C)
        generator.addProvider(event.includeClient(), new io.github.valajar.cryosat.datagen.fusion.FusionCoilModelProvider(output, files));
        // Survey Locator (satellite workstream B): needle frame models
        generator.addProvider(event.includeClient(), new io.github.valajar.cryosat.datagen.locator.LocatorModelProvider(output, files));
    }

    private CryoDataGen() {
    }
}
