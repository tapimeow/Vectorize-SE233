// Team ownership: Day (Backend/Logic). See README.md "Team split".
package se233.vectorize;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import se233.vectorize.exception.ConversionException;
import se233.vectorize.service.ImageImportService;
import se233.vectorize.service.ZipImportService;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ImportAndZipTest {
    @TempDir Path temp;

    @Test void directAndZipInputsKeepValidImagesWhenOneIsCorrupt() throws Exception {
        Path png = image(temp.resolve("valid.png"));
        Path corrupt = temp.resolve("broken.jpg"); Files.writeString(corrupt, "not a jpg");
        Path zip = temp.resolve("set.zip");
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(zip))) {
            output.putNextEntry(new ZipEntry("nested/image.png")); output.write(Files.readAllBytes(png)); output.closeEntry();
            output.putNextEntry(new ZipEntry("notes.txt")); output.write("ignore".getBytes()); output.closeEntry();
        }
        try (ZipImportService zipImport = new ZipImportService()) {
            ImageImportService.ImportResult result = new ImageImportService(zipImport).importPaths(List.of(png, corrupt, zip));
            assertEquals(2, result.documents().size());
            assertEquals(1, result.errors().size());
        }
    }

    @Test void traversalAndEmptyZipAreRejected() throws Exception {
        Path evil = temp.resolve("evil.zip");
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(evil))) {
            output.putNextEntry(new ZipEntry("../outside.png")); output.write(new byte[]{1, 2}); output.closeEntry();
        }
        Path empty = temp.resolve("empty.zip");
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(empty))) {
            output.putNextEntry(new ZipEntry("readme.txt")); output.write(new byte[]{1}); output.closeEntry();
        }
        try (ZipImportService service = new ZipImportService()) {
            assertEquals(ConversionException.Kind.ZIP, assertThrows(ConversionException.class, () -> service.extract(evil)).kind());
            assertThrows(ConversionException.class, () -> service.extract(empty));
        }
        assertFalse(Files.exists(temp.resolve("outside.png")));
    }

    @Test void corruptZipIsRejectedWithoutKillingImport() throws Exception {
        Path corrupt = temp.resolve("corrupt.zip"); Files.writeString(corrupt, "not a ZIP");
        try (ZipImportService service = new ZipImportService()) {
            var result = new ImageImportService(service).importPaths(List.of(corrupt, image(temp.resolve("good.png"))));
            assertEquals(1, result.documents().size()); assertEquals(1, result.errors().size());
        }
    }

    private static Path image(Path file) throws Exception {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(Color.WHITE); graphics.fillRect(0, 0, 32, 32);
        graphics.setColor(Color.RED); graphics.fillRect(8, 8, 16, 16);
        graphics.dispose(); ImageIO.write(image, "png", file.toFile()); return file;
    }
}
