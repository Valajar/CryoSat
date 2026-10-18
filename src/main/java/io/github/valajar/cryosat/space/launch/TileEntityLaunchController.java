package io.github.valajar.cryosat.space.launch;

import io.github.valajar.cryosat.client.render.LaunchPadClient;
import io.github.valajar.cryosat.registries.CryoBlocks;
import io.github.valajar.cryosat.space.MissionProblem;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.UsernameCache;
import org.jetbrains.annotations.Nullable;

/**
 * Brain of the Launch Pad and home of the Rocket Assembly screen. Always the multiblock master, so it syncs the rocket to
 * clients and renders it. Launches on a rising redstone edge too.
 */
public class TileEntityLaunchController extends TileEntityLaunchPadPlating {
    /** Redstone state last tick; {@code null} until the first tick so loading a powered pad never launches it. */
    @Nullable
    private Boolean wasPowered;

    public TileEntityLaunchController(BlockPos pos, BlockState state) {
        super(CryoBlocks.LAUNCH_CONTROLLER, pos, state);
        delaySupplier = NO_DELAY;
    }

    @Override
    public boolean canBeMaster() {
        return true;
    }

    @Override
    protected boolean onUpdateServer(LaunchPadMultiblockData multiblock) {
        boolean needsPacket = super.onUpdateServer(multiblock);
        boolean powered = level.hasNeighborSignal(worldPosition);
        if (powered && Boolean.FALSE.equals(wasPowered) && multiblock.isFormed()) {
            launch(null);
        }
        wasPowered = powered;
        setActive(multiblock.isFormed() && multiblock.getDesign() != null);
        return needsPacket;
    }

    @Override
    protected void onUpdateClient() {
        super.onUpdateClient();
        LaunchPadClient.tick(this);
    }

    /** The pad's Mekanism security owner, or {@code null} if nobody owns it. */
    @Nullable
    public UUID getOwner() {
        return getSecurity() == null ? null : getSecurity().getOwnerUUID();
    }

    /**
     * Launches the assembled rocket for the pad's owner, or for the player who pressed the button if nobody owns the pad.
     *
     * @return {@link LaunchStatus#LAUNCHING} once launched, otherwise why not
     */
    public LaunchStatus launch(@Nullable Player player) {
        LaunchPadMultiblockData pad = getMultiblock();
        if (!pad.isFormed() || !(level instanceof ServerLevel serverLevel)) {
            return pad.status;
        }
        UUID owner = getOwner();
        if (owner == null && player != null) {
            owner = player.getUUID();
        }
        UUID launcher = owner;
        return pad.launch(serverLevel, owner, () -> player != null ? player.getDisplayName() : nameOf(serverLevel.getServer(), launcher));
    }

    /** The owner's name for the launch announcement, without assuming the profile cache exists (it does not in game tests). */
    private static Component nameOf(MinecraftServer server, UUID owner) {
        ServerPlayer online = server.getPlayerList().getPlayer(owner);
        if (online != null) {
            return online.getDisplayName();
        }
        String name = UsernameCache.getLastKnownUsername(owner);
        if (name == null && server.getProfileCache() != null) {
            name = server.getProfileCache().get(owner).map(GameProfile::getName).orElse(null);
        }
        return Component.literal(name == null ? owner.toString() : name);
    }

    /** Runs a Rocket Assembly button pressed by a player who has this controller's screen open. */
    public void handleAction(LaunchPadAction.Action action, Player player) {
        LaunchPadMultiblockData pad = getMultiblock();
        if (!pad.isFormed()) {
            return;
        }
        switch (action) {
            case ASSEMBLE -> {
                MissionProblem problem = pad.assemble();
                if (problem != null) {
                    player.displayClientMessage(problem.getTextComponent(), true);
                }
            }
            case DISASSEMBLE -> pad.disassemble(player);
            case LAUNCH -> {
                LaunchStatus result = launch(player);
                if (result != LaunchStatus.LAUNCHING) {
                    MissionProblem problem = pad.getProblem();
                    player.displayClientMessage(problem != null ? problem.getTextComponent() : result.getTextComponent(), true);
                }
            }
            case NEXT_DESTINATION -> pad.cycleDestination(1);
            case PREVIOUS_DESTINATION -> pad.cycleDestination(-1);
        }
    }
}
