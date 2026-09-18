package se233.vectorize.exception;

public class ConversionException extends Exception {
    public enum Kind { INPUT, ZIP, ENGINE, SVG, OUTPUT, CANCELLED }
    private final Kind kind;

    public ConversionException(Kind kind, String message) { super(message); this.kind = kind; }
    public ConversionException(Kind kind, String message, Throwable cause) { super(message, cause); this.kind = kind; }
    public Kind kind() { return kind; }
}
