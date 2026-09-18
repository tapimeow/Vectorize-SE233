// Team ownership: Day (Backend/Logic). See README.md "Team split".
package se233.vectorize;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import se233.vectorize.adapter.VectorEngine;
import se233.vectorize.model.VectorizationSettings;
import se233.vectorize.service.ExportService;
import se233.vectorize.service.PresetService;
import se233.vectorize.service.VectorizationService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class ExportAndPresetTest {
    @TempDir Path temp;
    private static final String SVG = "<svg xmlns='http://www.w3.org/2000/svg' width='20' height='20'><path d='M0 0L10 0L10 10Z'/></svg>";

    @Test void exportDoesNotOverwriteAndProducesOpenableSvg() throws Exception {
        VectorEngine fake = (prepared, settings, ignored, cancelled) -> SVG;
        ExportService export = new ExportService(new VectorizationService(fake));
        Path source = temp.resolve("sample.png"); Files.write(source, new byte[]{1});
        var item = new ExportService.ExportItem(source, "sample.png", VectorizationSettings.defaults(), SVG,
                VectorizationService.sourceVersion(source), VectorizationService.PIPELINE_VERSION);
        Path first = export.exportOne(item, temp, () -> false, stage -> {}).savedPath();
        Path second = export.exportOne(item, temp, () -> false, stage -> {}).savedPath();
        assertNotEquals(first, second);
        assertEquals(SVG, Files.readString(first));
        assertTrue(first.getFileName().toString().endsWith(".svg"));
    }

    @Test void presetRoundTripAndInvalidCountIsRejected() throws Exception {
        PresetService service = new PresetService();
        Path file = temp.resolve("named.properties");
        var settings = VectorizationSettings.defaults().withCustomColors(4).withRemoveBackground(true);
        service.save(file, settings); assertEquals(settings, service.load(file));
        Files.writeString(file, "detail=MEDIUM\ncolorMode=CUSTOM\ncustomColors=9\nremoveBackground=false\n");
        assertThrows(java.io.IOException.class, () -> service.load(file));
        Files.writeString(file, "detail=MEDIUM\ncolorMode=CUSTOM\ncustomColors=2\nremoveBackground=maybe\n");
        assertThrows(java.io.IOException.class, () -> service.load(file));
    }

    @Test void changedSourceInvalidatesCachedSvgBeforeExport() throws Exception {
        Path source = temp.resolve("changed.png");
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(image, "png", source.toFile());
        String oldVersion = VectorizationService.sourceVersion(source);
        Files.setLastModifiedTime(source, FileTime.fromMillis(System.currentTimeMillis() + 5000));
        AtomicInteger traces = new AtomicInteger();
        VectorEngine fake = (prepared, settings, ignored, cancelled) -> { traces.incrementAndGet(); return SVG; };
        ExportService export = new ExportService(new VectorizationService(fake));
        var item = new ExportService.ExportItem(source, "changed.png", VectorizationSettings.defaults(),
                "<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><path d='M0 0L1 0Z'/></svg>",
                oldVersion, VectorizationService.PIPELINE_VERSION);
        Path result = export.exportOne(item, temp, () -> false, stage -> {}).savedPath();
        assertEquals(1, traces.get()); assertEquals(SVG, Files.readString(result));
    }
}
