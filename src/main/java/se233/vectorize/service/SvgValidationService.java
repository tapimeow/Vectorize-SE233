package se233.vectorize.service;

import se233.vectorize.exception.ConversionException;

/** Phase 4/7 gate shared by preview and export; FR-017, NFR-005. */
public final class SvgValidationService {
    /**
     * Reject malformed, empty, unsafe, or raster-wrapped results.
     * TODO Phase 4: parse XML with DTD/external-entity access disabled; verify
     * SVG root/namespace, useful dimensions and nonempty vector paths/shapes;
     * reject embedded image nodes, scripts and unsafe external references. A CLI
     * exit code of zero is not proof of vector content. Throw an SVG-category
     * ConversionException with an understandable message, retaining technical
     * cause for diagnostics. Parse off FX before WebView or file write. Test valid
     * paths, malformed XML, empty geometry, embedded raster and hostile content.
     */
    public void validate(String svg) throws ConversionException {
        throw new UnsupportedOperationException("TODO Phase 4: SVG validation");
    }
}
