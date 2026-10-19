package io.github.valajar.cryosat.space;

import java.util.List;
import java.util.UUID;
import mekanism.common.lib.frequency.FrequencyType;
import mekanism.common.lib.security.SecurityFrequency;
import mekanism.api.security.SecurityMode;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

/**
 * The Survey Satellites a player can use over one dimension: their own plus those of players who trust them in Mekanism's
 * Security Desk. Computed on demand and never saved, so changing satellites (or these formulas) takes effect at once.
 * <p>
 * Each satellite adds coverage value by tier (1, 2, 4, 8). Coverage approaches but never reaches 100 percent and decides how
 * precise results are, how far searches reach and, through the number of satellites, how many requests run at once.
 */
public record Constellation(List<OrbitalAsset> satellites) {
    /** Value for 63 percent coverage. */
    public static final double COVERAGE_SCALE = 8;
    /** From this coverage on results are exact. */
    public static final double EXACT_COVERAGE = 0.95;
    public static final int MAX_UNCERTAINTY = 512;
    public static final int BASE_RANGE = 1_000;
    public static final int EXTRA_RANGE = 15_000;

    public static Constellation of(MinecraftServer server, UUID viewer, ResourceKey<Level> dimension) {
        return new Constellation(OrbitalRegistry.get(server).all().stream()
              .filter(asset -> asset.state == OrbitalAsset.State.ORBITING)
              .filter(asset -> asset.satellite() != null && asset.satellite().orbitDimension.equals(dimension))
              .filter(asset -> canUse(viewer, asset.owner))
              .toList());
    }

    /** Whether {@code viewer} may use satellites owned by {@code owner}: their own, or the owner trusts them. */
    public static boolean canUse(UUID viewer, UUID owner) {
        if (viewer.equals(owner)) {
            return true;
        }
        SecurityFrequency frequency = FrequencyType.SECURITY.getManager(null, SecurityMode.PUBLIC).getFrequency(owner);
        return frequency != null && frequency.isTrusted(viewer);
    }

    public int value() {
        return satellites.stream().mapToInt(asset -> SatelliteState.coverageValue(asset.design.payloadTier())).sum();
    }

    /** Coverage from 0 to just below 1. */
    public double coverage() {
        return coverageFor(value());
    }

    public static double coverageFor(int value) {
        return 1 - Math.exp(-value / COVERAGE_SCALE);
    }

    /** Radius in blocks a reported location may be off by; 0 once coverage reaches {@link #EXACT_COVERAGE}. */
    public int uncertainty() {
        return uncertaintyFor(coverage());
    }

    public static int uncertaintyFor(double coverage) {
        return coverage >= EXACT_COVERAGE ? 0 : (int) Math.round(MAX_UNCERTAINTY * (1 - coverage));
    }

    /** How far around the terminal a search reaches, in blocks. */
    public int range() {
        return satellites.isEmpty() ? 0 : (int) Math.round(BASE_RANGE + EXTRA_RANGE * coverage());
    }

    /** Satellites with no pass in progress. */
    public long free() {
        return satellites.stream().filter(asset -> asset.satellite().isFree()).count();
    }
}
