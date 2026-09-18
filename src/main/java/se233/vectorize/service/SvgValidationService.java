// Team ownership: Day (Backend/Logic). See README.md "Team split".
package se233.vectorize.service;

import se233.vectorize.exception.ConversionException;

/** Phase 4/7 gate shared by preview and export; FR-017, NFR-005. */
public final class SvgValidationService {
    /**
     * Reject malformed, empty, unsafe, or raster-wrapped results.
     * TODO Phase 4: parse the XML; verify SVG root/namespace, useful dimensions
     * and nonempty vector paths/shapes; reject embedded image nodes (so a lazy
     * "wrap the raster in an SVG" shortcut can't pass FR-017), scripts and
     * unsafe external references. A CLI exit code of zero is not proof of
     * vector content. Throw an SVG-category ConversionException with an
     * understandable message, retaining technical cause for diagnostics. Parse
     * off FX before WebView or file write. Test valid paths, malformed XML,
     * empty geometry, and embedded raster.
     */
    public void validate(String svg) throws ConversionException {
        throw new UnsupportedOperationException("TODO Phase 4: SVG validation");
    }
}
