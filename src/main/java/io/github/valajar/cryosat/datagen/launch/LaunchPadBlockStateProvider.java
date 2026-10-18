package io.github.valajar.cryosat.datagen.launch;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.datagen.connected.ConnectedModelBuilder;
import io.github.valajar.cryosat.registries.CryoBlocks;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Block states and models for the Launch Pad. Textures live in {@code textures/block/launch_pad/}. Every part shares the
 * deck plate on top and the hazard striped edge on the sides, so the formed pad reads as one platform.
 */
public class LaunchPadBlockStateProvider extends BlockStateProvider {
    public LaunchPadBlockStateProvider(PackOutput output, ExistingFileHelper files) {
        super(output, CryoSat.MODID, files);
    }

    @Override
    public String getName() {
        return "Block States: cryosat launch pad";
    }

    @Override
    protected void registerStatesAndModels() {
        ResourceLocation side = tex("plating_side");
        ResourceLocation bottom = tex("plating_bottom");
        // Connected textures on the deck: the pad reads as one floor with the rim only around its outline and around the
        // controller and fuel ports. The sides already tile seamlessly and stay plain faces.
        simpleBlockWithItem(CryoBlocks.LAUNCH_PAD_PLATING.get(), ConnectedModelBuilder.cube(models(), "launch_pad_plating", tex("plating_top"),
                    CryoBlocks.LAUNCH_PAD_PLATING.get(), CryoBlocks.LAUNCH_PAD_FUEL_PORT.get(), CryoBlocks.LAUNCH_CONTROLLER.get())
              .texture("particle", side)
              .texture("down", bottom)
              .texture("north", side)
              .texture("south", side)
              .texture("east", side)
              .texture("west", side));
        ResourceLocation portSide = tex("fuel_port_side");
        simpleBlockWithItem(CryoBlocks.LAUNCH_PAD_FUEL_PORT.get(), models().cube("launch_pad_fuel_port", bottom, tex("fuel_port_top"), portSide, portSide,
              portSide, portSide).texture("particle", portSide));
        controller(CryoBlocks.LAUNCH_CONTROLLER.get(), side, bottom);
    }

    private ResourceLocation tex(String name) {
        return modLoc("block/launch_pad/" + name);
    }

    /** Plating with a console on the front, which lights up while a rocket stands on the pad. */
    private void controller(Block block, ResourceLocation side, ResourceLocation bottom) {
        ResourceLocation top = tex("controller_top");
        ModelFile idle = models().cube("launch_controller", bottom, top, tex("controller_front"), side, side, side).texture("particle", side);
        ModelFile active = models().cube("launch_controller_active", bottom, top, tex("controller_front_active"), side, side, side).texture("particle", side);
        BooleanProperty activeProperty = (BooleanProperty) property(block, "active");
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

    private static Property<?> property(Block block, String name) {
        Property<?> property = block.getStateDefinition().getProperty(name);
        if (property == null) {
            throw new IllegalStateException(block + " has no '" + name + "' block state property");
        }
        return property;
    }
}
