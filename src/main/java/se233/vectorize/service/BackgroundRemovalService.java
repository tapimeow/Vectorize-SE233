// Team ownership: Day (Backend/Logic). See README.md "Team split".
package se233.vectorize.service;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

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
        int width = image.getWidth();
        int height = image.getHeight();
        boolean[] excluded = new boolean[width * height];

        if (image.getColorModel().hasAlpha()) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++){
                    int argb = image.getRGB(x, y);
                    int alpha = new Color(argb, true).getAlpha();
                    if (alpha == 0) {
                        excluded[y * width + x] = true;
                    } 
                }
            }
        }
        if (!removeBackground) {
            return excluded;
        }

        Map<Integer, Integer> counts = new HashMap<>();
        Map<Integer, Integer> exampleColors = new HashMap<>();
        int total = 0;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                boolean onEdge = (x == 0) || (y == 0) || (x == width - 1) || (y == height - 1);
                if (!onEdge || excluded[y * width + x]) continue;
                Color c = new Color(image.getRGB(x, y));
                int group = (c.getRed() / 16) * 10000 + (c.getGreen() / 16) * 100 + (c.getBlue() / 16);
                counts.put(group, counts.getOrDefault(group, 0) + 1);
                exampleColors.putIfAbsent(group, image.getRGB(x, y));
                total++;
            }
        }

        int bestGroup = 0;
        int bestCount = 0;
        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > bestCount) {
                bestCount = entry.getValue();
                bestGroup = entry.getKey();
            }
        }

        if (total == 0 || bestCount < total * 0.6){
            return excluded;
        }
        int backgroundColor = exampleColors.get(bestGroup);

        ArrayDeque<Integer> queue = new ArrayDeque<>();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++){
                boolean onEdge = (x == 0) || (y == 0) || (x == width - 1) || (y == height - 1);
                int i = y * width + x;
                if (onEdge && !excluded[i] && isSimilar(image.getRGB(x, y), backgroundColor)) {
                    excluded[i] = true;
                    queue.add(i);
                }
            }
        }

        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};

        while (!queue.isEmpty()) {
            int i = queue.poll();
            int x = i % width;
            int y = i / width;

            for (int k = 0; k < 4; k++) {
                int nx = x + dx[k];
                int ny = y + dy[k];

                if (nx < 0 || nx >= width || ny < 0 || ny >= height) continue;

                int n = ny * width + nx;

                if (!excluded[n] && isSimilar(image.getRGB(nx, ny), backgroundColor)){
                    excluded[n] = true;
                    queue.add(n);
                }
            }
        }


        return excluded;
    }

    private boolean isSimilar(int rgb1, int rgb2) {
        Color a = new Color(rgb1);
        Color b = new Color(rgb2);
        return Math.abs(a.getRed() - b.getRed()) <= 24
                && Math.abs(a.getGreen() - b.getGreen()) <= 24
                && Math.abs(a.getBlue() - b.getBlue()) <= 24;
    }
}
