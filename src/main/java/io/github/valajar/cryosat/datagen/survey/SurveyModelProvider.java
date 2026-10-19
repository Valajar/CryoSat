package io.github.valajar.cryosat.datagen.survey;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.registries.CryoBlocks;
import mekanism.common.block.attribute.Attribute;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Block state and models of the Imaging Terminal, textures in {@code textures/block/imaging_terminal/}. Only the front
 * changes when active (the screen lights up). "Left" and "right" are as seen when looking at the front.
 */
public class SurveyModelProvider extends BlockStateProvider {
    private static final String NAME = "imaging_terminal";

    public SurveyModelProvider(PackOutput output, ExistingFileHelper files) {
        super(output, CryoSat.MODID, files);
    }

    @Override
    public String getName() {
        return "Block States: cryosat imaging terminal";
    }

    @Override
    protected void registerStatesAndModels() {
        BlockModelBuilder idle = model(NAME, "front");
        BlockModelBuilder active = model(NAME + "_active", "front_active");
        getVariantBuilder(CryoBlocks.IMAGING_TERMINAL.get()).forAllStates(state -> ConfiguredModel.builder()
              .modelFile(Attribute.isActive(state) ? active : idle)
              .rotationY(((int) Attribute.getFacing(state).toYRot() + 180) % 360)
              .build());
        simpleBlockItem(CryoBlocks.IMAGING_TERMINAL.get(), idle);
    }

    private BlockModelBuilder model(String modelName, String front) {
        return models().cube(modelName, tex("bottom"), tex("top"), tex(front), tex("back"), tex("left"), tex("right")).texture("particle", tex("front"));
    }

    private ResourceLocation tex(String face) {
        return modLoc("block/" + NAME + "/" + face);
    }
}
