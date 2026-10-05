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
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import javax.imageio.ImageIO;

/** Phase 4 pipeline orchestrator; no JavaFX or CLI commands here. */
public final class VectorizationService {
    public static final String PIPELINE_VERSION = "autotrace-1";
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
        checkCancelled(cancelled);
        stage.accept("Reading image");
        String version;
        BufferedImage image;
        try {
            version = sourceVersion(source);
            image = ImageIO.read(source.toFile());
        } catch (IOException e) {
            throw new ConversionException(ConversionException.Kind.INPUT,
                    "Could not read " + source.getFileName() + ": " + e.getMessage(), e);
        }
        if (image == null) {
            throw new ConversionException(ConversionException.Kind.INPUT,
                    source.getFileName() + " is not a readable JPG or PNG image.");
        }

        checkCancelled(cancelled);
        stage.accept("Preparing colors");
        RasterPreprocessor.PreparedRaster prepared = preprocessor.prepare(image, settings);
        image.flush();

        checkCancelled(cancelled);
        Path temp = null;
        try {
            temp = Files.createTempFile("vectorize-prepared-", ".png");
            if (!ImageIO.write(prepared.image(), "png", temp.toFile())) {
                throw new ConversionException(ConversionException.Kind.ENGINE, "No PNG writer is available.");
            }
            stage.accept("Tracing vectors");
            String svg = engine.trace(temp, settings, prepared.ignoredColor(), cancelled);
            checkCancelled(cancelled);
            stage.accept("Checking SVG");
            validator.validate(svg);
            return new ConversionResult(svg, prepared.detectedColors(), version, PIPELINE_VERSION);
        } catch (IOException e) {
            throw new ConversionException(ConversionException.Kind.ENGINE,
                    "Could not write the temporary image for tracing: " + e.getMessage(), e);
        } finally {
            prepared.image().flush();
            if (temp != null) {
                try { Files.deleteIfExists(temp); } catch (IOException ignored) { }
            }
        }
    }

    /**
     * Identify when a source changed so export does not reuse a stale preview.
     * TODO Phase 7: choose a bounded source signature (e.g. metadata or digest),
     * document its trade-off, and test modification between preview and export.
     * Filesystem work must occur on a worker, not in an Export All handler.
     */
    public static String sourceVersion(Path source) throws IOException {
        // Size + last-modified time: cheap (no full read) and catches normal edits;
        // trade-off: a same-size rewrite within one timestamp tick would be missed.
        return Files.size(source) + ":" + Files.getLastModifiedTime(source).toMillis();
    }

    private static void checkCancelled(BooleanSupplier cancelled) throws ConversionException {
        if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) {
            throw new ConversionException(ConversionException.Kind.CANCELLED, "Conversion cancelled.");
        }
    }
}
