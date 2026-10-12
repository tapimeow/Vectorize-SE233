// Team ownership: Day (Backend/Logic). See README.md "Team split".
package se233.vectorize.service;

import se233.vectorize.exception.ConversionException;
import se233.vectorize.model.ImageDocument;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static se233.vectorize.exception.ConversionException.Kind.INPUT;

/** Phase 3 boundary from dropped paths to ordered image documents. */
public final class ImageImportService {
    public record ImportResult(List<ImageDocument> documents, List<String> errors) {

    }

    private final ZipImportService zip;
    public ImageImportService(ZipImportService zip) {
        this.zip = zip;
    }

    /** TODO Phase 3: classify case-insensitive JPG/JPEG/PNG filenames for direct
     * and ZIP import. A matching suffix alone does not validate image contents. */
    public static boolean isSupportedImage(String name) {
        return name.toLowerCase().endsWith(".jpg") ||
               name.toLowerCase().endsWith(".jpeg") ||
               name.toLowerCase().endsWith(".png");
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
        List<ImageDocument> documents = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (Path path : dropped) {
            boolean isZip = path.toString().toLowerCase().endsWith(".zip");
            try {
                if(isZip) {
                    List<Path> extracted = zip.extract(path);
                    for (Path imagePath : extracted) {
                        // One corrupt member must not erase its valid siblings.
                        try {
                            validateImage(imagePath);
                            documents.add(new ImageDocument(imagePath,
                                    imagePath.getFileName().toString()));
                        } catch (ConversionException e) {
                            errors.add(path.getFileName() + " → " + e.getMessage());
                        }
                    }
                } else if (isSupportedImage(path.toString())) {
                    validateImage(path);
                    documents.add(new ImageDocument(path, path.getFileName().toString()));
                } else {
                    errors.add(path.getFileName() + ": unsupported file type");
                }
            } catch (ConversionException | IOException e) {
                errors.add(path.getFileName() + ": " + e.getMessage());
            }
        }
        return new ImportResult(documents, errors);
    }

    /**
     * Verify JPG/PNG content can really be decoded and fits a reasonable size cap.
     * TODO Phase 3: use ImageIO readers, compare decoded format to expected type,
     * inspect dimensions before loading huge pixels, and wrap decode errors in a
     * file-specific INPUT ConversionException. Never trust a renamed bad file.
     * Worker thread only. Test corrupt JPG/PNG and oversized/unsupported content.
     */
    public static void validateImage(Path path) throws ConversionException {
        try {
            BufferedImage image = ImageIO.read(path.toFile());

            if (image == null) {
                throw new ConversionException(INPUT, "Unsupported image format: " + path.getFileName());
            }

            if (image.getWidth() > 10000 || image.getHeight() > 10000) {
                throw new ConversionException(INPUT, "Image too large: " + path.getFileName());
            }

        } catch (IOException e) {
            throw new ConversionException(INPUT, "Failed to read image file: " + path.getFileName(), e);
        }
    }
}
