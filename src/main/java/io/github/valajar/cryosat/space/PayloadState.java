package io.github.valajar.cryosat.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.server.MinecraftServer;

/**
 * What an orbital asset does once launched, one implementation per {@link PayloadType}. The asset holds the parts every
 * payload shares (owner, name, design, timers); the state holds the rest and advances it each registry tick.
 */
public sealed interface PayloadState permits MinerState, SatelliteState {
    Codec<PayloadState> CODEC = PayloadType.CODEC.dispatch("type", PayloadState::type, PayloadState::codecFor);

    private static MapCodec<? extends PayloadState> codecFor(PayloadType type) {
        return switch (type) {
            case ASTEROID_MINER -> MinerState.CODEC;
            case SURVEY_SATELLITE -> SatelliteState.CODEC;
        };
    }

    PayloadType type();

    /**
     * Advances the asset to {@code now} (overworld game time): arrival, work, delivery.
     *
     * @return whether anything changed that needs saving
     */
    boolean tick(MinecraftServer server, OrbitalAsset asset, long now);
}
