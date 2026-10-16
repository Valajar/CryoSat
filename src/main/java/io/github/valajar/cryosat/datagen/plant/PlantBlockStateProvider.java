package io.github.valajar.cryosat.datagen.plant;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.registries.plant.PlantBlocks;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Block states and models for the Liquefaction Plant. Textures live in {@code textures/block/liquefaction/}.
 */
public class PlantBlockStateProvider extends BlockStateProvider {
    public PlantBlockStateProvider(PackOutput output, ExistingFileHelper files) {
        super(output, CryoSat.MODID, files);
    }

    @Override
    public String getName() {
        return "Block States: cryosat liquefaction plant";
    }

    @Override
    protected void registerStatesAndModels() {
        ModelFile casing = models().cubeAll("cryo_casing", tex("casing"));
        simpleBlockWithItem(PlantBlocks.CRYO_CASING.get(), casing);
        simpleBlockWithItem(PlantBlocks.CRYO_VALVE.get(), models().cubeAll("cryo_valve", tex("valve")));
        activeCube(PlantBlocks.CRYOCOOLER.get(), "cryocooler");
        activeCube(PlantBlocks.HEAT_REJECTION_PORT.get(), "heat_rejection_port");
        controller(PlantBlocks.LIQUEFACTION_CONTROLLER.get());
    }

    private ResourceLocation tex(String name) {
        return modLoc("block/liquefaction/" + name);
    }

    /** A cube whose texture switches to {@code <name>_active} while the block is active. */
    private void activeCube(Block block, String name) {
        ModelFile idle = models().cubeAll(name, tex(name));
        ModelFile active = models().cubeAll(name + "_active", tex(name + "_active"));
        BooleanProperty activeProperty = booleanProperty(block, "active");
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder().modelFile(state.getValue(activeProperty) ? active : idle).build());
        simpleBlockItem(block, idle);
    }

    /** Casing on every side except a screen on the front, which lights up while the plant runs. */
    private void controller(Block block) {
        ResourceLocation casing = tex("casing");
        ModelFile idle = models().cube("liquefaction_controller", casing, casing, tex("controller_front"), casing, casing, casing)
              .texture("particle", casing);
        ModelFile active = models().cube("liquefaction_controller_active", casing, casing, tex("controller_front_active"), casing, casing, casing)
              .texture("particle", casing);
        BooleanProperty activeProperty = booleanProperty(block, "active");
        DirectionProperty facing = (DirectionProperty) property(block, "facing");
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
              .modelFile(state.getValue(activeProperty) ? active : idle)
              .rotationY(yRotation(state.getValue(facing)))
              .build());
        simpleBlockItem(block, idle);
    }

    private static int yRotation(Direction facing) {
        return ((int) facing.toYRot() + 180) % 360;
    }

    private static BooleanProperty booleanProperty(Block block, String name) {
        return (BooleanProperty) property(block, name);
    }

    private static Property<?> property(Block block, String name) {
        Property<?> property = block.getStateDefinition().getProperty(name);
        if (property == null) {
            throw new IllegalStateException(block + " has no '" + name + "' block state property");
        }
        return property;
    }

    @SuppressWarnings("unused")
    private static boolean isActive(BlockState state) {
        return state.getValues().entrySet().stream().anyMatch(e -> e.getKey().getName().equals("active") && Boolean.TRUE.equals(e.getValue()));
    }
}
