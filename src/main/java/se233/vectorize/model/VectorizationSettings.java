// Team ownership: Day (Backend/Logic). See README.md "Team split".
package se233.vectorize.model;

import java.util.Objects;

public record VectorizationSettings(DetailLevel detail, ColorMode colorMode,
                                    int customColors, boolean removeBackground) {
    public VectorizationSettings {
        Objects.requireNonNull(detail);
        Objects.requireNonNull(colorMode);
        if (customColors < 2 || customColors > 5) {
            throw new IllegalArgumentException("Custom colors must be between 2 and 5");
        }
    }

    public static VectorizationSettings defaults() {
        return new VectorizationSettings(DetailLevel.MEDIUM, ColorMode.CUSTOM, 2, false);
    }

    // The reason why we have these "with" methods is because records are immutable,
    // so we need to create a new instance with the updated value.
    // Also main reason is about thread-safety
    
    public VectorizationSettings withDetail(DetailLevel value) {
        return new VectorizationSettings(value, colorMode, customColors, removeBackground);
    }
    public VectorizationSettings withColorMode(ColorMode value) {
        return new VectorizationSettings(detail, value, customColors, removeBackground);
    }
    public VectorizationSettings withCustomColors(int value) {
        return new VectorizationSettings(detail, colorMode, value, removeBackground);
    }
    public VectorizationSettings withRemoveBackground(boolean value) {
        return new VectorizationSettings(detail, colorMode, customColors, value);
    }
}
