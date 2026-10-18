package io.github.valajar.cryosat.space.launch;

import io.github.valajar.cryosat.CryoSat;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.ByIdMap;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * A Rocket Assembly button press, sent from the client to the server. Mekanism's own GUI button packets use a closed set
 * of actions, so the pad has its own small payload. The server only acts if the player has that controller's screen open.
 */
public record LaunchPadAction(BlockPos pos, Action action) implements CustomPacketPayload {
    public static final Type<LaunchPadAction> TYPE = new Type<>(CryoSat.rl("launch_pad_action"));
    public static final StreamCodec<ByteBuf, LaunchPadAction> STREAM_CODEC = StreamCodec.composite(
          BlockPos.STREAM_CODEC, LaunchPadAction::pos,
          Action.STREAM_CODEC, LaunchPadAction::action,
          LaunchPadAction::new
    );

    @NotNull
    @Override
    public Type<LaunchPadAction> type() {
        return TYPE;
    }

    private static void handle(LaunchPadAction payload, IPayloadContext context) {
        Player player = context.player();
        if (player.containerMenu instanceof MekanismTileContainer<?> container && container.getTileEntity() instanceof TileEntityLaunchController controller
            && controller.getBlockPos().equals(payload.pos()) && container.stillValid(player)) {
            controller.handleAction(payload.action(), player);
        }
    }

    public enum Action {
        ASSEMBLE,
        DISASSEMBLE,
        LAUNCH,
        NEXT_DESTINATION,
        PREVIOUS_DESTINATION;

        public static final IntFunction<Action> BY_ID = ByIdMap.continuous(Action::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        public static final StreamCodec<ByteBuf, Action> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Action::ordinal);
    }

    @EventBusSubscriber(modid = CryoSat.MODID)
    public static final class Registration {
        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            // Handlers run on the main thread by default
            event.registrar("1").playToServer(TYPE, STREAM_CODEC, LaunchPadAction::handle);
        }

        private Registration() {
        }
    }
}
