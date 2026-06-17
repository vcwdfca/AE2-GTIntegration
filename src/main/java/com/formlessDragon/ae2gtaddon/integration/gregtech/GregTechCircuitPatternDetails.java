package com.formlessDragon.ae2gtaddon.integration.gregtech;

import ae2.api.crafting.IPatternDetails;
import ae2.api.crafting.PatternDetailsTooltip;
import ae2.api.implementations.blockentities.ICraftingMachine;
import ae2.api.implementations.blockentities.IPatternProviderBatchTarget;
import ae2.api.implementations.blockentities.PatternContainerGroup;
import ae2.api.config.Actionable;
import ae2.api.config.PatternProviderInsertionMode;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.GenericStack;
import ae2.api.stacks.KeyCounter;
import ae2.helpers.patternprovider.PatternProviderTarget;
import ae2.helpers.patternprovider.PseudoPatternDetails;
import gregtech.api.capability.IGhostSlotConfigurable;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.recipes.ingredients.IntCircuitIngredient;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Adapts AE2 pattern details for GregTech ghost circuit machines.
 *
 * <p>The circuit card needs the configured integrated circuit to control GregTech's ghost slot,
 * while the real item must not be inserted into the machine input inventory.</p>
 */
public final class GregTechCircuitPatternDetails implements IPatternDetails {
    public static final CircuitMatcher GREGTECH_CIRCUIT_MATCHER = new GregTechCircuitMatcher();
    public static final CircuitConfigExtractor GREGTECH_CIRCUIT_CONFIG_EXTRACTOR = new GregTechCircuitConfigExtractor();
    private static final ThreadLocal<Integer> ACTIVE_CIRCUIT_CONFIGURATION = new ThreadLocal<>();

    private final IPatternDetails delegate;
    private final CircuitMatcher circuitMatcher;
    private final IInput[] originalInputs;
    private final IInput[] filteredInputs;
    private final int[] filteredInputOriginalIndexes;

