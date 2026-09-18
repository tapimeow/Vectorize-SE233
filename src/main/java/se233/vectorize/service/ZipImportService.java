package se233.vectorize.service;

import se233.vectorize.exception.ConversionException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Phase 3 ZIP quarantine/cleanup; FR-002 and NFR-003–NFR-004. */
public final class ZipImportService implements AutoCloseable {
    public static final int MAX_ENTRIES = 500;
    public static final long MAX_ENTRY_BYTES = 40L * 1024 * 1024;
    public static final long MAX_TOTAL_BYTES = 200L * 1024 * 1024;
    private final List<Path> temporaryRoots = new ArrayList<>();

    /**
     * Extract only supported images and return their managed temporary paths.
     * TODO Phase 3: open/validate the archive; iterate entries; skip directories
     * and unsupported files; normalize every entry path and reject absolute,
     * drive-prefixed, traversal or otherwise escaping names (Zip Slip). Bound
     * entry count and actual expanded bytes, not only declared sizes. Verify
     * integrity, use noncolliding names, clean a partial extraction on failure,
     * and report corrupt/empty ZIP clearly. Do not copy arbitrary archive trees.
     * ImageImportService subsequently decodes each result. Worker thread only;
     * test traversal, malformed archive, empty ZIP, duplicate names and size caps.
     */
    public List<Path> extract(Path archive) throws ConversionException {
        throw new UnsupportedOperationException("TODO Phase 3: safe ZIP extraction");
    }

    /** Phase 3/8: release only extraction roots owned by this service after
     * workers stop; test that temporary images survive until processing ends. */
    @Override public void close() {
        // TODO Phase 3: no temporary roots exist until extraction is implemented.
        // Once it is, delete only roots owned by this service after workers stop.
    }
}
