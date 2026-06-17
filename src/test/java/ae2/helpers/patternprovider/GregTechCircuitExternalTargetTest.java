package ae2.helpers.patternprovider;

import ae2.api.config.Actionable;
import ae2.api.config.PatternProviderInsertionMode;
import ae2.api.stacks.AEKey;
import com.formlessDragon.ae2gtaddon.integration.gregtech.GregTechCircuitPatternDetails;
import gregtech.api.capability.IGhostSlotConfigurable;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GregTechCircuitExternalTargetTest {
    private static final int CIRCUIT_CONFIGURATION = 12;

    @Test
    void configuredExternalTargetSetsGhostCircuitBeforeModulatedInsert() {
        RecordingPatternProviderTarget delegate = new RecordingPatternProviderTarget();
        RecordingGhostCircuitTarget ghostTarget = new RecordingGhostCircuitTarget();
        PatternProviderTarget wrapped = GregTechCircuitPatternDetails.wrapExternalTarget(
            delegate,
            ghostTarget,
            CIRCUIT_CONFIGURATION);

        long inserted = wrapped.insert(null, 3, Actionable.MODULATE, PatternProviderInsertionMode.DEFAULT);

        assertEquals(3, inserted);
        assertEquals(CIRCUIT_CONFIGURATION, ghostTarget.lastConfig);
        assertEquals(1, delegate.modulatedInsertCalls);
    }

    @Test
    void configuredExternalTargetDoesNotSetGhostCircuitForSimulatedInsert() {
        RecordingPatternProviderTarget delegate = new RecordingPatternProviderTarget();
        RecordingGhostCircuitTarget ghostTarget = new RecordingGhostCircuitTarget();
        PatternProviderTarget wrapped = GregTechCircuitPatternDetails.wrapExternalTarget(
            delegate,
            ghostTarget,
            CIRCUIT_CONFIGURATION);

        long inserted = wrapped.insert(null, 3, Actionable.SIMULATE, PatternProviderInsertionMode.DEFAULT);

        assertEquals(3, inserted);
        assertEquals(-1, ghostTarget.lastConfig);
        assertEquals(1, delegate.simulatedInsertCalls);
    }

    private static final class RecordingPatternProviderTarget implements PatternProviderTarget {
        private int simulatedInsertCalls;
        private int modulatedInsertCalls;

        @Override
        public long insert(AEKey what, long amount, Actionable type) {
            return this.insert(what, amount, type, PatternProviderInsertionMode.DEFAULT);
        }

        @Override
        public long insert(AEKey what, long amount, Actionable type, PatternProviderInsertionMode insertionMode) {
            if (type == Actionable.MODULATE) {
                this.modulatedInsertCalls++;
            } else {
                this.simulatedInsertCalls++;
            }
            return amount;
        }

        @Override
        public boolean containsPatternInput(Set<AEKey> patternInputs) {
            return false;
        }

        @Override
        public boolean containsAnyStack() {
            return false;
        }

        @Override
        public boolean hasEmptySlots() {
            return true;
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
}
