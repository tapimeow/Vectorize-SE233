package se233.vectorize.service;

import java.awt.image.BufferedImage;
import java.util.List;

/** Phase 4 color analysis boundary (FR-008–FR-010; Custom-colors rubric). */
public final class ColorAnalysisService {
    public record ColorBin(int rgb, int weight) { }
    public record Analysis(List<ColorBin> bins, List<Integer> detectedColors) {
        /**
         * Produce at most {@code count} representative foreground colors.
         * TODO Phase 4: implement a deterministic Median Cut palette over weighted
         * bins: choose splittable color regions, partition them, then represent each
         * region. Decide how to handle fewer meaningful colors and empty images.
         * The UI defaults to 2 and never requests more than 5. Compare the result
         * against flat art and gradients; do not equate JPEG noise with meaningful
         * colors. Pure computation: call on a worker, never inside a JavaFX handler.
         */
        public List<Integer> palette(int count) {
            throw new UnsupportedOperationException("TODO Phase 4: Median Cut palette");
        }
    }

    /**
     * Analyze decoded source pixels while omitting alpha/background pixels.
     * TODO Phase 4: bound sampling cost for large images, gather a weighted color
     * summary, and return detected swatches plus data for 2–5-color quantization.
     * Consider zero foreground pixels, photographs, and repeatable results. Do not
     * impose Custom quantization in Unlimited mode. Test count bounds and detected
     * palette relevance using images with known color bands. Worker thread only.
     */
    public Analysis analyze(BufferedImage image, boolean[] excluded) {
        throw new UnsupportedOperationException("TODO Phase 4: color detection");
    }
}
