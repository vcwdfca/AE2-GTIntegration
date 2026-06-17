package com.formlessDragon.ae2gtaddon.integration.hei;

import ae2.container.me.items.ContainerPatternEncodingTerm;

/**
 * Exposes the owning pattern encoding terminal container from AE2's encoding mode panels.
 */
public interface PatternEncodingPanelContainerAccess {

    /**
     * Returns the container that owns the encoding panel.
     *
     * @return the pattern encoding terminal container backing the panel.
     */
    ContainerPatternEncodingTerm ae2gtaddon$getContainer();
}
