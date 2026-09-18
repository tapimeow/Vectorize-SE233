// Team ownership: Day (Backend/Logic). See README.md "Team split".
package se233.vectorize.adapter;

import se233.vectorize.exception.ConversionException;
import se233.vectorize.model.VectorizationSettings;
import java.nio.file.Path;
import java.util.function.BooleanSupplier;

/** Phase 4 replaceable AutoTrace CLI adapter; complete integration is NOT here. */
public final class AutoTraceVectorEngine implements VectorEngine {
    /**
     * Trace prepared PNG foreground regions into actual SVG vector geometry.
     * TODO Phase 4: verify an installed executable and its color/alpha/SVG/license
     * behavior first. Build a fixed ProcessBuilder argument list (never a shell
     * string); map DetailLevel to engine options; pass the ignored mask color if
     * supported; capture exit/error output; enforce timeout and destroy the process
     * on cancellation. Read SVG output and clean temporary files. Do not rely on
     * tool-side color count alone: the app's Custom preprocessing controls 2–5
     * colors; some engine option combinations can yield empty SVG with exit zero.
     * Throw ENGINE/CANCELLED ConversionException for missing tool, nonzero exit,
     * timeout or interruption. SVG validation remains in VectorizationService.
     * Worker thread only. Test with real JPG/PNG fixtures and a missing executable.
     * FR-017–FR-018; vector-format and background/color rubric.
     */
    @Override public String trace(Path preparedPng, VectorizationSettings settings,
                                  String ignoredColor, BooleanSupplier cancelled)
                                  throws ConversionException {
        throw new UnsupportedOperationException("TODO Phase 4: AutoTrace adapter");
    }
}
