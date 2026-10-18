package io.github.valajar.cryosat.space.mission;

import io.github.valajar.cryosat.CryoSat;
import java.util.UUID;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * Client to server: a Mission Control button was pressed. Acts on the Mission Control screen the player has open, so a
 * player can only act through a block they could open (Mekanism security), and only on their own assets.
 */
public record MissionActionPayload(MissionAction action, UUID asset, String name) implements CustomPacketPayload {
    public static final Type<MissionActionPayload> TYPE = new Type<>(CryoSat.rl("mission_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MissionActionPayload> STREAM_CODEC = StreamCodec.composite(
          MissionAction.STREAM_CODEC, MissionActionPayload::action,
          UUIDUtil.STREAM_CODEC, MissionActionPayload::asset,
          ByteBufCodecs.stringUtf8(4 * MissionControl.MAX_NAME_LENGTH), MissionActionPayload::name,
          MissionActionPayload::new
    );

    @NotNull
    @Override
    public Type<MissionActionPayload> type() {
        return TYPE;
    }

    /** Runs on the server thread. */
    public static void handle(MissionActionPayload payload, IPayloadContext context) {
        Player player = context.player();
        if (player.containerMenu instanceof MekanismTileContainer<?> menu && menu.getTileEntity() instanceof TileEntityMissionControl tile
            && menu.stillValid(player)) {
            MissionResult result = tile.perform(player.getUUID(), payload.action(), payload.asset(), payload.name());
            player.displayClientMessage(result.message().translate(), true);
        }
    }
}
