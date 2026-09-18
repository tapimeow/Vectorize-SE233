package se233.vectorize.service;

import java.awt.image.BufferedImage;

/** Phase 4 background-mask service; FR-011, transparent-SVG rubric. */
public final class BackgroundRemovalService {
    /**
     * Return a row-major mask of pixels that should NOT become SVG foreground.
     * TODO Phase 4: always account for source PNG alpha. When removal is selected,
     * investigate a likely border/corner background color and grow only connected
     * matching regions; avoid deleting an interior foreground of the same color.
     * Decide how to report a busy/photo border that cannot be identified reliably.
     * Compute the mask before color quantization so Custom mode does not merge
     * foreground into background. Run on a worker; test transparent PNG, opaque
     * flat backgrounds, foreground touching edges, and textured photographs.
     * The final SVG must omit background geometry rather than paint it white.
     */
    public boolean[] mask(BufferedImage image, boolean removeBackground) {
        throw new UnsupportedOperationException("TODO Phase 4: background identification");
    }
}
