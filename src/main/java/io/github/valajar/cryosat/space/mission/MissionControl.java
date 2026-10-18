package io.github.valajar.cryosat.space.mission;

import io.github.valajar.cryosat.registries.CryoDataComponents;
import io.github.valajar.cryosat.space.OrbitalAsset;
import io.github.valajar.cryosat.space.OrbitalRegistry;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Server side of Mission Control: what a player sees and the actions they can take. Every action checks that the asset
 * belongs to the acting player first, whoever owns the Mission Control block.
 */
public final class MissionControl {
    public static final int MAX_NAME_LENGTH = 32;

    /** @return the player's assets, in launch order */
    public static List<MissionSummary> summaries(MinecraftServer server, UUID player) {
        return OrbitalRegistry.get(server).ownedBy(player).stream().map(MissionSummary::of).toList();
    }

    /** @return whether the player has an asset that is still mining or has cargo to deliver; lights up their Mission Control */
    public static boolean hasWorkInOrbit(MinecraftServer server, @Nullable UUID player) {
        return player != null && OrbitalRegistry.get(server).ownedBy(player).stream().anyMatch(asset -> !asset.isDone());
    }

    /** @return whether a stack can go into the Mission Control card slot: anything carrying an uplink target, normally the Uplink Card */
    public static boolean isUplinkCard(ItemStack stack) {
        return stack.has(CryoDataComponents.UPLINK_TARGET);
    }

    /**
     * Runs one action for a player.
     *
     * @param name the new name, for {@link MissionAction#RENAME}
     * @param card the stack in the card slot, for {@link MissionAction#RELINK}
     */
    public static MissionResult perform(MinecraftServer server, UUID player, MissionAction action, UUID assetId, String name, ItemStack card) {
        OrbitalRegistry registry = OrbitalRegistry.get(server);
        OrbitalAsset asset = owned(registry, player, assetId);
        if (asset == null) {
            return MissionResult.NOT_YOURS;
        }
        return switch (action) {
            case RENAME -> {
                registry.rename(asset, sanitizeName(name));
                yield MissionResult.RENAMED;
            }
            case RELINK -> {
                GlobalPos target = card.get(CryoDataComponents.UPLINK_TARGET);
                if (target == null) {
                    yield MissionResult.NO_CARD;
                }
                registry.relink(asset, target);
                yield MissionResult.RELINKED;
            }
            case UNLINK -> {
                registry.relink(asset, null);
                yield MissionResult.UNLINKED;
            }
            case DECOMMISSION -> {
                registry.decommission(asset);
                yield MissionResult.DECOMMISSIONED;
            }
        };
    }

    @Nullable
    private static OrbitalAsset owned(OrbitalRegistry registry, UUID player, UUID assetId) {
        OrbitalAsset asset = registry.get(assetId);
        return asset != null && asset.owner.equals(player) ? asset : null;
    }

    /** Drops formatting codes and control characters and caps the length, like an anvil rename. */
    public static String sanitizeName(String name) {
        String filtered = StringUtil.filterText(name).strip();
        return filtered.length() > MAX_NAME_LENGTH ? filtered.substring(0, MAX_NAME_LENGTH).strip() : filtered;
    }

    private MissionControl() {
    }
}
