package io.github.valajar.cryosat.datagen.separator;

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
 * Block state and models of the Superconducting Separator, textures in {@code textures/block/superconducting_separator/}.
 * Unlike most machines the active state changes more than the window: while superconducting, frost grows on the sides and
 * the top, so the active model swaps those faces too. "Left" and "right" are as seen when looking at the front.
 */
public class SeparatorModelProvider extends BlockStateProvider {
    private static final String NAME = "superconducting_separator";

    public SeparatorModelProvider(PackOutput output, ExistingFileHelper files) {
        super(output, CryoSat.MODID, files);
    }

    @Override
    public String getName() {
        return "Block States: cryosat superconducting separator";
    }

    @Override
    protected void registerStatesAndModels() {
        BlockModelBuilder idle = model(NAME, "");
        BlockModelBuilder active = model(NAME + "_active", "_active");
        getVariantBuilder(CryoBlocks.SUPERCONDUCTING_SEPARATOR.get()).forAllStates(state -> ConfiguredModel.builder()
              .modelFile(Attribute.isActive(state) ? active : idle)
              .rotationY(((int) Attribute.getFacing(state).toYRot() + 180) % 360)
              .build());
        simpleBlockItem(CryoBlocks.SUPERCONDUCTING_SEPARATOR.get(), idle);
    }

    /** @param suffix appended to the faces that change with the active state (front, left, right, top) */
    private BlockModelBuilder model(String modelName, String suffix) {
        return models().cube(modelName, tex("bottom"), tex("top" + suffix), tex("front" + suffix), tex("back"), tex("left" + suffix),
              tex("right" + suffix)).texture("particle", tex("front"));
    }

    private ResourceLocation tex(String face) {
        return modLoc("block/" + NAME + "/" + face);
    }
}
