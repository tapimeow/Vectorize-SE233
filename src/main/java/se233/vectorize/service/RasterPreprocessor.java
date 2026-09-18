package se233.vectorize.service;

import se233.vectorize.model.VectorizationSettings;
import java.awt.image.BufferedImage;
import java.util.List;

/** Phase 4 raster preparation before a replaceable tracing engine. */
public final class RasterPreprocessor {
    public record PreparedRaster(BufferedImage image, String ignoredColor, List<Integer> detectedColors) { }
    private final ColorAnalysisService colors = new ColorAnalysisService();
    private final BackgroundRemovalService background = new BackgroundRemovalService();

    /**
     * Prepare an engine-readable raster with the selected detail/color/background.
     * TODO Phase 4: scale/denoise according to DetailLevel; ask background for a
     * mask; ask colors for detected swatches; quantize foreground only in Custom
     * mode (2–5 colors), leaving Unlimited unconstrained. Represent masked pixels
     * in a way the chosen engine can omit, without colliding with foreground colors.
     * Return both prepared pixels and swatches for the UI. Keep work off FX; flush
     * temporary images and bound memory. Test all detail/mode combinations, alpha,
     * 2 and 5 colors, and an all-background image. FR-007–FR-011, FR-013.
     */
    public PreparedRaster prepare(BufferedImage source, VectorizationSettings settings) {
        throw new UnsupportedOperationException("TODO Phase 4: raster preprocessing");
    }
}
