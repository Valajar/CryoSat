package io.github.valajar.cryosat.space;

/**
 * A block entity that receives completed imaging passes (the Imaging Terminal). The satellite offers each completed task
 * while the terminal's chunk is loaded and keeps it until the terminal accepts it.
 */
public interface ISurveyTerminal {
    /**
     * @param task      the completed pass
     * @param satellite the satellite that did it
     *
     * @return whether the terminal took the result; if not, it is offered again later
     */
    boolean receiveSurvey(OrbitalTask task, OrbitalAsset satellite);
}
