package io.github.valajar.cryosat.datagen.fusion;

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
 * Block states and models of the Field Coil and its controller. Both frost over while the ring is cold (the active state).
 * Coil textures live in {@code textures/block/field_coil/} (side, top, plus {@code _active}); the controller has its own front
 * and top in {@code textures/block/field_coil_controller/} and shows coil windings on its other sides, since it is part of a
 * loop.
 */
public class FusionCoilModelProvider extends BlockStateProvider {
    private static final String COIL = "field_coil";
    private static final String CONTROLLER = "field_coil_controller";

    public FusionCoilModelProvider(PackOutput output, ExistingFileHelper files) {
        super(output, CryoSat.MODID, files);
    }

    @Override
    public String getName() {
        return "Block States: cryosat field coils";
    }

    @Override
    protected void registerStatesAndModels() {
        coil();
        controller();
    }

    private void coil() {
        BlockModelBuilder idle = coilModel(COIL, "");
        BlockModelBuilder active = coilModel(COIL + "_active", "_active");
        getVariantBuilder(CryoBlocks.FIELD_COIL.get()).forAllStates(state -> ConfiguredModel.builder()
              .modelFile(Attribute.isActive(state) ? active : idle)
              .build());
        simpleBlockItem(CryoBlocks.FIELD_COIL.get(), idle);
    }

    private BlockModelBuilder coilModel(String name, String suffix) {
        return models().cubeBottomTop(name, tex(COIL, "side" + suffix), tex(COIL, "top" + suffix), tex(COIL, "top" + suffix))
              .texture("particle", tex(COIL, "side"));
    }

    private void controller() {
        BlockModelBuilder idle = controllerModel(CONTROLLER, "");
        BlockModelBuilder active = controllerModel(CONTROLLER + "_active", "_active");
        // Same orientation as CryoSat's other machines: the model's north face is the front
        getVariantBuilder(CryoBlocks.FIELD_COIL_CONTROLLER.get()).forAllStates(state -> ConfiguredModel.builder()
              .modelFile(Attribute.isActive(state) ? active : idle)
              .rotationY(((int) Attribute.getFacing(state).toYRot() + 180) % 360)
              .build());
        simpleBlockItem(CryoBlocks.FIELD_COIL_CONTROLLER.get(), idle);
    }

    private BlockModelBuilder controllerModel(String name, String suffix) {
        ResourceLocation side = tex(COIL, "side" + suffix);
        ResourceLocation top = tex(CONTROLLER, "top" + suffix);
        return models().cube(name, top, top, tex(CONTROLLER, "front" + suffix), side, side, side)
              .texture("particle", tex(CONTROLLER, "front"));
    }

    private ResourceLocation tex(String dir, String face) {
        return modLoc("block/" + dir + "/" + face);
    }
}
