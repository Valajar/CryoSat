package io.github.valajar.cryosat.space.survey;

import io.github.valajar.cryosat.CryoSat;
import java.util.UUID;
import mekanism.api.security.IBlockSecurityUtils;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * Client to server: an Imaging Terminal button was pressed. Acts on the terminal screen the player has open, and only if the
 * player may use the terminal under Mekanism security.
 *
 * @param target the survey target, for {@link ImagingAction#REQUEST}
 * @param entry  the log entry, for {@link ImagingAction#DISMISS} and {@link ImagingAction#LOAD}
 */
public record ImagingActionPayload(ImagingAction action, ResourceLocation target, UUID entry) implements CustomPacketPayload {
    public static final Type<ImagingActionPayload> TYPE = new Type<>(CryoSat.rl("imaging_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ImagingActionPayload> STREAM_CODEC = StreamCodec.composite(
          ImagingAction.STREAM_CODEC, ImagingActionPayload::action,
          ResourceLocation.STREAM_CODEC, ImagingActionPayload::target,
          UUIDUtil.STREAM_CODEC, ImagingActionPayload::entry,
          ImagingActionPayload::new
    );
    private static final UUID NONE = new UUID(0, 0);

    public static ImagingActionPayload request(ResourceLocation target) {
        return new ImagingActionPayload(ImagingAction.REQUEST, target, NONE);
    }

    public static ImagingActionPayload onEntry(ImagingAction action, UUID entry) {
        return new ImagingActionPayload(action, CryoSat.rl("none"), entry);
    }

    @NotNull
    @Override
    public Type<ImagingActionPayload> type() {
        return TYPE;
    }

    /** Runs on the server thread. */
    public static void handle(ImagingActionPayload payload, IPayloadContext context) {
        Player player = context.player();
        if (!(player.containerMenu instanceof MekanismTileContainer<?> menu) || !(menu.getTileEntity() instanceof TileEntityImagingTerminal tile)
            || !menu.stillValid(player)) {
            return;
        }
        if (!IBlockSecurityUtils.INSTANCE.canAccess(player, tile.getLevel(), tile.getBlockPos(), tile)) {
            player.displayClientMessage(RequestResult.NO_ACCESS.message().translate(), true);
            return;
        }
        switch (payload.action()) {
            case REQUEST -> player.displayClientMessage(tile.request(player.getUUID(), payload.target()).message().translate(), true);
            case DISMISS -> tile.dismiss(payload.entry());
            case LOAD -> player.displayClientMessage(tile.loadOntoLocator(payload.entry()).message().translate(), true);
        }
    }
}
