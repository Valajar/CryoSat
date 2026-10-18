package io.github.valajar.cryosat.client.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.connected.ConnectedTextures;
import io.github.valajar.cryosat.connected.ConnectedTextures.Piece;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.RenderTypeGroup;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import org.joml.Vector3f;
import org.slf4j.Logger;

/**
 * Geometry of a {@code cryosat:connected} model: a full cube whose faces use connected textures (see
 * {@link ConnectedTextures}). The model JSON names the blocks it connects to and the five piece textures:
 * <pre>{@code
 * {
 *   "parent": "minecraft:block/block",
 *   "loader": "cryosat:connected",
 *   "connects_to": ["cryosat:cryo_casing", "cryosat:cryo_valve"],
 *   "textures": {
 *     "particle": "...", "corner": "...", "horizontal": "...", "vertical": "...", "inner": "...", "center": "..."
 *   }
 * }
 * }</pre>
 * A texture named after a direction ({@code "down"}, {@code "north"}, ...) makes that face a plain, unconnected face. The
 * block state must not rotate the model: connections are worked out in world directions.
 */
public record ConnectedGeometry(List<ResourceLocation> connectsTo) implements IUnbakedGeometry<ConnectedGeometry> {
    public static final ResourceLocation LOADER_ID = CryoSat.rl("connected");
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final IGeometryLoader<ConnectedGeometry> LOADER = ConnectedGeometry::read;

    private static ConnectedGeometry read(JsonObject json, JsonDeserializationContext context) throws JsonParseException {
        List<ResourceLocation> blocks = new ArrayList<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(json, "connects_to")) {
            blocks.add(ResourceLocation.parse(GsonHelper.convertToString(element, "connects_to")));
        }
        return new ConnectedGeometry(List.copyOf(blocks));
    }

    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState,
          ItemOverrides overrides) {
        Map<Piece, TextureAtlasSprite> pieces = new EnumMap<>(Piece.class);
        for (Piece piece : Piece.values()) {
            pieces.put(piece, spriteGetter.apply(context.getMaterial(piece.textureKey())));
        }
        FaceBakery bakery = new FaceBakery();
        // [face][piece]: one quad covering the whole face; [face][quarter][piece]: one quad per quarter
        ConnectedBakedModel.Quads quads = new ConnectedBakedModel.Quads();
        for (Direction face : Direction.values()) {
            String name = face.getSerializedName();
            if (context.hasMaterial(name)) {
                quads.staticFace(face, bake(bakery, face, 0, 0, 16, 16, spriteGetter.apply(context.getMaterial(name))));
                continue;
            }
            for (Piece piece : Piece.values()) {
                TextureAtlasSprite sprite = pieces.get(piece);
                quads.full(face, piece, bake(bakery, face, 0, 0, 16, 16, sprite));
                for (int quarter = 0; quarter < ConnectedTextures.QUARTERS; quarter++) {
                    float u = (quarter & 1) * 8, v = (quarter & 2) * 4;
                    quads.quarter(face, quarter, piece, bake(bakery, face, u, v, u + 8, v + 8, sprite));
                }
            }
        }
        Set<Block> blocks = new HashSet<>();
        for (ResourceLocation id : connectsTo) {
            BuiltInRegistries.BLOCK.getOptional(id).ifPresentOrElse(blocks::add, () -> LOGGER.warn("Connected model: unknown block {}", id));
        }
        ResourceLocation renderTypeHint = context.getRenderTypeHint();
        RenderTypeGroup renderTypes = renderTypeHint == null ? RenderTypeGroup.EMPTY : context.getRenderType(renderTypeHint);
        return new ConnectedBakedModel(quads, Set.copyOf(blocks), context.useAmbientOcclusion(), context.isGui3d(), context.useBlockLight(),
              spriteGetter.apply(context.getMaterial("particle")), context.getTransforms(), overrides, renderTypes);
    }

    /**
     * Bakes the part of a face that shows the texture square (u0, v0)-(u1, v1). The element's box is found from the UVs by
     * inverting Minecraft's default face UVs, so the quad shows exactly that part of the texture where a full face would.
     */
    private static BakedQuad bake(FaceBakery bakery, Direction face, float u0, float v0, float u1, float v1,
          TextureAtlasSprite sprite) {
        // World position of texture point (u, v) on this face, per BlockElement.uvsByFace
        Vector3f a = point(face, u0, v0), b = point(face, u1, v1);
        Vector3f from = new Vector3f(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z));
        Vector3f to = new Vector3f(Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z));
        BlockElementFace elementFace = new BlockElementFace(face, -1, "", new BlockFaceUV(null, 0));
        // Building the element fills in the default UVs for its box, which are (u0, v0, u1, v1) again
        BlockElement element = new BlockElement(from, to, Map.of(face, elementFace), null, true);
        return bakery.bakeQuad(element.from, element.to, elementFace, sprite, face, BlockModelRotation.X0_Y0, null, true);
    }

    private static Vector3f point(Direction face, float u, float v) {
        return switch (face) {
            case DOWN -> new Vector3f(u, 0, 16 - v);
            case UP -> new Vector3f(u, 16, v);
            case NORTH -> new Vector3f(16 - u, 16 - v, 0);
            case SOUTH -> new Vector3f(u, 16 - v, 16);
            case WEST -> new Vector3f(0, 16 - v, u);
            case EAST -> new Vector3f(16, 16 - v, 16 - u);
        };
    }
}
