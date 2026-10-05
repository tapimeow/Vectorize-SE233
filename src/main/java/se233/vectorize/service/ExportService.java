// Team ownership: Day (Backend/Logic). See README.md "Team split".
package se233.vectorize.service;

import se233.vectorize.exception.ConversionException;
import se233.vectorize.model.VectorizationSettings;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
        checkCancelled(cancelled);
        if (folder == null || !Files.isDirectory(folder) || !Files.isWritable(folder)) {
            throw new ConversionException(ConversionException.Kind.OUTPUT,
                    "The output folder does not exist or cannot be written: " + folder);
        }

        // Reuse the preview only if source, settings (checked by caller) and pipeline still match.
        String svg;
        List<Integer> colors = List.of();
        String version = item.cachedSourceVersion();
        String currentVersion = null;
        if (item.cachedSvg() != null && item.cachedSourceVersion() != null) {
            try {
                currentVersion = VectorizationService.sourceVersion(item.source());
            } catch (IOException e) {
                currentVersion = null;               // unreadable now: let convert report it
            }
        }
        boolean fresh = currentVersion != null
                && currentVersion.equals(item.cachedSourceVersion())
                && VectorizationService.PIPELINE_VERSION.equals(item.cachedPipelineVersion());
        if (fresh) {
            svg = item.cachedSvg();
        } else {
            VectorizationService.ConversionResult result =
                    vectorization.convert(item.source(), item.settings(), cancelled, stage);
            svg = result.svg();
            colors = result.detectedColors();
            version = result.sourceVersion();
        }
        stage.accept("Checking SVG");
        vectorization.validator().validate(svg);
        checkCancelled(cancelled);

        stage.accept("Writing file");
        Path target = reserve(folder, item.displayName());
        Path temp = null;
        try {
            temp = Files.createTempFile(folder, ".vectorize-", ".tmp");
            Files.writeString(temp, svg, StandardCharsets.UTF_8);
            checkCancelled(cancelled);
            Files.move(temp, target);                // no REPLACE_EXISTING: never overwrite silently
            temp = null;
            return new ExportOutcome(target, svg, colors, version, VectorizationService.PIPELINE_VERSION);
        } catch (IOException e) {
            throw new ConversionException(ConversionException.Kind.OUTPUT,
                    "Could not write " + target.getFileName() + ": " + e.getMessage(), e);
        } finally {
            if (temp != null) {
                try { Files.deleteIfExists(temp); } catch (IOException ignored) { }
            }
            synchronized (reserved) { reserved.remove(target); }
        }
    }

    /** Pick name.svg, name-1.svg, ... that neither exists nor is being written by a sibling job. */
    private Path reserve(Path folder, String displayName) {
        String base = displayName;
        int dot = base.lastIndexOf('.');
        if (dot > 0) base = base.substring(0, dot);
        base = base.replaceAll("[\\\\/:*?\"<>|]", "_");
        synchronized (reserved) {
            for (int i = 0; ; i++) {
                Path candidate = folder.resolve(i == 0 ? base + ".svg" : base + "-" + i + ".svg");
                if (!reserved.contains(candidate) && !Files.exists(candidate)) {
                    reserved.add(candidate);
                    return candidate;
                }
            }
        }
    }

    private static void checkCancelled(BooleanSupplier cancelled) throws ConversionException {
        if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) {
            throw new ConversionException(ConversionException.Kind.CANCELLED, "Export cancelled.");
        }
    }
}
