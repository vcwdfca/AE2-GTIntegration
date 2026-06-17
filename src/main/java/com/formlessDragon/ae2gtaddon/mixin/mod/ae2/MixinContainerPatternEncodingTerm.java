package com.formlessDragon.ae2gtaddon.mixin.mod.ae2;

import ae2.container.me.items.ContainerPatternEncodingTerm;
import com.formlessDragon.ae2gtaddon.integration.hei.ProcessingPatternCircuitImportMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = ContainerPatternEncodingTerm.class, remap = false)
public class MixinContainerPatternEncodingTerm implements ProcessingPatternCircuitImportMode {

    @Unique
    private boolean ae2gtaddon$ignoreCircuitInHeiProcessingPattern;

    @Override
    public boolean ae2gtaddon$shouldIgnoreCircuitInHeiProcessingPattern() {
        return this.ae2gtaddon$ignoreCircuitInHeiProcessingPattern;
    }

    @Override
    public void ae2gtaddon$setIgnoreCircuitInHeiProcessingPattern(boolean ignoreCircuit) {
        this.ae2gtaddon$ignoreCircuitInHeiProcessingPattern = ignoreCircuit;
    }
}
