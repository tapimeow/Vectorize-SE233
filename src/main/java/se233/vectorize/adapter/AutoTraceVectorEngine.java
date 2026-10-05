// Team ownership: Day (Backend/Logic). See README.md "Team split".
package se233.vectorize.adapter;

import se233.vectorize.exception.ConversionException;
import se233.vectorize.model.VectorizationSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/** Phase 4 replaceable AutoTrace CLI adapter (runs the external autotrace program). */
public final class AutoTraceVectorEngine implements VectorEngine {
    private static final long TIMEOUT_MILLIS = 60_000;     // one image may take at most 1 minute
    private static final long CHECK_EVERY_MILLIS = 100;     // how often we look at cancel/timeout
    private static final int MAX_ERROR_CHARS = 300;         // keep user messages short

    /**
     * Trace prepared PNG foreground regions into actual SVG vector geometry.
     * Runs: autotrace -output-format svg -error-threshold N [-background-color RRGGBB]
     *       -output-file out.svg in.png
     * Fixed argument list (never a shell string). Polls so Cancel and the timeout
     * can kill the process. SVG validation stays in VectorizationService.
     * Worker thread only. FR-017–FR-018.
     */
    @Override public String trace(Path preparedPng, VectorizationSettings settings,
                                  String ignoredColor, BooleanSupplier cancelled)
                                  throws ConversionException {
        Path svgFile = null;
        Path errorFile = null;
        Process process = null;
        if (cancelled.getAsBoolean()) {                     // don't even start a cancelled job
            throw new ConversionException(ConversionException.Kind.CANCELLED, "Conversion cancelled.");
        }
        if (ignoredColor != null && !ignoredColor.matches("[0-9A-Fa-f]{6}")) {
            throw new ConversionException(ConversionException.Kind.ENGINE,
                    "Invalid background color for AutoTrace: " + ignoredColor);
        }
        try {
            svgFile = Files.createTempFile("vectorize-", ".svg");
            errorFile = Files.createTempFile("vectorize-", ".log");

            // 1. Build the command, one argument per list item.
            List<String> command = new ArrayList<>();
            command.add(executable());
            command.add("-output-format");
            command.add("svg");
            command.add("-error-threshold");               // smaller = follows the edges more closely
            command.add(String.valueOf(settings.detail().curveError()));
            if (ignoredColor != null) {
                command.add("-background-color");          // AutoTrace skips this color = no background shape
                command.add(ignoredColor);
            }
            command.add("-output-file");
            command.add(svgFile.toString());
            command.add(preparedPng.toString());

            // 2. Start it. Error text goes to a file so a full pipe can never freeze the process.
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            builder.redirectError(errorFile.toFile());
            process = start(builder);

            // 3. Wait in small steps, so we can react to Cancel and to the timeout.
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(TIMEOUT_MILLIS);
            while (!process.waitFor(CHECK_EVERY_MILLIS, TimeUnit.MILLISECONDS)) {
                if (cancelled.getAsBoolean()) {
                    throw new ConversionException(ConversionException.Kind.CANCELLED, "Conversion cancelled.");
                }
                if (System.nanoTime() - deadline > 0) {      // nanoTime: immune to clock changes
                    throw new ConversionException(ConversionException.Kind.ENGINE,
                            "AutoTrace took longer than " + TIMEOUT_MILLIS / 1000 + " seconds and was stopped.");
                }
            }

            // 4. Finished: check the exit code, then read the SVG it wrote.
            if (process.exitValue() != 0) {
                String error = Files.readString(errorFile).trim();
                if (error.length() > MAX_ERROR_CHARS) error = error.substring(0, MAX_ERROR_CHARS) + "...";
                throw new ConversionException(ConversionException.Kind.ENGINE,
                        "AutoTrace failed (exit code " + process.exitValue() + ")"
                                + (error.isEmpty() ? "." : ": " + error));
            }
            String svg = Files.readString(svgFile);
            if (svg.isBlank()) {
                throw new ConversionException(ConversionException.Kind.ENGINE, "AutoTrace produced no SVG output.");
            }
            // Known quirk: AutoTrace can exit 0 with an <svg> that has no shapes
            // (e.g. everything was background). Report it as an engine problem.
            if (!svg.contains("<path")) {
                throw new ConversionException(ConversionException.Kind.ENGINE,
                        "AutoTrace found nothing to trace (the image may be all background).");
            }
            return svg;

        } catch (InterruptedException e) {
            // The worker thread was interrupted (Task.cancel): keep the flag for the caller.
            Thread.currentThread().interrupt();
            throw new ConversionException(ConversionException.Kind.CANCELLED, "Conversion cancelled.", e);
        } catch (IOException e) {
            // Temp files could not be created/read.
            throw new ConversionException(ConversionException.Kind.ENGINE,
                    "AutoTrace output could not be read: " + e.getMessage(), e);
        } finally {
            // Always: kill a still-running process (cancel/timeout) and delete temp files.
            if (process != null && process.isAlive()) process.destroyForcibly();
            deleteQuietly(svgFile);
            deleteQuietly(errorFile);
        }
    }

    /** Start the program; a missing or broken executable becomes a user-safe error. */
    private Process start(ProcessBuilder builder) throws ConversionException {
        try {
            return builder.start();
        } catch (IOException e) {
            // AutoTrace is not installed / path is wrong.
            throw new ConversionException(ConversionException.Kind.ENGINE,
                    "AutoTrace is missing or could not be started. Install it (brew install autotrace) "
                            + "or set -Dvectorize.autotrace=/path/to/autotrace.", e);
        }
    }

    /** -Dvectorize.autotrace=... wins; then the usual Homebrew paths; then rely on PATH. */
    private String executable() {
        String configured = System.getProperty("vectorize.autotrace");
        if (configured != null && !configured.isBlank()) return configured;
        for (String candidate : List.of("/opt/homebrew/bin/autotrace", "/usr/local/bin/autotrace")) {
            if (Files.isExecutable(Path.of(candidate))) return candidate;
        }
        return "autotrace";
    }

    private void deleteQuietly(Path file) {
        if (file == null) return;
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // a leftover temp file is harmless; the OS cleans its temp folder
        }
    }
}