    private GregTechCircuitPatternDetails(IPatternDetails delegate, CircuitMatcher circuitMatcher) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.circuitMatcher = Objects.requireNonNull(circuitMatcher, "circuitMatcher");
        this.originalInputs = delegate.getInputs().clone();
        InputFilterResult filtered = filterInputs(this.originalInputs, circuitMatcher);
        this.filteredInputs = filtered.inputs();
        this.filteredInputOriginalIndexes = filtered.originalIndexes();
    }

    public static IPatternDetails filterIntegratedCircuits(IPatternDetails patternDetails) {
        return filterIntegratedCircuits(patternDetails, GREGTECH_CIRCUIT_MATCHER);
    }

    public static IPatternDetails filterIntegratedCircuits(IPatternDetails patternDetails, CircuitMatcher circuitMatcher) {
        if (patternDetails instanceof GregTechCircuitPatternDetails) {
            return patternDetails;
        }
        return new GregTechCircuitPatternDetails(patternDetails, circuitMatcher);
    }

    public static IPatternDetails unwrap(IPatternDetails patternDetails) {
        if (patternDetails instanceof GregTechCircuitPatternDetails circuitPatternDetails) {
            return unwrap(circuitPatternDetails.delegate);
        }
        return patternDetails;
    }

    public static IPatternDetails unwrapPseudo(IPatternDetails patternDetails) {
        if (patternDetails instanceof GregTechCircuitPatternDetails circuitPatternDetails) {
            IPatternDetails unwrappedDelegate = PseudoPatternDetails.unwrap(circuitPatternDetails.delegate);
            if (unwrappedDelegate == circuitPatternDetails.delegate) {
                return patternDetails;
            }
            return new GregTechCircuitPatternDetails(unwrappedDelegate, circuitPatternDetails.circuitMatcher);
        }
        return PseudoPatternDetails.unwrap(patternDetails);
    }

    public static boolean isFiltered(IPatternDetails patternDetails) {
        return patternDetails instanceof GregTechCircuitPatternDetails;
    }

    public static boolean containsPattern(Iterable<? extends IPatternDetails> registeredPatterns, Object requestedPattern) {
        Objects.requireNonNull(registeredPatterns, "registeredPatterns");
        for (IPatternDetails registeredPattern : registeredPatterns) {
            if (matchesRegisteredPattern(registeredPattern, requestedPattern)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesRegisteredPattern(IPatternDetails registeredPattern, Object requestedPattern) {
        Objects.requireNonNull(registeredPattern, "registeredPattern");
        if (!(requestedPattern instanceof IPatternDetails requestedPatternDetails)) {
            return Objects.equals(registeredPattern, requestedPattern);
        }
        return registeredPattern.equals(requestedPatternDetails)
            || registeredPattern.equals(unwrap(requestedPatternDetails));
    }

    public static void setActiveCircuitConfiguration(int circuitConfiguration) {
        if (circuitConfiguration < 0) {
            clearActiveCircuitConfiguration();
            return;
        }
        ACTIVE_CIRCUIT_CONFIGURATION.set(circuitConfiguration);
    }

    public static OptionalInt getActiveCircuitConfiguration() {
        Integer configuration = ACTIVE_CIRCUIT_CONFIGURATION.get();
        return configuration == null ? OptionalInt.empty() : OptionalInt.of(configuration);
    }

    public static void clearActiveCircuitConfiguration() {
        ACTIVE_CIRCUIT_CONFIGURATION.remove();
    }

    private IInput[] getOriginalInputs() {
        return this.originalInputs.clone();
    }

    private static IInput[] getCircuitSearchInputs(IPatternDetails patternDetails) {
        Objects.requireNonNull(patternDetails, "patternDetails");
        if (patternDetails instanceof GregTechCircuitPatternDetails circuitPatternDetails) {
            return circuitPatternDetails.getOriginalInputs();
        }
        return patternDetails.getInputs();
    }

    public static OptionalInt findCircuitConfiguration(IPatternDetails patternDetails) {
        return findCircuitConfiguration(patternDetails, GREGTECH_CIRCUIT_CONFIG_EXTRACTOR);
    }

    public static OptionalInt findCircuitConfiguration(
        IPatternDetails patternDetails,
        CircuitConfigExtractor circuitConfigExtractor
    ) {
        Objects.requireNonNull(patternDetails, "patternDetails");
        Objects.requireNonNull(circuitConfigExtractor, "circuitConfigExtractor");
        for (IInput input : getCircuitSearchInputs(patternDetails)) {
            for (GenericStack possibleInput : input.possibleInputs()) {
                OptionalInt configuration = circuitConfigExtractor.getCircuitConfiguration(possibleInput.what());
                if (configuration.isPresent()) {
                    return configuration;
                }
            }
        }
        return OptionalInt.empty();
    }

    public static KeyCounter[] filterInputHolder(KeyCounter[] inputHolder, CircuitMatcher circuitMatcher) {
        Objects.requireNonNull(inputHolder, "inputHolder");
        Objects.requireNonNull(circuitMatcher, "circuitMatcher");
        KeyCounter[] filtered = new KeyCounter[inputHolder.length];
        for (int i = 0; i < inputHolder.length; i++) {
            filtered[i] = new KeyCounter();
            for (var input : inputHolder[i]) {
                if (!circuitMatcher.isCircuit(input.getKey())) {
                    filtered[i].add(input.getKey(), input.getLongValue());
                }
            }
        }
        return filtered;
    }

    public static ICraftingMachine wrapCraftingMachine(
        ICraftingMachine machine,
        IGhostSlotConfigurable ghostTarget,
        int circuitConfiguration
    ) {
        return wrapCraftingMachine(machine, ghostTarget, circuitConfiguration, GREGTECH_CIRCUIT_MATCHER);
    }

    public static ICraftingMachine wrapCraftingMachine(
        ICraftingMachine machine,
        IGhostSlotConfigurable ghostTarget,
        int circuitConfiguration,
        CircuitMatcher circuitMatcher
    ) {
        Objects.requireNonNull(machine, "machine");
        Objects.requireNonNull(ghostTarget, "ghostTarget");
        Objects.requireNonNull(circuitMatcher, "circuitMatcher");
        return new CircuitConfiguredCraftingMachine(machine, ghostTarget, circuitConfiguration, circuitMatcher);
    }

    public static IPatternProviderBatchTarget wrapBatchTarget(
        IPatternProviderBatchTarget batchTarget,
        IGhostSlotConfigurable ghostTarget,
        int circuitConfiguration
    ) {
        return wrapBatchTarget(batchTarget, ghostTarget, circuitConfiguration, GREGTECH_CIRCUIT_MATCHER);
    }

    public static IPatternProviderBatchTarget wrapBatchTarget(
        IPatternProviderBatchTarget batchTarget,
        IGhostSlotConfigurable ghostTarget,
        int circuitConfiguration,
        CircuitMatcher circuitMatcher
    ) {
        Objects.requireNonNull(batchTarget, "batchTarget");
        Objects.requireNonNull(ghostTarget, "ghostTarget");
        Objects.requireNonNull(circuitMatcher, "circuitMatcher");
        return new CircuitConfiguredBatchTarget(batchTarget, ghostTarget, circuitConfiguration, circuitMatcher);
    }

    public static PatternProviderTarget wrapExternalTarget(
        PatternProviderTarget target,
        IGhostSlotConfigurable ghostTarget,
        int circuitConfiguration
    ) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(ghostTarget, "ghostTarget");
        return new CircuitConfiguredExternalTarget(target, ghostTarget, circuitConfiguration);
    }

    @Nullable
    public static IGhostSlotConfigurable findGhostCircuitTarget(
        @Nullable TileEntity tileEntity,
        @Nullable ICraftingMachine machine
    ) {
        if (machine instanceof IGhostSlotConfigurable configurable && configurable.hasGhostCircuitInventory()) {
            return configurable;
        }
        if (tileEntity instanceof IGregTechTileEntity gtTileEntity && gtTileEntity instanceof MetaTileEntityHolder
            && gtTileEntity.getMetaTileEntity() instanceof IGhostSlotConfigurable configurable
            && configurable.hasGhostCircuitInventory()) {
            return configurable;
        }
        return null;
    }

    private static boolean isGregTechIntegratedCircuit(AEKey key) {
        if (!(key instanceof AEItemKey itemKey)) {
            return false;
        }
        return IntCircuitIngredient.isIntegratedCircuit(itemKey.toStack());
    }

    private static OptionalInt getGregTechCircuitConfiguration(AEKey key) {
        if (!(key instanceof AEItemKey itemKey)) {
            return OptionalInt.empty();
        }
        ItemStack stack = itemKey.toStack();
        if (!IntCircuitIngredient.isIntegratedCircuit(stack)) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(IntCircuitIngredient.getCircuitConfiguration(stack));
    }

    @Override
    public AEItemKey getDefinition() {
        return this.delegate.getDefinition();
    }

    @Override
    public IInput[] getInputs() {
        return this.filteredInputs.clone();
    }

    @Override
    public List<GenericStack> getOutputs() {
        return this.delegate.getOutputs();
    }

    @Override
    public boolean supportsPushInputsToExternalInventory() {
        return this.delegate.supportsPushInputsToExternalInventory();
    }

    @Override
    public void pushInputsToExternalInventory(KeyCounter[] inputHolder, PatternInputSink inputSink) {
        Objects.requireNonNull(inputHolder, "inputHolder");
        Objects.requireNonNull(inputSink, "inputSink");
        this.delegate.pushInputsToExternalInventory(toOriginalInputHolder(inputHolder), (key, amount) -> {
            if (!this.circuitMatcher.isCircuit(key)) {
                inputSink.pushInput(key, amount);
            }
        });
    }

    @Override
    public PatternDetailsTooltip getTooltip(World level, ITooltipFlag flags) {
        return this.delegate.getTooltip(level, flags);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof GregTechCircuitPatternDetails other
            && this.delegate.equals(other.delegate)
            && this.circuitMatcher.equals(other.circuitMatcher);
    }

    @Override
    public int hashCode() {
        return 31 * this.delegate.hashCode() + this.circuitMatcher.hashCode();
    }

    private KeyCounter[] toOriginalInputHolder(KeyCounter[] inputHolder) {
        if (inputHolder.length == this.originalInputs.length) {
            return toOriginalInputHolder(inputHolder, this.originalInputs);
        }
        if (inputHolder.length == this.filteredInputs.length) {
            return expandFilteredInputHolder(inputHolder);
        }
        throw new IllegalArgumentException("Input holder length " + inputHolder.length
            + " matches neither filtered input count " + this.filteredInputs.length
            + " nor original input count " + this.originalInputs.length + ".");
    }

    private KeyCounter[] expandFilteredInputHolder(KeyCounter[] inputHolder) {
        KeyCounter[] expanded = createEmptyInputHolder(this.originalInputs.length);
        boolean[] hasRealInputSlot = new boolean[this.originalInputs.length];
        for (int filteredSlot = 0; filteredSlot < inputHolder.length; filteredSlot++) {
            int originalSlot = this.filteredInputOriginalIndexes[filteredSlot];
            expanded[originalSlot] = filterInputCounter(inputHolder[filteredSlot], this.circuitMatcher);
            hasRealInputSlot[originalSlot] = true;
        }
        synthesizeCircuitInputs(expanded, hasRealInputSlot, getInputPushMultiplier(inputHolder, this.filteredInputs));
        return expanded;
    }

    private KeyCounter[] toOriginalInputHolder(KeyCounter[] inputHolder, IInput[] inputs) {
        KeyCounter[] result = createEmptyInputHolder(inputs.length);
        boolean[] hasRealInputSlot = new boolean[inputs.length];
        for (int slot = 0; slot < inputs.length; slot++) {
            result[slot] = filterInputCounter(inputHolder[slot], this.circuitMatcher);
            hasRealInputSlot[slot] = hasNonCircuitInput(inputs[slot], this.circuitMatcher);
        }
        synthesizeCircuitInputs(result, hasRealInputSlot, getInputPushMultiplier(inputHolder, inputs));
        return result;
    }

    private static KeyCounter[] createEmptyInputHolder(int length) {
        KeyCounter[] result = new KeyCounter[length];
        for (int i = 0; i < result.length; i++) {
            result[i] = new KeyCounter();
        }
        return result;
    }

    private static KeyCounter filterInputCounter(KeyCounter inputCounter, CircuitMatcher circuitMatcher) {
        KeyCounter filtered = new KeyCounter();
        for (var input : inputCounter) {
            if (!circuitMatcher.isCircuit(input.getKey())) {
                filtered.add(input.getKey(), input.getLongValue());
            }
        }
        return filtered;
    }

    private void synthesizeCircuitInputs(
        KeyCounter[] inputHolder,
        boolean[] hasRealInputSlot,
        long patternPushMultiplier
    ) {
        for (int slot = 0; slot < this.originalInputs.length; slot++) {
            if (hasRealInputSlot[slot]) {
                continue;
            }
            GenericStack circuitTemplate = findCircuitTemplate(this.originalInputs[slot], this.circuitMatcher);
            long amount = Math.multiplyExact(
                Math.multiplyExact(circuitTemplate.amount(), this.originalInputs[slot].getMultiplier()),
                patternPushMultiplier);
            if (amount > 0) {
                inputHolder[slot].add(circuitTemplate.what(), amount);
            }
        }
    }

    private static long getInputPushMultiplier(KeyCounter[] inputHolder, IInput[] inputs) {
        long multiplier = Long.MAX_VALUE;
        for (int slot = 0; slot < inputs.length; slot++) {
            GenericStack[] possibleInputs = inputs[slot].possibleInputs();
            if (possibleInputs.length == 0) {
                continue;
            }
            long expectedAmount = Math.multiplyExact(possibleInputs[0].amount(), inputs[slot].getMultiplier());
            if (expectedAmount <= 0) {
                continue;
            }
            long actualAmount = 0;
            for (var input : inputHolder[slot]) {
                actualAmount += input.getLongValue();
            }
            multiplier = Math.min(multiplier, actualAmount / expectedAmount);
        }
        return multiplier == Long.MAX_VALUE ? 1 : multiplier;
    }

    private static boolean hasNonCircuitInput(IInput input, CircuitMatcher circuitMatcher) {
        for (GenericStack possibleInput : input.possibleInputs()) {
            if (!circuitMatcher.isCircuit(possibleInput.what())) {
                return true;
            }
        }
        return false;
    }

    private static GenericStack findCircuitTemplate(IInput input, CircuitMatcher circuitMatcher) {
        for (GenericStack possibleInput : input.possibleInputs()) {
            if (circuitMatcher.isCircuit(possibleInput.what())) {
                return possibleInput;
            }
        }
        throw new IllegalStateException("A filtered-out input slot did not contain a circuit template.");
    }

    private static InputFilterResult filterInputs(IInput[] inputs, CircuitMatcher circuitMatcher) {
        List<IInput> result = new ArrayList<>(inputs.length);
        List<Integer> originalIndexes = new ArrayList<>(inputs.length);
        for (int slot = 0; slot < inputs.length; slot++) {
            IInput input = inputs[slot];
            GenericStack[] possibleInputs = input.possibleInputs();
            GenericStack[] filteredPossibleInputs = Arrays.stream(possibleInputs)
                .filter(possibleInput -> !circuitMatcher.isCircuit(possibleInput.what()))
                .toArray(GenericStack[]::new);
            if (filteredPossibleInputs.length > 0) {
                result.add(new FilteredInput(input, circuitMatcher, filteredPossibleInputs));
                originalIndexes.add(slot);
            }
        }
        int[] indexes = new int[originalIndexes.size()];
        for (int i = 0; i < indexes.length; i++) {
            indexes[i] = originalIndexes.get(i);
        }
        return new InputFilterResult(result.toArray(IInput[]::new), indexes);
    }

    /**
     * Identifies AE keys that represent GregTech integrated circuits.
     */
    public interface CircuitMatcher {
        boolean isCircuit(AEKey key);
    }

    /**
     * Extracts the configured circuit number from AE keys that represent GregTech integrated circuits.
     */
    public interface CircuitConfigExtractor {
        OptionalInt getCircuitConfiguration(AEKey key);
    }

    private record GregTechCircuitMatcher() implements CircuitMatcher {

        @Override
        public boolean isCircuit(AEKey key) {
            return isGregTechIntegratedCircuit(key);
        }
    }

    private record GregTechCircuitConfigExtractor() implements CircuitConfigExtractor {

        @Override
        public OptionalInt getCircuitConfiguration(AEKey key) {
            return getGregTechCircuitConfiguration(key);
        }
    }

    private record InputFilterResult(IInput[] inputs, int[] originalIndexes) {

        private InputFilterResult {
            inputs = inputs.clone();
            originalIndexes = originalIndexes.clone();
        }

        @Override
        public IInput[] inputs() {
            return this.inputs.clone();
        }

        @Override
        public int[] originalIndexes() {
            return this.originalIndexes.clone();
        }
    }

    private record FilteredInput(
        IInput delegate,
        CircuitMatcher circuitMatcher,
        GenericStack[] possibleInputs
    ) implements IInput {

        private FilteredInput {
            Objects.requireNonNull(delegate, "delegate");
            Objects.requireNonNull(circuitMatcher, "circuitMatcher");
            possibleInputs = possibleInputs.clone();
        }

        @Override
        public GenericStack[] possibleInputs() {
            return this.possibleInputs.clone();
        }

        @Override
        public long getMultiplier() {
            return this.delegate.getMultiplier();
        }

        @Override
        public boolean isValid(AEKey input, World level) {
            return !this.circuitMatcher.isCircuit(input) && this.delegate.isValid(input, level);
        }

        @Override
        public @Nullable AEKey getRemainingKey(AEKey template) {
            if (this.circuitMatcher.isCircuit(template)) {
                return null;
            }
            return this.delegate.getRemainingKey(template);
        }
    }

    private record CircuitConfiguredCraftingMachine(
        ICraftingMachine delegate,
        IGhostSlotConfigurable ghostTarget,
        int circuitConfiguration,
        CircuitMatcher circuitMatcher
    ) implements ICraftingMachine {

        private CircuitConfiguredCraftingMachine {
            Objects.requireNonNull(delegate, "delegate");
            Objects.requireNonNull(ghostTarget, "ghostTarget");
            Objects.requireNonNull(circuitMatcher, "circuitMatcher");
        }

        @Override
        public PatternContainerGroup getCraftingMachineInfo() {
            return this.delegate.getCraftingMachineInfo();
        }

        @Override
        public boolean pushPattern(
            IPatternDetails patternDetails,
            KeyCounter[] inputs,
            int multiplier,
            EnumFacing ejectionDirection
        ) {
            this.ghostTarget.setGhostCircuitConfig(this.circuitConfiguration);
            return this.delegate.pushPattern(
                filterIntegratedCircuits(patternDetails, this.circuitMatcher),
                filterInputHolder(inputs, this.circuitMatcher),
                multiplier,
                ejectionDirection);
        }

        @Override
        public boolean acceptsPlans() {
            return this.delegate.acceptsPlans();
        }

        @Override
        public int getMaxPatternPushMultiplier(
            IPatternDetails patternDetails,
            KeyCounter[] inputs,
            int maxMultiplier,
            EnumFacing ejectionDirection
        ) {
            return wrapBatchTarget(
                this.delegate,
                this.ghostTarget,
                this.circuitConfiguration,
                this.circuitMatcher).getMaxPatternPushMultiplier(patternDetails, inputs, maxMultiplier, ejectionDirection);
        }
    }

    private record CircuitConfiguredBatchTarget(
        IPatternProviderBatchTarget delegate,
        IGhostSlotConfigurable ghostTarget,
        int circuitConfiguration,
        CircuitMatcher circuitMatcher
    ) implements IPatternProviderBatchTarget {

        private CircuitConfiguredBatchTarget {
            Objects.requireNonNull(delegate, "delegate");
            Objects.requireNonNull(ghostTarget, "ghostTarget");
            Objects.requireNonNull(circuitMatcher, "circuitMatcher");
        }

        @Override
        public int getMaxPatternPushMultiplier(
            IPatternDetails patternDetails,
            KeyCounter[] inputs,
            int maxMultiplier,
            EnumFacing ejectionDirection
        ) {
            this.ghostTarget.setGhostCircuitConfig(this.circuitConfiguration);
            return this.delegate.getMaxPatternPushMultiplier(
                filterIntegratedCircuits(patternDetails, this.circuitMatcher),
                filterInputHolder(inputs, this.circuitMatcher),
                maxMultiplier,
                ejectionDirection);
        }
    }

    private record CircuitConfiguredExternalTarget(
        PatternProviderTarget delegate,
        IGhostSlotConfigurable ghostTarget,
        int circuitConfiguration
    ) implements PatternProviderTarget {

        private CircuitConfiguredExternalTarget {
            Objects.requireNonNull(delegate, "delegate");
            Objects.requireNonNull(ghostTarget, "ghostTarget");
        }

        @Override
        public long insert(AEKey what, long amount, Actionable type) {
            return this.insert(what, amount, type, PatternProviderInsertionMode.DEFAULT);
        }

        @Override
        public long insert(AEKey what, long amount, Actionable type, PatternProviderInsertionMode insertionMode) {
            if (type == Actionable.MODULATE) {
                this.ghostTarget.setGhostCircuitConfig(this.circuitConfiguration);
            }
            return this.delegate.insert(what, amount, type, insertionMode);
        }

        @Override
        public boolean containsPatternInput(Set<AEKey> patternInputs) {
            return this.delegate.containsPatternInput(patternInputs);
        }

        @Override
        public boolean containsAnyStack() {
            return this.delegate.containsAnyStack();
        }

        @Override
        public boolean hasEmptySlots() {
            return this.delegate.hasEmptySlots();
        }
    }
}
