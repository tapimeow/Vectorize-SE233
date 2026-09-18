// Team ownership: Day (Backend/Logic). See README.md "Team split".
package se233.vectorize.service;

import se233.vectorize.adapter.VectorEngine;
import se233.vectorize.exception.ConversionException;
import se233.vectorize.model.VectorizationSettings;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.io.IOException;

/** Phase 4 pipeline orchestrator; no JavaFX or CLI commands here. */
public final class VectorizationService {
    public static final String PIPELINE_VERSION = "student-todo";
    public record ConversionResult(String svg, List<Integer> detectedColors,
                                   String sourceVersion, String pipelineVersion) { }
    private final RasterPreprocessor preprocessor = new RasterPreprocessor();
    private final SvgValidationService validator = new SvgValidationService();
    private final VectorEngine engine;
    public VectorizationService(VectorEngine engine) { this.engine = engine; }
    public SvgValidationService validator() { return validator; }

    /**
     * Turn a JPG/PNG path into validated standalone vector SVG (FR-007–FR-018).
     * TODO Phase 4, worker only: check cancellation; decode/verify source and
     * capture a version signature; prepare detail, palette and background mask;
     * write an intermediate engine-readable raster; call VectorEngine.trace;
     * reject invalid or raster-embedded SVG; return SVG/swatches/signatures.
     * Publish honest stage messages, not invented trace percentages. Clean all
     * temporary resources on success, failure and cancellation. Wrap file/tool
     * errors with useful ConversionException categories. Test JPG, PNG/alpha,
     * Low/Medium/High, Unlimited/Custom 2–5, transparent output and bad engine.
     */
    public ConversionResult convert(Path source, VectorizationSettings settings,
                                    BooleanSupplier cancelled, Consumer<String> stage)
                                    throws ConversionException {
        throw new UnsupportedOperationException("TODO Phase 4: vectorization pipeline");
    }

    /**
     * Identify when a source changed so export does not reuse a stale preview.
     * TODO Phase 7: choose a bounded source signature (e.g. metadata or digest),
     * document its trade-off, and test modification between preview and export.
     * Filesystem work must occur on a worker, not in an Export All handler.
     */
    public static String sourceVersion(Path source) throws IOException {
        throw new UnsupportedOperationException("TODO Phase 7: source freshness");
    }
}
