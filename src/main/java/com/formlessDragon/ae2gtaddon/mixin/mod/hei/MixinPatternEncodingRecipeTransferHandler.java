package com.formlessDragon.ae2gtaddon.mixin.mod.hei;

import ae2.api.stacks.GenericStack;
import ae2.container.me.items.ContainerPatternEncodingTerm;
import ae2.integration.modules.hei.GenericIngredientHelper;
import ae2.integration.modules.hei.PatternEncodingRecipeTransferHandler;
import com.formlessDragon.ae2gtaddon.integration.gregtech.GregTechCircuitPatternDetails;
import com.formlessDragon.ae2gtaddon.integration.hei.ProcessingPatternCircuitImportMode;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mezz.jei.api.gui.IRecipeLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = PatternEncodingRecipeTransferHandler.class, remap = false)
public class MixinPatternEncodingRecipeTransferHandler {
    @Unique
    private static final int AE2GTADDON_CRAFTING_GRID_SIZE = 9;

    @Unique
    private static boolean ae2gtaddon$ignoreCircuitForCurrentImport;

    @Inject(method = "encodeProcessingRecipe", at = @At("HEAD"))
    private static void ae2gtaddon$captureImportMode(
        ContainerPatternEncodingTerm container,
        IRecipeLayout recipeLayout,
        CallbackInfo ci
    ) {
        ae2gtaddon$ignoreCircuitForCurrentImport = ((ProcessingPatternCircuitImportMode) container)
            .ae2gtaddon$shouldIgnoreCircuitInHeiProcessingPattern();
    }

    @Inject(method = "encodeProcessingRecipe", at = @At("RETURN"))
    private static void ae2gtaddon$clearImportMode(
        ContainerPatternEncodingTerm container,
        IRecipeLayout recipeLayout,
        CallbackInfo ci
    ) {
        ae2gtaddon$ignoreCircuitForCurrentImport = false;
    }

    @Redirect(
        method = "encodeProcessingRecipe",
        at = @At(
            value = "INVOKE",
            target = "Lae2/integration/modules/hei/PatternEncodingRecipeTransferHandler;getGenericInputs(Lmezz/jei/api/gui/IRecipeLayout;)Ljava/util/List;"
        )
    )
    private static List<List<GenericStack>> ae2gtaddon$getGenericInputs(IRecipeLayout recipeLayout) {
        List<List<GenericStack>> inputs = GenericIngredientHelper.getIngredients(
            recipeLayout,
            true,
            false,
            AE2GTADDON_CRAFTING_GRID_SIZE);
        return ae2gtaddon$shouldIgnoreCircuitInProcessingImport()
            ? ae2gtaddon$filterIntegratedCircuits(inputs)
            : inputs;
    }

    @Unique
    private static boolean ae2gtaddon$shouldIgnoreCircuitInProcessingImport() {
        return ae2gtaddon$ignoreCircuitForCurrentImport;
    }

    @Unique
    private static List<List<GenericStack>> ae2gtaddon$filterIntegratedCircuits(
        List<List<GenericStack>> inputs
    ) {
        List<List<GenericStack>> filteredInputs = new ObjectArrayList<>(inputs.size());
        for (List<GenericStack> candidates : inputs) {
            List<GenericStack> filteredCandidates = new ObjectArrayList<>(candidates.size());
            for (GenericStack candidate : candidates) {
                if (candidate != null
                    && candidate.what() != null
                    && !GregTechCircuitPatternDetails.GREGTECH_CIRCUIT_MATCHER.isCircuit(candidate.what())) {
                    filteredCandidates.add(candidate);
                }
            }
            filteredInputs.add(filteredCandidates);
        }
        return filteredInputs;
    }
}
