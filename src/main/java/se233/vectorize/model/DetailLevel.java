// Team ownership: Day (Backend/Logic). See README.md "Team split".
package se233.vectorize.model;

public enum DetailLevel {
    LOW("Low"), MEDIUM("Medium"), HIGH("High");

    private final String label;
    DetailLevel(String label) {
        this.label = label;
    }

    /** Phase 4, FR-007: choose a detail-dependent resize cap; test visibly
     * different Low/Medium/High outputs without exhausting memory. */
    public int maxDimension() {
        return switch (this) {
            case LOW -> 512;
            case MEDIUM -> 1024;
            case HIGH -> 2048;
        };
    }
    /** Phase 4, FR-007: choose a corresponding tracer tolerance or equivalent
     * parameter; test that detail changes actual SVG, not only UI text. */
    public double curveError() {
        return switch (this) {
            case LOW -> 0.5;
            case MEDIUM -> 0.25;
            case HIGH -> 0.1;
        };
    }

    @Override public String toString() {
        return label;
    }
}
