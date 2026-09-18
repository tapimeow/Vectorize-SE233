package se233.vectorize.model;

public enum DetailLevel {
    LOW("Low"), MEDIUM("Medium"), HIGH("High");

    private final String label;
    DetailLevel(String label) { this.label = label; }

    /** Phase 4, FR-007: choose a detail-dependent resize cap; test visibly
     * different Low/Medium/High outputs without exhausting memory. */
    public int maxDimension() { throw new UnsupportedOperationException("TODO Phase 4: detail resize policy"); }
    /** Phase 4, FR-007: choose a corresponding tracer tolerance or equivalent
     * parameter; test that detail changes actual SVG, not only UI text. */
    public double curveError() { throw new UnsupportedOperationException("TODO Phase 4: tracing detail policy"); }
    @Override public String toString() { return label; }
}
