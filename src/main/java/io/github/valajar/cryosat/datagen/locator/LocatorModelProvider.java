package io.github.valajar.cryosat.datagen.locator;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.registries.CryoItems;
import io.github.valajar.cryosat.space.locator.ItemSurveyLocator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Item models of the Survey Locator: one flat model per needle frame ({@code textures/item/survey_locator_NN.png}, drawn by
 * {@code tools/textures/gen/survey_locator.py}), switched by the {@code cryosat:angle} property like vanilla's compass. Frame 0
 * points straight up (target ahead) and the frames turn clockwise.
 */
public class LocatorModelProvider extends ItemModelProvider {
    /** Needle frames, as many as the texture script draws. */
    public static final int FRAMES = 16;

    public LocatorModelProvider(PackOutput output, ExistingFileHelper files) {
        super(output, CryoSat.MODID, files);
    }

    @Override
    public String getName() {
        return "Item Models: cryosat survey locator";
    }

    @Override
    protected void registerModels() {
        String name = CryoItems.SURVEY_LOCATOR.getId().getPath();
        ModelFile base = new ModelFile.UncheckedModelFile(modLoc("item/" + name));
        ItemModelBuilder builder = frame(name, name, 0);
        // Frame i covers the angles within half a frame of i / FRAMES; the last half frame wraps back to frame 0
        for (int i = 1; i < FRAMES; i++) {
            ModelFile model = frame(name + String.format("_%02d", i), name, i);
            builder.override().predicate(ItemSurveyLocator.NEEDLE_ANGLE, (i - 0.5F) / FRAMES).model(model).end();
        }
        builder.override().predicate(ItemSurveyLocator.NEEDLE_ANGLE, (FRAMES - 0.5F) / FRAMES).model(base).end();
    }

    private ItemModelBuilder frame(String model, String name, int frame) {
        ResourceLocation texture = modLoc("item/" + name + String.format("_%02d", frame));
        return withExistingParent(model, mcLoc("item/generated")).texture("layer0", texture);
    }
}
