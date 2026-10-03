// Team ownership: Day (Backend/Logic). See README.md "Team split".
package se233.vectorize.service;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

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
            List<Integer> result = new ArrayList<>();
            if (bins.isEmpty()) return result;          // nothing but background

            // Start with one box holding every bin.
            List<List<ColorBin>> boxes = new ArrayList<>();
            boxes.add(new ArrayList<>(bins));

            // Keep splitting the heaviest box until we have `count` boxes.
            while (boxes.size() < count) {
                List<ColorBin> heaviest = null;
                for (List<ColorBin> box : boxes) {
                    if (box.size() < 2) continue;       // one bin can't be split
                    if (heaviest == null || totalWeight(box) > totalWeight(heaviest)) {
                        heaviest = box;
                    }
                }
                if (heaviest == null) break;            // fewer real colors than asked for

                // Sort along the channel (0=red, 1=green, 2=blue) where the box is widest.
                int channel = widestChannel(heaviest);
                heaviest.sort((a, b) -> {
                    int diff = channelValue(a.rgb(), channel) - channelValue(b.rgb(), channel);
                    if (diff != 0) return diff;
                    return Integer.compare(a.rgb(), b.rgb());
                });

                // Cut at the weighted median: first half holds about half the pixels.
                int half = totalWeight(heaviest) / 2;
                int sum = 0;
                int cut = 1;
                for (int i = 0; i < heaviest.size() - 1; i++) {
                    sum += heaviest.get(i).weight();
                    cut = i + 1;
                    if (sum >= half) break;
                }

                boxes.remove(heaviest);
                boxes.add(new ArrayList<>(heaviest.subList(0, cut)));
                boxes.add(new ArrayList<>(heaviest.subList(cut, heaviest.size())));
            }

            // Biggest box first so palette order is stable.
            boxes.sort((a, b) -> totalWeight(b) - totalWeight(a));

            // Each box becomes one color: the weighted average of its bins.
            for (List<ColorBin> box : boxes) {
                long r = 0, g = 0, b = 0;
                long w = totalWeight(box);
                for (ColorBin bin : box) {
                    Color c = new Color(bin.rgb());
                    r += (long) c.getRed() * bin.weight();
                    g += (long) c.getGreen() * bin.weight();
                    b += (long) c.getBlue() * bin.weight();
                }
                result.add(new Color((int) (r / w), (int) (g / w), (int) (b / w)).getRGB());
            }
            return result;
        }

        private static int totalWeight(List<ColorBin> box) {
            int sum = 0;
            for (ColorBin bin : box) sum += bin.weight();
            return sum;
        }

        private static int channelValue(int rgb, int channel) {
            Color c = new Color(rgb);
            if (channel == 0) return c.getRed();
            if (channel == 1) return c.getGreen();
            return c.getBlue();
        }

        private static int widestChannel(List<ColorBin> box) {
            int bestChannel = 0;
            int bestRange = -1;
            for (int channel = 0; channel < 3; channel++) {
                int min = 255, max = 0;
                for (ColorBin bin : box) {
                    int v = channelValue(bin.rgb(), channel);
                    min = Math.min(min, v);
                    max = Math.max(max, v);
                }
                if (max - min > bestRange) {
                    bestRange = max - min;
                    bestChannel = channel;
                }
            }
            return bestChannel;
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
        int width = image.getWidth();
        int height = image.getHeight();
        Map<Integer, Integer> counts = new HashMap<>();
        int total = 0;

        // Big images: only look at every step-th pixel (~250,000 samples max).
        int step = Math.max(1, (int) Math.sqrt((double) width * height / 250_000));

        for (int y = 0; y < height; y += step) {
            for (int x = 0; x < width; x += step) {
                int i = y * width + x;

                if (excluded[i]) continue;

                Color c = new Color(image.getRGB(x, y));
                int r = c.getRed() / 16 * 16 + 8;
                int g = c.getGreen() / 16 * 16 + 8;
                int b = c.getBlue() / 16 * 16 + 8;
                int key = new Color(r, g, b).getRGB();

                counts.put(key, counts.getOrDefault(key, 0) + 1);
                total++;
            }
        }
        List<ColorBin> bins = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            bins.add(new ColorBin(entry.getKey(), entry.getValue()));
        }

        // Biggest weight first; equal weights sorted by rgb so results are repeatable.
        bins.sort((a, b) -> {
            if (a.weight() != b.weight()) return b.weight() - a.weight();
            return Integer.compare(a.rgb(), b.rgb());
        });

        // Detected colors: bins with at least 1% of the kept pixels (ignores JPEG noise).
        List<Integer> detectedColors = new ArrayList<>();
        for (ColorBin bin : bins) {
            if (detectedColors.size() == 10) break;
            if (bin.weight() * 100 < total) break;
            detectedColors.add(bin.rgb());
        }

        return new Analysis(bins, detectedColors);
    }
}
