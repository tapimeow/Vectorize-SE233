// Team ownership: Day (Backend/Logic). See README.md "Team split".
package se233.vectorize.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Mutable session state; the coordinator updates it on the JavaFX Application Thread. */
public final class ImageDocument {
    private final UUID id = UUID.randomUUID();
    private final Path source;
    private final String displayName;
    private VectorizationSettings settings = VectorizationSettings.defaults();
    private ProcessingStatus status = ProcessingStatus.QUEUED;
    private List<Integer> detectedColors = List.of();
    private String svg;
    private VectorizationSettings resultSettings;
    private String resultSourceVersion;
    private String resultPipelineVersion;
    private long generation;
    private String error;
    private double progress;

    public ImageDocument(Path source, String displayName) {
        this.source = Objects.requireNonNull(source);
        this.displayName = Objects.requireNonNull(displayName);
    }
    public UUID id() {
        return id;
    }
    public Path source() { return
            source;
    }
    public String displayName() {
        return displayName;
    }
    public VectorizationSettings settings() {
        return settings;
    }
    public void settings(VectorizationSettings value) {
        settings = Objects.requireNonNull(value);
        generation++;
        status = ProcessingStatus.QUEUED;
        error = null;
        progress = 0;
    }
    public ProcessingStatus status() {
        return status;
    }
    public void status(ProcessingStatus value) {
        status = value;
    }
    public List<Integer> detectedColors() {
        return detectedColors;
    }
    public void detectedColors(List<Integer> value) {
        detectedColors = List.copyOf(value);
    }
    public String svg() {
        return svg;
    }
    public boolean hasCurrentResult() {
        return svg != null && settings.equals(resultSettings);
    }
    public String resultSourceVersion() {
        return resultSourceVersion;
    }
    public String resultPipelineVersion() {
        return resultPipelineVersion;
    }
    public void result(String value, VectorizationSettings resultSettings, String sourceVersion,
                       String pipelineVersion) {
        svg = value;
        this.resultSettings = resultSettings;
        resultSourceVersion = sourceVersion;
        resultPipelineVersion = pipelineVersion;
        status = ProcessingStatus.READY;
        error = null; progress = 1;
    }
    public long generation() {
        return generation;
    }
    public long nextGeneration() {
        return ++generation;
    }
    public String error() {
        return error;
    }
    public void error(String value) {
        error = value;
        status = ProcessingStatus.FAILED; }
    public double progress() {
        return progress;
    }
    public void progress(double value) {
        progress = value;
    }
}
