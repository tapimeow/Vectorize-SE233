// Team ownership: Mona (Frontend/View). See README.md "Team split".
package se233.vectorize.service;

import se233.vectorize.model.ColorMode;
import se233.vectorize.model.DetailLevel;
import se233.vectorize.model.VectorizationSettings;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** Optional Phase 9 additional feature: named conversion settings presets. */
public final class PresetService {
    private static final String DETAIL = "detail", COLOR_MODE = "colorMode",
            CUSTOM_COLORS = "customColors", REMOVE_BACKGROUND = "removeBackground";

    /** Write the settings as a small properties file. The data goes to a temp file
     * next to the target first, so a failed write never corrupts an existing preset. */
    public void save(Path file, VectorizationSettings settings) throws IOException {
        Properties properties = new Properties();
        properties.setProperty(DETAIL, settings.detail().name());
        properties.setProperty(COLOR_MODE, settings.colorMode().name());
        properties.setProperty(CUSTOM_COLORS, Integer.toString(settings.customColors()));
        properties.setProperty(REMOVE_BACKGROUND, Boolean.toString(settings.removeBackground()));

        Path absolute = file.toAbsolutePath();
        Path temp = Files.createTempFile(absolute.getParent(), "preset-", ".tmp");
        try {
            try (Writer out = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                properties.store(out, "Vectorize preset");
            }
            try {
                Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    /** Parse and validate every field; any problem is reported as an IOException
     * with a message that is safe to show the user. */
    public VectorizationSettings load(Path file) throws IOException {
        Properties properties = new Properties();
        try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(in);
        } catch (NoSuchFileException e) {
            throw new IOException("Preset file not found: " + file.getFileName(), e);
        } catch (IOException | IllegalArgumentException e) {
            throw new IOException("Preset file could not be read: " + file.getFileName(), e);
        }
        DetailLevel detail = enumValue(properties, DETAIL, DetailLevel.class);
        ColorMode colorMode = enumValue(properties, COLOR_MODE, ColorMode.class);
        int colors;
        try {
            colors = Integer.parseInt(required(properties, CUSTOM_COLORS));
        } catch (NumberFormatException e) {
            throw new IOException("Preset field customColors must be a whole number.", e);
        }
        if (colors < 2 || colors > 5) {
            throw new IOException("Preset field customColors must be between 2 and 5.");
        }
        String background = required(properties, REMOVE_BACKGROUND);
        if (!background.equals("true") && !background.equals("false")) {
            throw new IOException("Preset field removeBackground must be true or false.");
        }
        return new VectorizationSettings(detail, colorMode, colors, Boolean.parseBoolean(background));
    }

    private static String required(Properties properties, String key) throws IOException {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IOException("Preset is missing the field " + key + ".");
        }
        return value.trim();
    }

    private static <E extends Enum<E>> E enumValue(Properties properties, String key,
                                                   Class<E> type) throws IOException {
        try {
            return Enum.valueOf(type, required(properties, key).toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IOException("Preset field " + key + " has an unknown value.", e);
        }
    }
}
