// Team ownership: Day (Backend/Logic). See README.md "Team split".
package se233.vectorize.service;

import se233.vectorize.model.ColorMode;
import se233.vectorize.model.VectorizationSettings;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Phase 4 raster preparation before a replaceable tracing engine. */
public final class RasterPreprocessor {
    public record PreparedRaster(BufferedImage image, String ignoredColor, List<Integer> detectedColors) { }
    private final ColorAnalysisService colors = new ColorAnalysisService();
    private final BackgroundRemovalService background = new BackgroundRemovalService();

    /**
     * Prepare an engine-readable raster with the selected detail/color/background.
     * Steps: scale to the detail level's size cap (smoothing also softens noise),
     * mask background, analyze colors, snap foreground to the palette only in
     * Custom mode, then paint masked pixels one unused color the engine can skip.
     * FR-007–FR-011, FR-013. Worker thread only.
     */
    public PreparedRaster prepare(BufferedImage source, VectorizationSettings settings) {
        BufferedImage scaled = scale(source, settings.detail().maxDimension());
        int width = scaled.getWidth();
        int height = scaled.getHeight();

        // Mask first, so quantization never merges foreground into background.
        boolean[] excluded = background.mask(scaled, settings.removeBackground());
        ColorAnalysisService.Analysis analysis = colors.analyze(scaled, excluded);

        // Only Custom mode limits colors; Unlimited keeps the pixels as they are.
        List<Integer> palette = null;
        if (settings.colorMode() == ColorMode.CUSTOM) {
            palette = analysis.palette(settings.customColors());
        }

        BufferedImage prepared = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Set<Integer> usedColors = new HashSet<>();
        boolean anyExcluded = false;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (excluded[y * width + x]) {
                    anyExcluded = true;
                    continue;                       // painted after we pick a free color
                }
                int rgb = scaled.getRGB(x, y);
                if (palette != null && !palette.isEmpty()) {
                    rgb = nearest(rgb, palette);
                }
                prepared.setRGB(x, y, rgb);
                usedColors.add(new Color(rgb).getRGB());
            }
        }

        // Background gets a color no foreground pixel uses, and we tell the engine to skip it.
        String ignoredColor = null;
        if (anyExcluded) {
            Color free = unusedColor(usedColors);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (excluded[y * width + x]) prepared.setRGB(x, y, free.getRGB());
                }
            }
            ignoredColor = String.format("%02X%02X%02X", free.getRed(), free.getGreen(), free.getBlue());
        }

        if (scaled != source) scaled.flush();     // free the temporary copy
        return new PreparedRaster(prepared, ignoredColor, analysis.detectedColors());
    }

    /** Shrink so the longest side is at most maxDimension; small images are left alone. */
    private BufferedImage scale(BufferedImage source, int maxDimension) {
        int width = source.getWidth();
        int height = source.getHeight();
        int longest = Math.max(width, height);
        if (longest <= maxDimension) return source;

        double factor = (double) maxDimension / longest;
        int newWidth = Math.max(1, (int) Math.round(width * factor));
        int newHeight = Math.max(1, (int) Math.round(height * factor));

        // ARGB so transparent PNG pixels stay transparent for the background mask.
        BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, newWidth, newHeight, null);
        g.dispose();
        return scaled;
    }

    /** The palette color closest to rgb (straight-line distance in RGB). */
    private int nearest(int rgb, List<Integer> palette) {
        Color c = new Color(rgb);
        int best = palette.get(0);
        int bestDistance = Integer.MAX_VALUE;
        for (int candidate : palette) {
            Color p = new Color(candidate);
            int dr = c.getRed() - p.getRed();
            int dg = c.getGreen() - p.getGreen();
            int db = c.getBlue() - p.getBlue();
            int distance = dr * dr + dg * dg + db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    /** Magenta-ish colors are rare in real images; take the first one nobody uses. */
    private Color unusedColor(Set<Integer> usedColors) {
        for (int g = 0; g < 256; g++) {
            Color candidate = new Color(255, g, 255);
            if (!usedColors.contains(candidate.getRGB())) return candidate;
        }
        for (int r = 0; r < 256; r++) {
            for (int b = 0; b < 256; b++) {
                Color candidate = new Color(r, 0, b);
                if (!usedColors.contains(candidate.getRGB())) return candidate;
            }
        }
        return new Color(255, 0, 255);           // practically unreachable
    }
}
