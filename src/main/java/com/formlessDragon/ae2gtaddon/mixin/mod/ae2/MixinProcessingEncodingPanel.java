package com.formlessDragon.ae2gtaddon.mixin.mod.ae2;

import ae2.client.gui.AEBaseGui;
import ae2.client.gui.Icon;
import ae2.client.gui.Tooltip;
import ae2.client.gui.WidgetContainer;
import ae2.client.gui.me.items.GuiPatternEncodingTerm;
import ae2.client.gui.me.items.ProcessingEncodingPanel;
import ae2.client.gui.widgets.ActionButton;
import ae2.client.gui.widgets.ToggleButton;
import com.formlessDragon.ae2gtaddon.integration.hei.PatternEncodingPanelContainerAccess;
import com.formlessDragon.ae2gtaddon.integration.hei.ProcessingPatternCircuitImportMode;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.text.TextComponentTranslation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

@Mixin(value = ProcessingEncodingPanel.class, remap = false)
public abstract class MixinProcessingEncodingPanel {
    @Unique
    private static final int AE2GTADDON_BUTTON_SPACING = 10;

    @Shadow
    @Final
    private ActionButton clearBtn;

    @Unique
    private ToggleButton ae2gtaddon$ignoreCircuitButton;

    @Unique
    private ProcessingPatternCircuitImportMode ae2gtaddon$importMode;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void ae2gtaddon$addIgnoreCircuitButton(
        GuiPatternEncodingTerm screen,
        WidgetContainer widgets,
        CallbackInfo ci
    ) {
        this.ae2gtaddon$importMode = (ProcessingPatternCircuitImportMode)
            ((PatternEncodingPanelContainerAccess) this).ae2gtaddon$getContainer();
        this.ae2gtaddon$ignoreCircuitButton = new ToggleButton(
            Icon.S_SUBSTITUTION_DISABLED,
            Icon.S_SUBSTITUTION_ENABLED,
            this::ae2gtaddon$setIgnoreCircuit);
        this.ae2gtaddon$ignoreCircuitButton.setHalfSize(true);
        this.ae2gtaddon$ignoreCircuitButton.setDisableBackground(true);
        this.ae2gtaddon$ignoreCircuitButton.setTooltipOn(List.of(
            new TextComponentTranslation("gui.tooltips.ae2gtaddon.IgnoreCircuitHeiImport"),
            new TextComponentTranslation("gui.tooltips.ae2gtaddon.IgnoreCircuitHeiImportOn")));
        this.ae2gtaddon$ignoreCircuitButton.setTooltipOff(List.of(
            new TextComponentTranslation("gui.tooltips.ae2gtaddon.IgnoreCircuitHeiImport"),
            new TextComponentTranslation("gui.tooltips.ae2gtaddon.IgnoreCircuitHeiImportOff")));
    }

    @Inject(method = "updateBeforeRender", at = @At("TAIL"))
    private void ae2gtaddon$updateIgnoreCircuitButton(CallbackInfo ci) {
        this.ae2gtaddon$ignoreCircuitButton.setState(this.ae2gtaddon$shouldIgnoreCircuit());
        this.ae2gtaddon$ignoreCircuitButton.x = this.clearBtn.x;
        this.ae2gtaddon$ignoreCircuitButton.y = this.clearBtn.y + AE2GTADDON_BUTTON_SPACING;
    }

    @Inject(method = "setVisible", at = @At("TAIL"))
    private void ae2gtaddon$setIgnoreCircuitButtonVisible(boolean visible, CallbackInfo ci) {
        this.ae2gtaddon$ignoreCircuitButton.setVisibility(visible);
    }

    @Unique
    private boolean ae2gtaddon$shouldIgnoreCircuit() {
        return this.ae2gtaddon$importMode.ae2gtaddon$shouldIgnoreCircuitInHeiProcessingPattern();
    }

    @Unique
    private void ae2gtaddon$setIgnoreCircuit(boolean ignoreCircuit) {
        this.ae2gtaddon$importMode.ae2gtaddon$setIgnoreCircuitInHeiProcessingPattern(ignoreCircuit);
    }

    @Unique
    @SuppressWarnings("all")
    public void populateScreen(Consumer<GuiButton> addWidget, Rectangle bounds, AEBaseGui<?> baseGui) {
        addWidget.accept(this.ae2gtaddon$ignoreCircuitButton);
    }

    @Unique
    @SuppressWarnings("all")
    public boolean wantsAllMouseDownEvents() {
        return true;
    }

    @Unique
    @SuppressWarnings("all")
    public Tooltip getTooltip(int mouseX, int mouseY) {
        if(!this.ae2gtaddon$ignoreCircuitButton.visible) {
            return null;
        }

        if(this.ae2gtaddon$ignoreCircuitButton.isMouseOver()) {
            return new Tooltip(ae2gtaddon$ignoreCircuitButton.getTooltipMessage());
        }
        return null;
    }
}
