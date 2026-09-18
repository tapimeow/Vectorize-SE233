// Team ownership: Day (Backend/Logic). See README.md "Team split".
package se233.vectorize.service;

import se233.vectorize.exception.ConversionException;
import se233.vectorize.model.VectorizationSettings;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Phase 7 single-image export operation; batch scheduling belongs to coordinator. */
public final class ExportService {
    public record ExportItem(Path source, String displayName, VectorizationSettings settings,
                             String cachedSvg, String cachedSourceVersion,
                             String cachedPipelineVersion) { }
    public record ExportOutcome(Path savedPath, String svg, List<Integer> detectedColors,
                                String sourceVersion, String pipelineVersion) { }
    private final VectorizationService vectorization;
    private final Set<Path> reserved = new HashSet<>();
    public ExportService(VectorizationService vectorization) { this.vectorization = vectorization; }

    /**
     * Produce one valid openable SVG in the OS-selected folder (FR-015–FR-017).
     * TODO Phase 7, worker only: check folder access and cancellation; compare
     * source/settings/pipeline signatures before reusing a preview; process a
     * stale image through VectorizationService; validate SVG again; reserve a
     * collision-free .svg name; write via a temporary file and avoid silent
     * overwrite; clean partial files after failure. Return path/result details.
     * Coordinator owns parallel jobs and aggregate progress, not this method.
     * Test inaccessible folder, name collisions, source changes, write failure,
     * cancellation and externally opening JPG/PNG results.
     */
    public ExportOutcome exportOne(ExportItem item, Path folder, BooleanSupplier cancelled,
                                   Consumer<String> stage) throws ConversionException {
        throw new UnsupportedOperationException("TODO Phase 7: final SVG export");
    }
}
