package se233.vectorize;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import se233.vectorize.adapter.AutoTraceVectorEngine;
import se233.vectorize.exception.ConversionException;
import se233.vectorize.model.VectorizationSettings;
import se233.vectorize.service.ExportService;
import se233.vectorize.service.SvgValidationService;
import se233.vectorize.service.VectorizationService;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ExportIntegrationTest {
    @TempDir Path temp;

    @Test void realJpgAndPngExportsRenderInExternalSvgViewer() throws Exception {
        Assumptions.assumeTrue(Files.isExecutable(Path.of("/opt/homebrew/bin/autotrace")) && Files.isExecutable(Path.of("/opt/homebrew/bin/magick")));
        BufferedImage image = new BufferedImage(120, 80, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics(); graphics.setColor(Color.WHITE); graphics.fillRect(0, 0, 120, 80);
        graphics.setColor(Color.RED); graphics.fillRect(20, 20, 70, 40); graphics.dispose();
        Path jpg = temp.resolve("picture.jpg"), png = temp.resolve("picture.png");
        ImageIO.write(image, "jpg", jpg.toFile()); ImageIO.write(image, "png", png.toFile());
        VectorizationService pipeline = new VectorizationService(new AutoTraceVectorEngine());
        ExportService export = new ExportService(pipeline);
        Path jpgSvg = export.exportOne(new ExportService.ExportItem(jpg, "picture.jpg", VectorizationSettings.defaults(), null, null, null), temp,
                () -> false, stage -> {}).savedPath();
        Path pngSvg = export.exportOne(new ExportService.ExportItem(png, "picture.png",
                VectorizationSettings.defaults().withRemoveBackground(true), null, null, null), temp,
                () -> false, stage -> {}).savedPath();
        assertNotEquals(jpgSvg, pngSvg);
        SvgValidationService validator = new SvgValidationService();
        for (Path svg : new Path[]{jpgSvg, pngSvg}) {
            validator.validate(Files.readString(svg));
            Path rendered = temp.resolve(svg.getFileName().toString() + ".png");
            Process process = new ProcessBuilder("/opt/homebrew/bin/magick", svg.toString(), rendered.toString()).start();
            assertTrue(process.waitFor(10, TimeUnit.SECONDS));
            assertEquals(0, process.exitValue(), "External SVG renderer must open exported file");
            assertNotNull(ImageIO.read(rendered.toFile()));
        }
    }

    @Test void inaccessibleOutputFolderFailsPerFile() throws Exception {
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><path d='M0 0L5 0L5 5Z'/></svg>";
        ExportService export = new ExportService(new VectorizationService((prepared, settings, ignored, cancelled) -> svg));
        var item = new ExportService.ExportItem(temp.resolve("source.png"), "source.png", VectorizationSettings.defaults(), svg, null, null);
        ConversionException error = assertThrows(ConversionException.class,
                () -> export.exportOne(item, temp.resolve("missing"), () -> false, stage -> {}));
        assertEquals(ConversionException.Kind.OUTPUT, error.kind());
    }
}
