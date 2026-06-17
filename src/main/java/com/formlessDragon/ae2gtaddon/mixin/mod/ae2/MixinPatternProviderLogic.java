package com.formlessDragon.ae2gtaddon.mixin.mod.ae2;

import ae2.api.AECapabilities;
import ae2.api.crafting.IPatternDetails;
import ae2.api.implementations.blockentities.ICraftingMachine;
import ae2.api.implementations.blockentities.IPatternProviderBatchTarget;
import ae2.api.networking.crafting.ICraftingProvider;
import ae2.api.stacks.KeyCounter;
import ae2.api.upgrades.IUpgradeInventory;
import ae2.helpers.patternprovider.PatternProviderLogic;
import ae2.helpers.patternprovider.PatternProviderLogicHost;
import ae2.helpers.patternprovider.PatternProviderTarget;
import ae2.parts.p2p.PatternProviderP2PTunnelPart;
import ae2.util.inv.InternalInventoryHost;
import com.formlessDragon.ae2gtaddon.Init.Items;
import com.formlessDragon.ae2gtaddon.integration.gregtech.GregTechCircuitPatternDetails;
import gregtech.api.capability.IGhostSlotConfigurable;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

@Mixin(value = PatternProviderLogic.class, remap = false)
public abstract class MixinPatternProviderLogic implements InternalInventoryHost, ICraftingProvider {

    @Shadow
    @Final
    private IUpgradeInventory upgrades;

    @Shadow
    @Final
    private PatternProviderLogicHost host;

    @Shadow
    private PatternProviderTarget findAdapter(EnumFacing side) {
        throw new AssertionError();
    }

    @Unique
    private int ae2gtaddon$currentCircuitConfig = -1;

    @Redirect(
        method = {
            "canMergePatternPushBasic",
            "pushPattern"
        },
        at = @At(
            value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/objects/ObjectList;contains(Ljava/lang/Object;)Z"
        )
    )
    private boolean ae2gtaddon$containsOriginalPattern(ObjectList<IPatternDetails> patterns, Object patternDetails) {
        return GregTechCircuitPatternDetails.containsPattern(patterns, patternDetails);
    }

    @Inject(
        method = "getAvailablePatterns",
        at = @At("RETURN"),
        cancellable = true
    )
    private void ae2gtaddon$filterCircuitPatternsForCraftingPlan(CallbackInfoReturnable<List<IPatternDetails>> cir) {
        if (!this.ae2gtaddon$hasCircuitCard()) {
            return;
        }

        List<IPatternDetails> availablePatterns = cir.getReturnValue();
        if (availablePatterns.isEmpty()) {
            return;
        }

        boolean changed = false;
        List<IPatternDetails> filteredPatterns = new ArrayList<>(availablePatterns.size());
        for (IPatternDetails patternDetails : availablePatterns) {
            IPatternDetails filteredPattern = this.ae2gtaddon$filterCircuitPattern(patternDetails);
            filteredPatterns.add(filteredPattern);
            changed |= filteredPattern != patternDetails;
        }
        if (changed) {
            cir.setReturnValue(filteredPatterns);
        }
    }

    @Inject(
        method = "pushPattern",
        at = @At("HEAD")
    )
    private void ae2gtaddon$captureCircuitConfiguration(
        IPatternDetails patternDetails,
        KeyCounter[] inputHolder,
        int multiplier,
        CallbackInfoReturnable<Boolean> cir
    ) {
        this.ae2gtaddon$captureCircuitConfiguration(patternDetails);
    }

    @Inject(
        method = "getMaxPatternPushMultiplier",
        at = @At("HEAD")
    )
    private void ae2gtaddon$captureCircuitConfigurationForMergePush(
        IPatternDetails patternDetails,
        int maxMultiplier,
        CallbackInfoReturnable<Integer> cir
    ) {
        this.ae2gtaddon$captureCircuitConfiguration(patternDetails);
    }

    @Inject(
        method = "getMaxPatternPushMultiplierThroughP2P",
        at = @At("HEAD")
    )
    private void ae2gtaddon$captureCircuitConfigurationForP2PMergePush(
        PatternProviderP2PTunnelPart inputTunnel,
        IPatternDetails patternDetails,
        KeyCounter[] inputs,
        int maxMultiplier,
        CallbackInfoReturnable<Integer> cir
    ) {
        this.ae2gtaddon$captureCircuitConfiguration(patternDetails);
    }

