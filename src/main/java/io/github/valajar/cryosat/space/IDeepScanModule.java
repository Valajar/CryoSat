package io.github.valajar.cryosat.space;

/**
 * A ground module next to an Imaging Terminal that makes underground survey targets searchable (the SQUID Array).
 */
public interface IDeepScanModule {
    /** @return whether a deep scan could run right now (for showing deep targets as available) */
    boolean canDeepScan();

    /**
     * Uses whatever one deep scan costs.
     *
     * @return whether it ran; if not, the deep target cannot be searched this time
     */
    boolean useDeepScan();
}
