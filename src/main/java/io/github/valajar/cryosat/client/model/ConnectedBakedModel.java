package io.github.valajar.cryosat.client.model;

import io.github.valajar.cryosat.connected.ConnectedTextures;
import io.github.valajar.cryosat.connected.ConnectedTextures.Connections;
import io.github.valajar.cryosat.connected.ConnectedTextures.Piece;
import java.util.List;
import java.util.Set;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.RenderTypeGroup;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A cube with connected textures. Connections are worked out from the level when the chunk section is built
 * ({@link #getModelData}); every quad is pre baked, so drawing only picks quads. A face whose four quarters share a piece is
 * one full quad, so an isolated block or a held item draws exactly like a plain cube. All quads carry their face as cull
 * face, so culling and ambient occlusion work as for any cube.
 */
public class ConnectedBakedModel implements IDynamicBakedModel {
    public static final ModelProperty<Connections> CONNECTIONS = new ModelProperty<>();

    private final Quads quads;
    private final Set<Block> parts;
    private final boolean ambientOcclusion;
    private final boolean gui3d;
    private final boolean blockLight;
    private final TextureAtlasSprite particle;
    private final ItemTransforms transforms;
    private final ItemOverrides overrides;
    @Nullable
    private final ChunkRenderTypeSet blockRenderTypes;
    @Nullable
    private final List<RenderType> itemRenderTypes;
    @Nullable
    private final List<RenderType> fabulousItemRenderTypes;

    ConnectedBakedModel(Quads quads, Set<Block> parts, boolean ambientOcclusion, boolean gui3d, boolean blockLight, TextureAtlasSprite particle,
          ItemTransforms transforms, ItemOverrides overrides, RenderTypeGroup renderTypes) {
        this.quads = quads;
        this.parts = parts;
        this.ambientOcclusion = ambientOcclusion;
        this.gui3d = gui3d;
        this.blockLight = blockLight;
        this.particle = particle;
        this.transforms = transforms;
        this.overrides = overrides;
        this.blockRenderTypes = renderTypes.isEmpty() ? null : ChunkRenderTypeSet.of(renderTypes.block());
        this.itemRenderTypes = renderTypes.isEmpty() ? null : List.of(renderTypes.entity());
        this.fabulousItemRenderTypes = renderTypes.isEmpty() ? null : List.of(renderTypes.entityFabulous());
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData) {
        Connections connections = ConnectedTextures.compute(level, pos, s -> parts.contains(s.getBlock()));
        return modelData.derive().with(CONNECTIONS, connections).build();
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData data,
          @Nullable RenderType renderType) {
        if (side == null) {
            return List.of();
        }
        int f = side.get3DDataValue();
        if (quads.staticFaces[f] != null) {
            return quads.staticFaces[f];
        }
        Connections connections = data.get(CONNECTIONS);
        if (connections == null) {
            connections = Connections.ISOLATED;
        }
        Piece uniform = connections.uniform(side);
        if (uniform != null) {
            return quads.full[f][uniform.ordinal()];
        }
        BakedQuad[] out = new BakedQuad[ConnectedTextures.QUARTERS];
        for (int quarter = 0; quarter < out.length; quarter++) {
            out[quarter] = quads.quarters[f][quarter][connections.piece(side, quarter).ordinal()];
        }
        return List.of(out);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return ambientOcclusion;
    }

    @Override
    public boolean isGui3d() {
        return gui3d;
    }

    @Override
    public boolean usesBlockLight() {
        return blockLight;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleIcon() {
        return particle;
    }

    @Override
    public @NotNull ItemTransforms getTransforms() {
        return transforms;
    }

    @Override
    public @NotNull ItemOverrides getOverrides() {
        return overrides;
    }

    @Override
    public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        return blockRenderTypes != null ? blockRenderTypes : IDynamicBakedModel.super.getRenderTypes(state, rand, data);
    }

    @Override
    public @NotNull List<RenderType> getRenderTypes(@NotNull ItemStack stack, boolean fabulous) {
        List<RenderType> types = fabulous ? fabulousItemRenderTypes : itemRenderTypes;
        return types != null ? types : IDynamicBakedModel.super.getRenderTypes(stack, fabulous);
    }

    /** Every quad the model can draw, baked once. */
    static final class Quads {
        @SuppressWarnings("unchecked")
        final List<BakedQuad>[] staticFaces = new List[6];
        @SuppressWarnings("unchecked")
        final List<BakedQuad>[][] full = new List[6][Piece.values().length];
        final BakedQuad[][][] quarters = new BakedQuad[6][ConnectedTextures.QUARTERS][Piece.values().length];

        void staticFace(Direction face, BakedQuad quad) {
            staticFaces[face.get3DDataValue()] = List.of(quad);
        }

        void full(Direction face, Piece piece, BakedQuad quad) {
            full[face.get3DDataValue()][piece.ordinal()] = List.of(quad);
        }

        void quarter(Direction face, int quarter, Piece piece, BakedQuad quad) {
            quarters[face.get3DDataValue()][quarter][piece.ordinal()] = quad;
        }
    }
}
