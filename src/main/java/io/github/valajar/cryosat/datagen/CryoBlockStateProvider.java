package io.github.valajar.cryosat.datagen;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.machine.MachineBlock;
import io.github.valajar.cryosat.registries.CryoBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class CryoBlockStateProvider extends BlockStateProvider {
    public CryoBlockStateProvider(PackOutput output, ExistingFileHelper files) {
        super(output, CryoSat.MODID, files);
    }

    @Override
    protected void registerStatesAndModels() {
        machine(CryoBlocks.ATMOSPHERIC_COMPRESSOR.get(), "atmospheric_compressor");
    }

    /**
     * A 1x1 machine with per face textures in {@code textures/block/<name>/}: front, front_active, back, left, right,
     * top, bottom. "Left" and "right" are as seen when looking at the front.
     */
    private void machine(MachineBlock block, String name) {
        BlockModelBuilder idle = machineModel(name, "front");
        BlockModelBuilder active = machineModel(name + "_active", "front_active", name);
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
              .modelFile(state.getValue(MachineBlock.ACTIVE) ? active : idle)
              .rotationY(((int) state.getValue(MachineBlock.FACING).toYRot() + 180) % 360)
              .build());
        simpleBlockItem(block, idle);
    }

    private BlockModelBuilder machineModel(String name, String front) {
        return machineModel(name, front, name);
    }

    private BlockModelBuilder machineModel(String modelName, String front, String textureDir) {
        return models().cube(modelName, tex(textureDir, "bottom"), tex(textureDir, "top"), tex(textureDir, front), tex(textureDir, "back"),
              tex(textureDir, "left"), tex(textureDir, "right")).texture("particle", tex(textureDir, "front"));
    }

    private ResourceLocation tex(String dir, String face) {
        return modLoc("block/" + dir + "/" + face);
    }
}
