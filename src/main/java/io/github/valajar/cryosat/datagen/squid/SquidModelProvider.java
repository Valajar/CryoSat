package io.github.valajar.cryosat.datagen.squid;

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
 * Block state and models of the SQUID Array, textures in {@code textures/block/squid_array/}. Like the Superconducting
 * Separator, the active (superconducting) model also frosts the sides and the top, not only the front.
 */
public class SquidModelProvider extends BlockStateProvider {
    private static final String NAME = "squid_array";

    public SquidModelProvider(PackOutput output, ExistingFileHelper files) {
        super(output, CryoSat.MODID, files);
    }

    @Override
    public String getName() {
        return "Block States: cryosat squid array";
    }

    @Override
    protected void registerStatesAndModels() {
        BlockModelBuilder idle = model(NAME, "");
        BlockModelBuilder active = model(NAME + "_active", "_active");
        getVariantBuilder(CryoBlocks.SQUID_ARRAY.get()).forAllStates(state -> ConfiguredModel.builder()
              .modelFile(Attribute.isActive(state) ? active : idle)
              .rotationY(((int) Attribute.getFacing(state).toYRot() + 180) % 360)
              .build());
        simpleBlockItem(CryoBlocks.SQUID_ARRAY.get(), idle);
    }

    /** @param suffix appended to the faces that change with the active state (front, sides, top) */
    private BlockModelBuilder model(String modelName, String suffix) {
        return models().cube(modelName, tex("bottom"), tex("top" + suffix), tex("front" + suffix), tex("back"), tex("side" + suffix),
              tex("side" + suffix)).texture("particle", tex("front"));
    }

    private ResourceLocation tex(String face) {
        return modLoc("block/" + NAME + "/" + face);
    }
}
