package se233.vectorize;

import org.junit.jupiter.api.Test;
import se233.vectorize.concurrency.ProcessingPolicy;
import se233.vectorize.model.ColorMode;
import se233.vectorize.model.DetailLevel;
import se233.vectorize.model.VectorizationSettings;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProcessingPolicyTest {
    private final ProcessingPolicy policy = new ProcessingPolicy();

    @Test void rubricPredicateHasOneBoundary() {
        VectorizationSettings defaults = VectorizationSettings.defaults();
        assertFalse(policy.isParallelCandidate(1, defaults));
        assertTrue(policy.isParallelCandidate(2, defaults));
        assertFalse(policy.isParallelCandidate(2, defaults.withDetail(DetailLevel.HIGH)));
        assertFalse(policy.isParallelCandidate(2, defaults.withColorMode(ColorMode.UNLIMITED)));
        assertTrue(policy.hasParallelPair(List.of(defaults, defaults, defaults.withDetail(DetailLevel.LOW))));
        assertFalse(policy.hasParallelPair(List.of(defaults, defaults.withDetail(DetailLevel.LOW))));
    }

    @Test void settingsAreTypedAndBounded() {
        assertEquals(2, VectorizationSettings.defaults().customColors());
        assertThrows(IllegalArgumentException.class, () -> VectorizationSettings.defaults().withCustomColors(6));
        assertThrows(IllegalArgumentException.class, () -> VectorizationSettings.defaults().withCustomColors(1));
    }
}