    @Inject(
        method = "pushPatternThroughP2P",
        at = @At("HEAD")
    )
    private void ae2gtaddon$captureCircuitConfigurationForP2PPush(
        PatternProviderP2PTunnelPart inputTunnel,
        IPatternDetails patternDetails,
        KeyCounter[] inputHolder,
        int multiplier,
        CallbackInfoReturnable<Boolean> cir
    ) {
        this.ae2gtaddon$captureCircuitConfiguration(patternDetails);
    }

    @Unique
    private void ae2gtaddon$captureCircuitConfiguration(IPatternDetails patternDetails) {
        this.ae2gtaddon$currentCircuitConfig = -1;
        boolean hasCircuitCard = this.ae2gtaddon$hasCircuitCard();
        OptionalInt configuration;
        if (hasCircuitCard) {
            configuration = GregTechCircuitPatternDetails.findCircuitConfiguration(patternDetails);
            if (configuration.isPresent()) {
                this.ae2gtaddon$currentCircuitConfig = configuration.getAsInt();
                GregTechCircuitPatternDetails.setActiveCircuitConfiguration(this.ae2gtaddon$currentCircuitConfig);
                return;
            }
        }
        GregTechCircuitPatternDetails.clearActiveCircuitConfiguration();
    }

    @Inject(
        method = "pushPattern",
        at = @At("RETURN")
    )
    private void ae2gtaddon$clearCircuitConfiguration(
        IPatternDetails patternDetails,
        KeyCounter[] inputHolder,
        int multiplier,
        CallbackInfoReturnable<Boolean> cir
    ) {
        this.ae2gtaddon$currentCircuitConfig = -1;
        GregTechCircuitPatternDetails.clearActiveCircuitConfiguration();
    }

    @Inject(
        method = "getMaxPatternPushMultiplier",
        at = @At("RETURN")
    )
    private void ae2gtaddon$clearCircuitConfigurationForMergePush(
        IPatternDetails patternDetails,
        int maxMultiplier,
        CallbackInfoReturnable<Integer> cir
    ) {
        this.ae2gtaddon$currentCircuitConfig = -1;
        GregTechCircuitPatternDetails.clearActiveCircuitConfiguration();
    }

    @Inject(
        method = "getMaxPatternPushMultiplierThroughP2P",
        at = @At("RETURN")
    )
    private void ae2gtaddon$clearCircuitConfigurationForP2PMergePush(
        PatternProviderP2PTunnelPart inputTunnel,
        IPatternDetails patternDetails,
        KeyCounter[] inputs,
        int maxMultiplier,
        CallbackInfoReturnable<Integer> cir
    ) {
        this.ae2gtaddon$currentCircuitConfig = -1;
        GregTechCircuitPatternDetails.clearActiveCircuitConfiguration();
    }

    @Inject(
        method = "pushPatternThroughP2P",
        at = @At("RETURN")
    )
    private void ae2gtaddon$clearCircuitConfigurationForP2PPush(
        PatternProviderP2PTunnelPart inputTunnel,
        IPatternDetails patternDetails,
        KeyCounter[] inputHolder,
        int multiplier,
        CallbackInfoReturnable<Boolean> cir
    ) {
        this.ae2gtaddon$currentCircuitConfig = -1;
        GregTechCircuitPatternDetails.clearActiveCircuitConfiguration();
    }

    @Redirect(
        method = {
            "pushPattern",
            "canMergePatternPush",
            "getMaxPatternPushMultiplier",
            "getMaxPatternPushMultiplierThroughP2P",
            "pushPatternThroughP2P"
        },
        at = @At(
            value = "INVOKE",
            target = "Lae2/helpers/patternprovider/PseudoPatternDetails;unwrap(Lae2/api/crafting/IPatternDetails;)Lae2/api/crafting/IPatternDetails;"
        )
    )
    private IPatternDetails ae2gtaddon$unwrapCircuitPattern(IPatternDetails details) {
        return GregTechCircuitPatternDetails.unwrapPseudo(details);
    }

