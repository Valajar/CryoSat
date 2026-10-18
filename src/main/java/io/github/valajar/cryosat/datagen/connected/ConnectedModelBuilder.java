package io.github.valajar.cryosat.datagen.connected;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.connected.ConnectedTextures.Piece;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockModelProvider;
import net.neoforged.neoforge.client.model.generators.CustomLoaderBuilder;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Datagen for {@code cryosat:connected} models (see {@code client.model.ConnectedGeometry}): adds the loader and its
 * {@code connects_to} list.
 */
public class ConnectedModelBuilder<T extends ModelBuilder<T>> extends CustomLoaderBuilder<T> {
    private final List<ResourceLocation> connectsTo = new ArrayList<>();

    protected ConnectedModelBuilder(T parent, ExistingFileHelper existingFileHelper) {
        super(CryoSat.rl("connected"), parent, existingFileHelper, false);
    }

    public static <T extends ModelBuilder<T>> ConnectedModelBuilder<T> begin(T parent, ExistingFileHelper existingFileHelper) {
        return new ConnectedModelBuilder<>(parent, existingFileHelper);
    }

    public ConnectedModelBuilder<T> connectsTo(Block... blocks) {
        for (Block block : blocks) {
            connectsTo.add(BuiltInRegistries.BLOCK.getKey(block));
        }
        return this;
    }

    @Override
    public JsonObject toJson(JsonObject json) {
        json = super.toJson(json);
        JsonArray blocks = new JsonArray();
        connectsTo.forEach(id -> blocks.add(id.toString()));
        json.add("connects_to", blocks);
        return json;
    }

    /**
     * A connected cube named {@code name}, with the piece textures {@code <base>}, {@code <base>_horizontal},
     * {@code <base>_vertical}, {@code <base>_inner} and {@code <base>_center}; the plain texture doubles as particle.
     */
    public static BlockModelBuilder cube(BlockModelProvider models, String name, ResourceLocation base, Block... connectsTo) {
        BlockModelBuilder model = models.withExistingParent(name, ResourceLocation.withDefaultNamespace("block/block"))
              .customLoader(ConnectedModelBuilder::begin)
              .connectsTo(connectsTo)
              .end()
              .texture("particle", base);
        for (Piece piece : Piece.values()) {
            model.texture(piece.textureKey(), piece == Piece.CORNER ? base : base.withSuffix("_" + piece.textureKey()));
        }
        return model;
    }
}
