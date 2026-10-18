package io.github.valajar.cryosat.datagen.receiver;

import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.registries.CryoBlocks;
import io.github.valajar.cryosat.registries.CryoItems;
import mekanism.common.block.attribute.Attribute;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Block state and models of the Cargo Receiver (textures in {@code textures/block/cargo_receiver/}), plus the Uplink
 * Card's item model, whose {@code cryosat:linked} override switches to the linked texture while it holds a target.
 */
public class ReceiverModelProvider extends BlockStateProvider {
    public ReceiverModelProvider(PackOutput output, ExistingFileHelper files) {
        super(output, CryoSat.MODID, files);
    }

    @Override
    public String getName() {
        return "Block States: cryosat cargo receiver";
    }

    @Override
    protected void registerStatesAndModels() {
        receiver();
        uplinkCard();
    }

    /** A 1x1 machine without an active state; "left" and "right" are as seen when looking at the front. */
    private void receiver() {
        String name = CryoBlocks.CARGO_RECEIVER.getId().getPath();
        BlockModelBuilder model = models().cube(name, tex(name, "bottom"), tex(name, "top"), tex(name, "front"), tex(name, "back"), tex(name, "left"),
              tex(name, "right")).texture("particle", tex(name, "front"));
        getVariantBuilder(CryoBlocks.CARGO_RECEIVER.get()).forAllStates(state -> ConfiguredModel.builder()
              .modelFile(model)
              .rotationY(((int) Attribute.getFacing(state).toYRot() + 180) % 360)
              .build());
        simpleBlockItem(CryoBlocks.CARGO_RECEIVER.get(), model);
    }

    private void uplinkCard() {
        String name = CryoItems.UPLINK_CARD.getId().getPath();
        ItemModelBuilder linked = itemModels().withExistingParent(name + "_linked", mcLoc("item/generated"))
              .texture("layer0", modLoc("item/" + name + "_linked"));
        itemModels().withExistingParent(name, mcLoc("item/generated"))
              .texture("layer0", modLoc("item/" + name))
              .override().predicate(CryoSat.rl("linked"), 1).model(linked).end();
    }

    private ResourceLocation tex(String dir, String face) {
        return modLoc("block/" + dir + "/" + face);
    }
}
