package se233.vectorize.service;

import se233.vectorize.exception.ConversionException;
import se233.vectorize.model.ImageDocument;
import java.nio.file.Path;
import java.util.List;

/** Phase 3 boundary from dropped paths to ordered image documents. */
public final class ImageImportService {
    public record ImportResult(List<ImageDocument> documents, List<String> errors) { }
    private final ZipImportService zip;
    public ImageImportService(ZipImportService zip) { this.zip = zip; }

    /** TODO Phase 3: classify case-insensitive JPG/JPEG/PNG filenames for direct
     * and ZIP import. A matching suffix alone does not validate image contents. */
    public static boolean isSupportedImage(String name) {
        throw new UnsupportedOperationException("TODO Phase 3: supported image suffixes");
    }

    /**
     * Import files while retaining successes and per-file errors (FR-001–FR-003).
     * TODO Phase 3: reject unreadable/unsupported paths; delegate ZIP to zip.extract;
     * validate every direct/extracted image; create ImageDocument objects in a
     * deterministic order. A corrupt member should not erase valid siblings.
     * Return useful user messages, not stack traces. Call on a worker through the
     * coordinator; test mixed valid/bad files, ZIP, empty input and case variants.
     */
    public ImportResult importPaths(List<Path> dropped) {
        throw new UnsupportedOperationException("TODO Phase 3: import pipeline");
    }

    /**
     * Verify JPG/PNG content can really be decoded and fits a reasonable size cap.
     * TODO Phase 3: use ImageIO readers, compare decoded format to expected type,
     * inspect dimensions before loading huge pixels, and wrap decode errors in a
     * file-specific INPUT ConversionException. Never trust a renamed bad file.
     * Worker thread only. Test corrupt JPG/PNG and oversized/unsupported content.
     */
    public static void validateImage(Path path) throws ConversionException {
        throw new UnsupportedOperationException("TODO Phase 3: image decode validation");
    }
}
