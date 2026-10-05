// Team ownership: Day (Backend/Logic). See README.md "Team split".
package se233.vectorize.service;

import se233.vectorize.exception.ConversionException;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.ArrayList;
import java.util.List;

import static se233.vectorize.exception.ConversionException.Kind.ZIP;

/** Phase 3 ZIP quarantine/cleanup; FR-002 and NFR-003–NFR-004. */
public final class ZipImportService implements AutoCloseable {
    private final List<Path> temporaryRoots = new ArrayList<>();

    /**
     * Extract only supported images and return their managed temporary paths.
     * TODO Phase 3: open/validate the archive; iterate entries; skip directories
     * and unsupported files; normalize every entry path and reject absolute,
     * drive-prefixed, traversal or otherwise escaping names (Zip Slip) — this
     * check is not optional, a traversal entry can write outside the intended
     * folder. Verify integrity, use noncolliding names, clean a partial
     * extraction on failure, and report corrupt/empty ZIP clearly. Do not copy
     * arbitrary archive trees. ImageImportService subsequently decodes each
     * result. Worker thread only; test traversal, malformed archive, empty ZIP,
     * and duplicate names.
     */
    public List<Path> extract(Path archive) throws ConversionException, IOException {
        Path tempRoot = Files.createTempDirectory("vectorize-zip");
        temporaryRoots.add(tempRoot);

        List<Path> results = new ArrayList<>();

        try (ZipFile zipFile = new ZipFile(archive.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            int index = 0;

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                if (entry.isDirectory()) {
                    continue;
                }
                if (!ImageImportService.isSupportedImage(entry.getName())) {
                    continue;
                }

                // Zip Slip: check the raw entry name, before it is flattened to a file name.
                String name = entry.getName().replace('\\', '/');
                Path resolved = tempRoot.resolve(name).normalize();
                if (name.startsWith("/") || name.matches("^[A-Za-z]:.*")
                        || !resolved.startsWith(tempRoot.normalize())) {
                    throw new ConversionException(ZIP, "Unsafe entry path in " + archive.getFileName() + ": "
                    + entry.getName());
                }
                Path safeFileName = Paths.get(name).getFileName();
                Path destination = tempRoot.resolve(index + "_" + safeFileName);

                try (var in = zipFile.getInputStream(entry)) {
                    Files.copy(in, destination);
                }

                results.add(destination);
                index++;
            }
        } catch (IOException e) {
            deleteAll(results);
            throw new ConversionException(ZIP, "Failed to extract ZIP: " + e.getMessage(), e);
        } catch (ConversionException e) {
            deleteAll(results);                      // no partial extraction left behind
            throw e;
        }

        if (results.isEmpty()) {
            throw new ConversionException(ZIP, "No supported images found in " + archive.getFileName());
        }

        return results;
    }

    private void deleteAll(List<Path> files) {
        for (Path file : files) {
            try { Files.deleteIfExists(file); } catch (IOException ignored) { }
        }
    }

    /** Phase 3/8: release only extraction roots owned by this service after
     * workers stop; test that temporary images survive until processing ends. */
    @Override public void close() {
        // TODO Phase 3: no temporary roots exist until extraction is implemented.
        // Once it is, delete only roots owned by this service after workers stop.
        for (Path root : temporaryRoots) {
            try {
                Files.walkFileTree(root, new SimpleFileVisitor<Path>(){
                    public FileVisitResult visitFile(Path f, BasicFileAttributes a) throws IOException {
                        Files.delete(f);
                        return FileVisitResult.CONTINUE;
                    }
                    public FileVisitResult postVisitDirectory(Path d, IOException e) throws IOException {
                        Files.delete(d);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                System.err.println("Cleanup failed for " + root + ": " + e.getMessage());
            }
        }
        temporaryRoots.clear();
    }
}