    @Redirect(
        method = "collectPushTargets",
        at = @At(
            value = "INVOKE",
            target = "Lae2/api/implementations/blockentities/ICraftingMachine;of(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;)Lae2/api/implementations/blockentities/ICraftingMachine;"
        )
    )
    private ICraftingMachine ae2gtaddon$collectCraftingMachine(World level, BlockPos pos, EnumFacing side) {
        TileEntity tileEntity = level.getTileEntity(pos);
        ICraftingMachine machine = ICraftingMachine.of(tileEntity, side);
        if (machine == null || this.ae2gtaddon$currentCircuitConfig < 0) {
            return machine;
        }

        IGhostSlotConfigurable ghostTarget = GregTechCircuitPatternDetails.findGhostCircuitTarget(tileEntity, machine);
        if (ghostTarget == null) {
            return machine;
        }

        return GregTechCircuitPatternDetails.wrapCraftingMachine(
            machine,
            ghostTarget,
            this.ae2gtaddon$currentCircuitConfig);
    }

    @Redirect(
        method = "collectPushTargets",
        at = @At(
            value = "INVOKE",
            target = "Lae2/helpers/patternprovider/PatternProviderLogic;getBatchTarget(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;Lae2/api/implementations/blockentities/ICraftingMachine;)Lae2/api/implementations/blockentities/IPatternProviderBatchTarget;"
        )
    )
    private IPatternProviderBatchTarget ae2gtaddon$collectBatchTarget(
        World level,
        BlockPos pos,
        EnumFacing side,
        ICraftingMachine fallback
    ) {
        TileEntity tileEntity = level.getTileEntity(pos);
        IPatternProviderBatchTarget batchTarget = fallback;
        if (tileEntity != null && tileEntity.hasCapability(AECapabilities.PATTERN_PROVIDER_BATCH_TARGET, side)) {
            IPatternProviderBatchTarget capabilityTarget = tileEntity.getCapability(
                AECapabilities.PATTERN_PROVIDER_BATCH_TARGET, side);
            if (capabilityTarget != null) {
                batchTarget = capabilityTarget;
            }
        }
        if (this.ae2gtaddon$currentCircuitConfig < 0) {
            return batchTarget;
        }

        IGhostSlotConfigurable ghostTarget = GregTechCircuitPatternDetails.findGhostCircuitTarget(tileEntity, fallback);
        if (ghostTarget == null || batchTarget == fallback) {
            return batchTarget;
        }

        return GregTechCircuitPatternDetails.wrapBatchTarget(
            batchTarget,
            ghostTarget,
            this.ae2gtaddon$currentCircuitConfig);
    }

    @Redirect(
        method = "collectPushTargets",
        at = @At(
            value = "INVOKE",
            target = "Lae2/helpers/patternprovider/PatternProviderLogic;findAdapter(Lnet/minecraft/util/EnumFacing;)Lae2/helpers/patternprovider/PatternProviderTarget;"
        )
    )
    private PatternProviderTarget ae2gtaddon$collectExternalTarget(PatternProviderLogic logic, EnumFacing side) {
        PatternProviderTarget target = this.findAdapter(side);
        return this.ae2gtaddon$wrapExternalTarget(side, target);
    }

    @Unique
    private PatternProviderTarget ae2gtaddon$wrapExternalTarget(EnumFacing direction, PatternProviderTarget target) {
        if (target == null || this.ae2gtaddon$currentCircuitConfig < 0) {
            return target;
        }

        TileEntity hostTileEntity = this.host.getTileEntity();
        World level = hostTileEntity.getWorld();
        if (level == null) {
            return target;
        }

        BlockPos targetPos = hostTileEntity.getPos().offset(direction);
        TileEntity targetTileEntity = level.getTileEntity(targetPos);
        IGhostSlotConfigurable ghostTarget = GregTechCircuitPatternDetails.findGhostCircuitTarget(targetTileEntity, null);
        if (ghostTarget == null) {
            return target;
        }

        return GregTechCircuitPatternDetails.wrapExternalTarget(
            target,
            ghostTarget,
            this.ae2gtaddon$currentCircuitConfig);
    }

    @Unique
    private boolean ae2gtaddon$hasCircuitCard() {
        return this.upgrades.getInstalledUpgrades(Items.CIRCUIT_CARD.item()) > 0;
    }

    @Unique
    private IPatternDetails ae2gtaddon$filterCircuitPattern(IPatternDetails patternDetails) {
        if (GregTechCircuitPatternDetails.findCircuitConfiguration(patternDetails).isEmpty()) {
            return patternDetails;
        }
        return GregTechCircuitPatternDetails.filterIntegratedCircuits(patternDetails);
    }

}
