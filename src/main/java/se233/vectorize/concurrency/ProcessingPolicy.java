package se233.vectorize.concurrency;

import se233.vectorize.model.ColorMode;
import se233.vectorize.model.DetailLevel;
import se233.vectorize.model.VectorizationSettings;

import java.util.List;

/** The assignment's parallel predicate belongs only here (FR-014). */
public final class ProcessingPolicy {
    /**
     * Decide whether one image is eligible for parallel work in this batch.
     * Phase 6 — concurrency; FR-014 and C-003; graded preprocessing/export.
     *
     * TODO: Examine the batch size, detail, and color mode as one policy.
     * Return a boolean; do not repeat the rule in a controller or export service.
     * Test one versus multiple files and matching versus nonmatching settings.
     * This pure method is safe on any thread.
     */
    public boolean isParallelCandidate(int imageCount, VectorizationSettings settings) {
        throw new UnsupportedOperationException("TODO Phase 6: graded parallel policy");
    }

    /**
     * Decide if a snapshot contains enough eligible independent jobs to overlap.
     * Phase 6 — FR-014; important when per-image settings differ.
     * TODO: Use the single-image policy above; clarify the lecturer's mixed-settings
     * interpretation in docs before changing this contract. Test empty, one, mixed,
     * and all-eligible snapshots. This method must not schedule worker threads.
     */
    public boolean hasParallelPair(List<VectorizationSettings> settings) {
        throw new UnsupportedOperationException("TODO Phase 6: parallel batch decision");
    }
}
