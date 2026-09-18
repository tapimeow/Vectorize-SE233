package se233.vectorize.model;

public enum ColorMode {
    UNLIMITED("Unlimited"), CUSTOM("Custom");

    private final String label;
    ColorMode(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
