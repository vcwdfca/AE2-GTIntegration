package com.formlessDragon.ae2gtaddon.integration.gregtech;

import ae2.api.crafting.IPatternDetails;
import ae2.api.implementations.blockentities.ICraftingMachine;
import ae2.api.implementations.blockentities.IPatternProviderBatchTarget;
import ae2.api.implementations.blockentities.PatternContainerGroup;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.AEKeyType;
import ae2.api.stacks.GenericStack;
import ae2.api.stacks.KeyCounter;
import gregtech.api.capability.IGhostSlotConfigurable;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GregTechCircuitPatternDetailsTest {
    private static final TestKey IRON = new TestKey("iron");
    private static final TestKey GOLD = new TestKey("gold");
    private static final TestKey REDSTONE = new TestKey("redstone");
    private static final TestKey CIRCUIT_4 = new TestKey("circuit-4");
    private static final TestKey CIRCUIT_7 = new TestKey("circuit-7");
    private static final TestKey CIRCUIT_12 = new TestKey("circuit-12");
    private static final TestKey CIRCUIT_21 = new TestKey("circuit-21");
    private static final TestCircuitStrategy CIRCUITS = new TestCircuitStrategy();

    @Test
    void findsFirstCircuitConfigurationWithInjectedExtractor() {
        TestPatternDetails patternDetails = new TestPatternDetails(
            new TestInput(IRON),
            new TestInput(CIRCUIT_7));

        OptionalInt configuration = GregTechCircuitPatternDetails.findCircuitConfiguration(patternDetails, CIRCUITS);

        assertTrue(configuration.isPresent());
        assertEquals(7, configuration.getAsInt());
    }

    @Test
    void wrappedInputsExcludeCircuitChoices() {
        TestPatternDetails patternDetails = new TestPatternDetails(
            new TestInput(IRON, CIRCUIT_4),
            new TestInput(GOLD));

        IPatternDetails wrapped = GregTechCircuitPatternDetails.filterIntegratedCircuits(patternDetails, CIRCUITS);

        IPatternDetails.IInput[] inputs = wrapped.getInputs();
        assertEquals(2, inputs.length);
        assertStacks(List.of(IRON), inputs[0].possibleInputs());
        assertStacks(List.of(GOLD), inputs[1].possibleInputs());
    }

    @Test
    void wrappedInputsDropCircuitOnlyIngredients() {
        TestPatternDetails patternDetails = new TestPatternDetails(
            new TestInput(CIRCUIT_12),
            new TestInput(REDSTONE));

        IPatternDetails wrapped = GregTechCircuitPatternDetails.filterIntegratedCircuits(patternDetails, CIRCUITS);

        IPatternDetails.IInput[] inputs = wrapped.getInputs();
        assertEquals(1, inputs.length);
        assertStacks(List.of(REDSTONE), inputs[0].possibleInputs());
    }

    @Test
    void plannerInputViewDropsCircuitOnlyIngredients() {
        TestPatternDetails patternDetails = new TestPatternDetails(
            new TestInput(CIRCUIT_12),
            new TestInput(REDSTONE));

        IPatternDetails wrapped = GregTechCircuitPatternDetails.filterIntegratedCircuits(patternDetails, CIRCUITS);

        IPatternDetails.IInput[] plannerInputs = wrapped.getInputs();
        assertEquals(1, plannerInputs.length);
        assertStacks(List.of(REDSTONE), plannerInputs[0].possibleInputs());
    }

    @Test
    void wrappedPatternStillExposesOriginalCircuitConfiguration() {
        TestPatternDetails patternDetails = new TestPatternDetails(
            new TestInput(CIRCUIT_12),
            new TestInput(REDSTONE));
        IPatternDetails wrapped = GregTechCircuitPatternDetails.filterIntegratedCircuits(patternDetails, CIRCUITS);

        OptionalInt configuration = GregTechCircuitPatternDetails.findCircuitConfiguration(wrapped, CIRCUITS);

        assertTrue(configuration.isPresent());
        assertEquals(12, configuration.getAsInt());
    }

    @Test
    void plannerInputCountIgnoresFilteredCircuitInputs() {
        TestPatternDetails patternDetails = new TestPatternDetails(
            new TestInput(3, CIRCUIT_12),
            new TestInput(2, REDSTONE));
        IPatternDetails wrapped = GregTechCircuitPatternDetails.filterIntegratedCircuits(patternDetails, CIRCUITS);

        assertEquals(0, countPlannerInputs(wrapped, CIRCUIT_12));
        assertEquals(2, countPlannerInputs(wrapped, REDSTONE));
    }

    @Test
    void plannerPatternNetIgnoresFilteredCircuitInputs() {
        TestPatternDetails patternDetails = new TestPatternDetails(
            List.of(new GenericStack(GOLD, 4)),
            new TestInput(3, CIRCUIT_12),
            new TestInput(2, REDSTONE));
        IPatternDetails wrapped = GregTechCircuitPatternDetails.filterIntegratedCircuits(patternDetails, CIRCUITS);

        KeyCounter net = new KeyCounter();
        Set<AEKey> inputKeys = new HashSet<>();
        accumulatePlannerPatternNet(net, inputKeys, wrapped, 2);

        assertEquals(8, net.get(GOLD));
        assertEquals(-4, net.get(REDSTONE));
        assertEquals(0, net.get(CIRCUIT_12));
        assertFalse(inputKeys.contains(CIRCUIT_12));
        assertTrue(inputKeys.contains(REDSTONE));
    }

    @Test
    void unwrapReturnsOriginalPatternForFilteredCircuitPattern() {
        TestPatternDetails patternDetails = new TestPatternDetails(
            new TestInput(CIRCUIT_12),
            new TestInput(REDSTONE));
        IPatternDetails wrapped = GregTechCircuitPatternDetails.filterIntegratedCircuits(patternDetails, CIRCUITS);

        assertSame(patternDetails, GregTechCircuitPatternDetails.unwrap(wrapped));
    }

    @Test
    void unwrapPseudoKeepsCircuitWrapperWhenPatternIsNotPseudo() {
        TestPatternDetails patternDetails = new TestPatternDetails(
            new TestInput(CIRCUIT_12),
            new TestInput(REDSTONE));
        IPatternDetails wrapped = GregTechCircuitPatternDetails.filterIntegratedCircuits(patternDetails, CIRCUITS);

        assertSame(wrapped, GregTechCircuitPatternDetails.unwrapPseudo(wrapped));
    }

    @Test
    void filteringAlreadyFilteredPatternKeepsSameWrapper() {
        TestPatternDetails patternDetails = new TestPatternDetails(
            new TestInput(CIRCUIT_12),
            new TestInput(REDSTONE));
        IPatternDetails wrapped = GregTechCircuitPatternDetails.filterIntegratedCircuits(patternDetails, CIRCUITS);

        assertSame(wrapped, GregTechCircuitPatternDetails.filterIntegratedCircuits(wrapped, CIRCUITS));
    }

    @Test
    void repeatedFilteringOfSamePatternCanHitPlannerPatternMaps() {
        TestPatternDetails patternDetails = new TestPatternDetails(
            new TestInput(CIRCUIT_12),
            new TestInput(REDSTONE));
        IPatternDetails firstWrapped = GregTechCircuitPatternDetails.filterIntegratedCircuits(patternDetails, CIRCUITS);
        IPatternDetails secondWrapped = GregTechCircuitPatternDetails.filterIntegratedCircuits(patternDetails, CIRCUITS);

        Map<IPatternDetails, String> providersByPattern = new HashMap<>();
        providersByPattern.put(firstWrapped, "provider");

        assertEquals(firstWrapped, secondWrapped);
        assertEquals(firstWrapped.hashCode(), secondWrapped.hashCode());
        assertEquals("provider", providersByPattern.get(secondWrapped));
    }

    @Test
    void unwrapKeepsInnerWrapperForDoubleWrappedPattern() {
        TestPatternDetails patternDetails = new TestPatternDetails(
            new TestInput(CIRCUIT_12),
            new TestInput(REDSTONE));
        TestPatternDetailsWrapper innerWrapper = new TestPatternDetailsWrapper(patternDetails);
        IPatternDetails wrapped = GregTechCircuitPatternDetails.filterIntegratedCircuits(innerWrapper, CIRCUITS);

        assertSame(innerWrapper, GregTechCircuitPatternDetails.unwrap(wrapped));
    }

    @Test
    void registeredProviderPatternMatchesFilteredCircuitWrapper() {
        TestPatternDetails patternDetails = new TestPatternDetails(
            new TestInput(CIRCUIT_12),
            new TestInput(REDSTONE));
        IPatternDetails wrapped = GregTechCircuitPatternDetails.filterIntegratedCircuits(patternDetails, CIRCUITS);

        assertTrue(GregTechCircuitPatternDetails.containsPattern(List.of(patternDetails), wrapped));
    }

    @Test
    void pushInputsToExternalInventorySkipsCircuitKeysAndKeepsDelegateAmounts() {
        TestPatternDetails patternDetails = new TestPatternDetails(
            new TestInput(CIRCUIT_21),
            new TestInput(IRON));
        IPatternDetails wrapped = GregTechCircuitPatternDetails.filterIntegratedCircuits(patternDetails, CIRCUITS);

        KeyCounter firstInput = new KeyCounter();
        firstInput.add(CIRCUIT_21, 1);
        KeyCounter secondInput = new KeyCounter();
        secondInput.add(IRON, 3);

        List<GenericStack> pushed = new ArrayList<>();
        wrapped.pushInputsToExternalInventory(new KeyCounter[] { firstInput, secondInput },
            (key, amount) -> pushed.add(new GenericStack(key, amount)));

        assertEquals(1, pushed.size());
        assertSame(IRON, pushed.getFirst().what());
        assertEquals(3, pushed.getFirst().amount());
    }

    @Test
    void pushInputsToExternalInventoryAcceptsFilteredInputHolderAfterDroppingCircuitOnlyInput() {
        TestPatternDetails patternDetails = new IndexingExternalPushPatternDetails(
            new TestInput(CIRCUIT_21),
            new TestInput(IRON));
        IPatternDetails wrapped = GregTechCircuitPatternDetails.filterIntegratedCircuits(patternDetails, CIRCUITS);

        KeyCounter materialInput = new KeyCounter();
        materialInput.add(IRON, 3);

        List<GenericStack> pushed = new ArrayList<>();
        wrapped.pushInputsToExternalInventory(new KeyCounter[] { materialInput },
            (key, amount) -> pushed.add(new GenericStack(key, amount)));

        assertEquals(1, pushed.size());
        assertSame(IRON, pushed.getFirst().what());
        assertEquals(3, pushed.getFirst().amount());
    }

    @Test
    void filteredInputRejectsCircuitValidityAndDelegatesNormalValidity() {
        TestInput delegateInput = new TestInput(IRON, CIRCUIT_4);
        IPatternDetails wrapped = GregTechCircuitPatternDetails.filterIntegratedCircuits(
            new TestPatternDetails(delegateInput),
            CIRCUITS);
        IPatternDetails.IInput filteredInput = wrapped.getInputs()[0];

        assertTrue(filteredInput.isValid(IRON, null));
        assertFalse(filteredInput.isValid(CIRCUIT_4, null));
        assertSame(REDSTONE, filteredInput.getRemainingKey(IRON));
        assertEquals(1, delegateInput.validityChecks);
        assertEquals(1, delegateInput.remainingKeyChecks);
    }

    @Test
    void noCircuitPatternHasNoConfigurationAndKeepsInputs() {
        TestPatternDetails patternDetails = new TestPatternDetails(
            new TestInput(IRON),
            new TestInput(GOLD));

        IPatternDetails wrapped = GregTechCircuitPatternDetails.filterIntegratedCircuits(patternDetails, CIRCUITS);

        assertTrue(GregTechCircuitPatternDetails.findCircuitConfiguration(patternDetails, CIRCUITS).isEmpty());
        assertEquals(2, wrapped.getInputs().length);
    }

    @Test
    void filterInputHolderRemovesCircuitEntriesWithoutMutatingOriginalCounters() {
        KeyCounter firstInput = new KeyCounter();
        firstInput.add(CIRCUIT_4, 1);
        firstInput.add(IRON, 2);
        KeyCounter secondInput = new KeyCounter();
        secondInput.add(CIRCUIT_7, 1);

        KeyCounter[] filtered = GregTechCircuitPatternDetails.filterInputHolder(
            new KeyCounter[] { firstInput, secondInput },
            CIRCUITS);

        assertEquals(2, filtered.length);
        assertEquals(0, filtered[0].get(CIRCUIT_4));
        assertEquals(2, filtered[0].get(IRON));
        assertTrue(filtered[1].isEmpty());
        assertEquals(1, firstInput.get(CIRCUIT_4));
        assertEquals(1, secondInput.get(CIRCUIT_7));
    }

    @Test
    void wrappedCraftingMachineSetsGhostCircuitAndFiltersMachineInputs() {
        TestPatternDetails patternDetails = new TestPatternDetails(
            new TestInput(CIRCUIT_4),
            new TestInput(IRON));
        KeyCounter circuitSlot = new KeyCounter();
        circuitSlot.add(CIRCUIT_4, 1);
        KeyCounter materialSlot = new KeyCounter();
        materialSlot.add(IRON, 3);
        KeyCounter[] inputHolder = new KeyCounter[] { circuitSlot, materialSlot };
        RecordingCraftingMachine machine = new RecordingCraftingMachine();
        RecordingGhostCircuitTarget ghostTarget = new RecordingGhostCircuitTarget();

        ICraftingMachine wrapped = GregTechCircuitPatternDetails.wrapCraftingMachine(
            machine,
            ghostTarget,
            4,
            CIRCUITS);

        assertEquals(9, wrapped.getMaxPatternPushMultiplier(patternDetails, inputHolder, 9, EnumFacing.NORTH));
        assertEquals(4, ghostTarget.lastConfig);
        assertEquals(0, machine.lastBatchInputs[0].get(CIRCUIT_4));
        assertEquals(3, machine.lastBatchInputs[1].get(IRON));
        assertInstanceOf(GregTechCircuitPatternDetails.class, machine.lastBatchPattern);

        assertTrue(wrapped.pushPattern(patternDetails, inputHolder, 2, EnumFacing.NORTH));
        assertEquals(4, ghostTarget.lastConfig);
        assertEquals(0, machine.lastPushInputs[0].get(CIRCUIT_4));
        assertEquals(3, machine.lastPushInputs[1].get(IRON));
        assertInstanceOf(GregTechCircuitPatternDetails.class, machine.lastPushPattern);
        assertEquals(1, circuitSlot.get(CIRCUIT_4));
    }

    @Test
    void wrappedBatchTargetSetsGhostCircuitAndFiltersBatchInputs() {
        TestPatternDetails patternDetails = new TestPatternDetails(
            new TestInput(CIRCUIT_7),
            new TestInput(IRON));
        KeyCounter circuitSlot = new KeyCounter();
        circuitSlot.add(CIRCUIT_7, 1);
        KeyCounter materialSlot = new KeyCounter();
        materialSlot.add(IRON, 5);
        RecordingBatchTarget batchTarget = new RecordingBatchTarget();
        RecordingGhostCircuitTarget ghostTarget = new RecordingGhostCircuitTarget();

        IPatternProviderBatchTarget wrapped = GregTechCircuitPatternDetails.wrapBatchTarget(
            batchTarget,
            ghostTarget,
            7,
            CIRCUITS);

        assertEquals(4, wrapped.getMaxPatternPushMultiplier(
            patternDetails,
            new KeyCounter[] { circuitSlot, materialSlot },
            4,
            EnumFacing.SOUTH));
        assertEquals(7, ghostTarget.lastConfig);
        assertEquals(0, batchTarget.lastBatchInputs[0].get(CIRCUIT_7));
        assertEquals(5, batchTarget.lastBatchInputs[1].get(IRON));
        assertInstanceOf(GregTechCircuitPatternDetails.class, batchTarget.lastBatchPattern);
        assertEquals(1, circuitSlot.get(CIRCUIT_7));
    }

    private static void assertStacks(List<TestKey> expected, GenericStack[] actual) {
        assertEquals(expected.size(), actual.length);
        for (int i = 0; i < expected.size(); i++) {
            assertSame(expected.get(i), actual[i].what());
        }
    }

    private static long countPlannerInputs(IPatternDetails patternDetails, TestKey what) {
        long total = 0;
        for (IPatternDetails.IInput input : patternDetails.getInputs()) {
            for (GenericStack possibleInput : input.possibleInputs()) {
                if (what.matches(possibleInput)) {
                    total += possibleInput.amount() * input.getMultiplier();
                    break;
                }
            }
        }
        return total;
    }

    @SuppressWarnings("SameParameterValue")
    private static void accumulatePlannerPatternNet(
        KeyCounter netByKey,
        Set<AEKey> inputKeys,
        IPatternDetails patternDetails,
        long times
    ) {
        for (GenericStack output : patternDetails.getOutputs()) {
            netByKey.add(output.what(), output.amount() * times);
        }
        for (IPatternDetails.IInput input : patternDetails.getInputs()) {
            GenericStack[] possibleInputs = input.possibleInputs();
            if (possibleInputs.length == 0) {
                continue;
            }
            GenericStack primaryInput = possibleInputs[0];
            netByKey.add(primaryInput.what(), -primaryInput.amount() * input.getMultiplier() * times);
            inputKeys.add(primaryInput.what());
        }
    }

    private static class TestPatternDetails implements IPatternDetails {
        private final IPatternDetails.IInput[] inputs;
        private final List<GenericStack> outputs;

        private TestPatternDetails(IPatternDetails.IInput... inputs) {
            this(List.of(), inputs);
        }

        private TestPatternDetails(List<GenericStack> outputs, IPatternDetails.IInput... inputs) {
            this.inputs = inputs.clone();
            this.outputs = List.copyOf(outputs);
        }

        @Override
        public AEItemKey getDefinition() {
            throw new UnsupportedOperationException("Definition is not part of this logic test");
        }

        @Override
        public IPatternDetails.IInput[] getInputs() {
            return this.inputs.clone();
        }

        @Override
        public List<GenericStack> getOutputs() {
            return this.outputs;
        }
    }

    private record TestPatternDetailsWrapper(IPatternDetails delegate) implements IPatternDetails {

        @Override
            public AEItemKey getDefinition() {
                return this.delegate.getDefinition();
            }

            @Override
            public IInput[] getInputs() {
                return this.delegate.getInputs();
            }

            @Override
            public List<GenericStack> getOutputs() {
                return this.delegate.getOutputs();
            }
        }

    private static final class IndexingExternalPushPatternDetails extends TestPatternDetails {

        private IndexingExternalPushPatternDetails(IPatternDetails.IInput... inputs) {
            super(inputs);
        }

        @Override
        public void pushInputsToExternalInventory(KeyCounter[] inputHolder, PatternInputSink inputSink) {
            IPatternDetails.IInput[] inputs = this.getInputs();
            for (int i = 0; i < inputs.length; i++) {
                for (var input : inputHolder[i]) {
                    inputSink.pushInput(input.getKey(), input.getLongValue());
                }
            }
        }
    }

    private static final class TestInput implements IPatternDetails.IInput {
        private final GenericStack[] possibleInputs;
        private int validityChecks;
        private int remainingKeyChecks;

        private TestInput(TestKey... possibleInputs) {
            this(1, possibleInputs);
        }

        private TestInput(long multiplier, TestKey... possibleInputs) {
            this.possibleInputs = new GenericStack[possibleInputs.length];
            for (int i = 0; i < possibleInputs.length; i++) {
                this.possibleInputs[i] = new GenericStack(possibleInputs[i], 1);
            }
            this.multiplier = multiplier;
        }

        private final long multiplier;

        @Override
        public GenericStack[] possibleInputs() {
            return this.possibleInputs.clone();
        }

        @Override
        public long getMultiplier() {
            return this.multiplier;
        }

        @Override
        public boolean isValid(AEKey input, World level) {
            this.validityChecks++;
            for (GenericStack possibleInput : this.possibleInputs) {
                if (possibleInput.what().equals(input)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public AEKey getRemainingKey(AEKey template) {
            this.remainingKeyChecks++;
            return REDSTONE;
        }
    }

    private record TestCircuitStrategy()
        implements GregTechCircuitPatternDetails.CircuitMatcher, GregTechCircuitPatternDetails.CircuitConfigExtractor {

        @Override
        public boolean isCircuit(AEKey key) {
            return getCircuitConfiguration(key).isPresent();
        }

        @Override
        public OptionalInt getCircuitConfiguration(AEKey key) {
            if (key instanceof TestKey testKey && testKey.name().startsWith("circuit-")) {
                return OptionalInt.of(Integer.parseInt(testKey.name().substring("circuit-".length())));
            }
            return OptionalInt.empty();
        }
    }

    private static final class RecordingGhostCircuitTarget implements IGhostSlotConfigurable {
        private int lastConfig = -1;

        @Override
        public boolean hasGhostCircuitInventory() {
            return true;
        }

        @Override
        public void setGhostCircuitConfig(int config) {
            this.lastConfig = config;
        }
    }

    private static final class RecordingCraftingMachine implements ICraftingMachine {
        private IPatternDetails lastBatchPattern;
        private KeyCounter[] lastBatchInputs;
        private IPatternDetails lastPushPattern;
        private KeyCounter[] lastPushInputs;

        @Override
        public PatternContainerGroup getCraftingMachineInfo() {
            return PatternContainerGroup.nothing();
        }

        @Override
        public boolean pushPattern(
            IPatternDetails patternDetails,
            KeyCounter[] inputs,
            int multiplier,
            EnumFacing ejectionDirection
        ) {
            this.lastPushPattern = patternDetails;
            this.lastPushInputs = inputs;
            return true;
        }

        @Override
        public boolean acceptsPlans() {
            return true;
        }

        @Override
        public int getMaxPatternPushMultiplier(
            IPatternDetails patternDetails,
            KeyCounter[] inputs,
            int maxMultiplier,
            EnumFacing ejectionDirection
        ) {
            this.lastBatchPattern = patternDetails;
            this.lastBatchInputs = inputs;
            return maxMultiplier;
        }
    }

    private static final class RecordingBatchTarget implements IPatternProviderBatchTarget {
        private IPatternDetails lastBatchPattern;
        private KeyCounter[] lastBatchInputs;

        @Override
        public int getMaxPatternPushMultiplier(
            IPatternDetails patternDetails,
            KeyCounter[] inputs,
            int maxMultiplier,
            EnumFacing ejectionDirection
        ) {
            this.lastBatchPattern = patternDetails;
            this.lastBatchInputs = inputs;
            return maxMultiplier;
        }
    }

    private static final class TestKey extends AEKey {
        private final String name;

        private TestKey(String name) {
            this.name = name;
        }

        private String name() {
            return this.name;
        }

        @Override
        public AEKeyType getType() {
            return AEKeyType.items();
        }

        @Override
        public AEKey dropSecondary() {
            return this;
        }

        @Override
        public NBTTagCompound toTag() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("name", this.name);
            return tag;
        }

        @Override
        public Object getPrimaryKey() {
            return this.name;
        }

        @Override
        public ResourceLocation getId() {
            return new ResourceLocation("ae2gtaddon_test", this.name);
        }

        @Override
        public void writeToPacket(PacketBuffer data) {
            throw new UnsupportedOperationException("Packet serialization is not part of this logic test");
        }

        @Override
        public @Nullable Object getReadOnlyStack() {
            return this.name;
        }

        @Override
        protected ITextComponent computeDisplayName() {
            return new TextComponentString(this.name);
        }

        @Override
        public boolean isTagged(String tag) {
            return false;
        }

        @Override
        public @Nullable NBTBase get(String componentId) {
            return null;
        }

        @Override
        public boolean hasComponents() {
            return false;
        }
    }
}
