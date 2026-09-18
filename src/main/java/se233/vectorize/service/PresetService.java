package se233.vectorize.service;

import se233.vectorize.model.VectorizationSettings;
import java.io.IOException;
import java.nio.file.Path;

/** Optional Phase 9 additional feature: named conversion settings presets. */
public final class PresetService {
    /** TODO Phase 9: serialize typed detail/color/count/background settings to a
     * chosen file without corrupting an existing preset; work off FX. Test round
     * trip and write failure. Confirm significance with lecturers (FR-019). */
    public void save(Path file, VectorizationSettings settings) throws IOException {
        throw new UnsupportedOperationException("TODO Phase 9: save preset");
    }
    /** TODO Phase 9: parse and validate every field, including the 2–5 bound;
     * report malformed/missing files clearly. Worker only; test invalid input. */
    public VectorizationSettings load(Path file) throws IOException {
        throw new UnsupportedOperationException("TODO Phase 9: load preset");
    }
}
