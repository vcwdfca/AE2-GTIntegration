package com.formlessDragon.ae2gtaddon.mixin.mod.ae2;

import ae2.container.me.items.ContainerPatternEncodingTerm;
import com.formlessDragon.ae2gtaddon.integration.hei.PatternEncodingPanelContainerAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "ae2.client.gui.me.items.EncodingModePanel", remap = false)
public class MixinEncodingModePanel implements PatternEncodingPanelContainerAccess {

    @Shadow
    @Final
    protected ContainerPatternEncodingTerm container;

    @Override
    public ContainerPatternEncodingTerm ae2gtaddon$getContainer() {
        return this.container;
    }
}
