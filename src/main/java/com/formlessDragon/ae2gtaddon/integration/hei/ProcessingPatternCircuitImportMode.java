package com.formlessDragon.ae2gtaddon.integration.hei;

/**
 * Stores the client-side HEI import preference for a pattern encoding terminal container.
 *
 * <p>HEI writes processing pattern slots on the client before sending fake slot updates to the server, so this state
 * only needs to live on the client container that owns the visible processing pattern panel.</p>
 */
public interface ProcessingPatternCircuitImportMode {

    /**
     * Returns whether HEI processing pattern imports should skip GregTech integrated circuits.
     *
     * @return {@code true} when integrated circuits should not be written into processing input slots.
     */
    boolean ae2gtaddon$shouldIgnoreCircuitInHeiProcessingPattern();

    /**
     * Updates whether HEI processing pattern imports should skip GregTech integrated circuits.
     *
     * @param ignoreCircuit {@code true} to skip integrated circuits, {@code false} to keep the original HEI import
     *                      behavior.
     */
    void ae2gtaddon$setIgnoreCircuitInHeiProcessingPattern(boolean ignoreCircuit);
}
