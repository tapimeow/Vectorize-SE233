// Team ownership: Day (Backend/Logic). See README.md "Team split".
package se233.vectorize;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import se233.vectorize.adapter.AutoTraceVectorEngine;
import se233.vectorize.exception.ConversionException;
import se233.vectorize.model.ColorMode;
import se233.vectorize.model.DetailLevel;
import se233.vectorize.model.VectorizationSettings;
import se233.vectorize.service.RasterPreprocessor;
import se233.vectorize.service.SvgValidationService;
import se233.vectorize.service.VectorizationService;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class VectorPipelineTest {
    @TempDir Path temp;

    @Test void customPaletteIsBoundedWhileUnlimitedRetainsMoreColors() {
        BufferedImage image = bands(); RasterPreprocessor processor = new RasterPreprocessor();
        var two = processor.prepare(image, VectorizationSettings.defaults());
        var five = processor.prepare(image, VectorizationSettings.defaults().withCustomColors(5));
        var unlimited = processor.prepare(image, VectorizationSettings.defaults().withColorMode(ColorMode.UNLIMITED));
        assertTrue(unique(two.image()).size() <= 2);
        assertTrue(unique(five.image()).size() > unique(two.image()).size());
        assertTrue(unique(unlimited.image()).size() >= unique(five.image()).size());
        assertFalse(two.detectedColors().isEmpty());
    }

    @Test void realTracerProducesPathsForJpgAndTransparentPng() throws Exception {
        Assumptions.assumeTrue(Files.isExecutable(Path.of("/opt/homebrew/bin/autotrace")) || Files.isExecutable(Path.of("/usr/local/bin/autotrace")));
        BufferedImage image = new BufferedImage(120, 80, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics(); graphics.setColor(Color.WHITE); graphics.fillRect(0, 0, 120, 80);
        graphics.setColor(Color.RED); graphics.fillRect(20, 20, 70, 40); graphics.dispose();
        Path jpg = temp.resolve("shape.jpg"), png = temp.resolve("shape.png");
        ImageIO.write(image, "jpg", jpg.toFile()); ImageIO.write(image, "png", png.toFile());
        VectorizationService pipeline = new VectorizationService(new AutoTraceVectorEngine());
        String jpgSvg = pipeline.convert(jpg, VectorizationSettings.defaults(), () -> false, stage -> {}).svg();
        String pngSvg = pipeline.convert(png, VectorizationSettings.defaults().withRemoveBackground(true), () -> false, stage -> {}).svg();
        assertTrue(jpgSvg.contains("<path")); assertFalse(jpgSvg.contains("<image"));
        assertTrue(pngSvg.contains("<path")); assertFalse(pngSvg.contains("<image"));
        assertFalse(pngSvg.contains("fill:#ffffff"), "Removed white background must not remain an opaque path");
        BufferedImage alpha = new BufferedImage(120, 80, BufferedImage.TYPE_INT_ARGB);
        var alphaGraphics = alpha.createGraphics(); alphaGraphics.setColor(Color.RED);
        alphaGraphics.fillRect(20, 20, 70, 40); alphaGraphics.dispose();
        Path transparent = temp.resolve("alpha.png"); ImageIO.write(alpha, "png", transparent.toFile());
        String alphaSvg = pipeline.convert(transparent, VectorizationSettings.defaults(), () -> false, stage -> {}).svg();
        assertTrue(alphaSvg.contains("<path")); assertFalse(alphaSvg.contains("<image"));
    }

    @Test void unlimitedModeCanTraceAContinuousGradient() throws Exception {
        Assumptions.assumeTrue(Files.isExecutable(Path.of("/opt/homebrew/bin/autotrace")) || Files.isExecutable(Path.of("/usr/local/bin/autotrace")));
        BufferedImage gradient = new BufferedImage(180, 120, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 120; y++) for (int x = 0; x < 180; x++)
            gradient.setRGB(x, y, ((x * 255 / 179) << 16) | (y * 255 / 119));
        Path file = temp.resolve("gradient.png"); ImageIO.write(gradient, "png", file.toFile());
        String svg = new VectorizationService(new AutoTraceVectorEngine()).convert(file,
                VectorizationSettings.defaults().withColorMode(ColorMode.UNLIMITED), () -> false, stage -> {}).svg();
        assertTrue(svg.contains("<path"));
    }

    @Test void detailLevelsChangeActualSvgDimensions() throws Exception {
        Assumptions.assumeTrue(Files.isExecutable(Path.of("/opt/homebrew/bin/autotrace")) || Files.isExecutable(Path.of("/usr/local/bin/autotrace")));
        BufferedImage image = new BufferedImage(1000, 600, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics(); graphics.setColor(Color.WHITE); graphics.fillRect(0, 0, 1000, 600);
        graphics.setColor(Color.RED); graphics.fillRect(100, 100, 800, 400); graphics.dispose();
        Path file = temp.resolve("large.png"); ImageIO.write(image, "png", file.toFile());
        VectorizationService pipeline = new VectorizationService(new AutoTraceVectorEngine());
        String low = pipeline.convert(file, VectorizationSettings.defaults().withDetail(DetailLevel.LOW), () -> false, stage -> {}).svg();
        String medium = pipeline.convert(file, VectorizationSettings.defaults(), () -> false, stage -> {}).svg();
        String high = pipeline.convert(file, VectorizationSettings.defaults().withDetail(DetailLevel.HIGH), () -> false, stage -> {}).svg();
        assertTrue(low.contains("width=\"360\""));
        assertTrue(medium.contains("width=\"640\""));
        assertTrue(high.contains("width=\"960\""));
    }

    @Test void missingExecutableIsAUserSafeEngineFailure() throws Exception {
        Path png = temp.resolve("sample.png"); ImageIO.write(bands(), "png", png.toFile());
        String previous = System.getProperty("vectorize.autotrace");
        try {
            System.setProperty("vectorize.autotrace", "/definitely/missing/autotrace");
            ConversionException error = assertThrows(ConversionException.class, () ->
                new VectorizationService(new AutoTraceVectorEngine()).convert(png, VectorizationSettings.defaults(), () -> false, stage -> {}));
            assertEquals(ConversionException.Kind.ENGINE, error.kind());
            assertTrue(error.getMessage().contains("AutoTrace"));
        } finally {
            if (previous == null) System.clearProperty("vectorize.autotrace");
            else System.setProperty("vectorize.autotrace", previous);
        }
    }

    @Test void invalidRasterWrappedSvgIsRejected() {
        SvgValidationService validator = new SvgValidationService();
        assertThrows(ConversionException.class, () -> validator.validate("<svg xmlns='http://www.w3.org/2000/svg'><image href='data:image/png;base64,AAAA'/></svg>"));
        assertThrows(ConversionException.class, () -> validator.validate("<svg xmlns='http://www.w3.org/2000/svg'></svg>"));
        assertThrows(ConversionException.class, () -> validator.validate("<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10' onload='alert(1)'><path d='M0 0L5 5Z'/></svg>"));
    }

    private static BufferedImage bands() {
        BufferedImage image = new BufferedImage(100, 50, BufferedImage.TYPE_INT_RGB);
        int[] colors = {Color.RED.getRGB(), Color.BLUE.getRGB(), Color.GREEN.getRGB(), Color.YELLOW.getRGB(), Color.BLACK.getRGB()};
        for (int y = 0; y < 50; y++) for (int x = 0; x < 100; x++) image.setRGB(x, y, colors[x / 20]);
        return image;
    }
    private static HashSet<Integer> unique(BufferedImage image) {
        HashSet<Integer> values = new HashSet<>();
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) values.add(image.getRGB(x, y) & 0xffffff);
        return values;
    }
}
