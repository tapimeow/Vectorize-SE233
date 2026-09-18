package se233.vectorize.concurrency;

import javafx.concurrent.Task;
import se233.vectorize.model.ImageDocument;
import se233.vectorize.service.ExportService;
import se233.vectorize.service.ImageImportService;
import se233.vectorize.service.VectorizationService;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** Phase 6 coordinator boundary; FX callers schedule, workers perform heavy work. */
public final class ProcessingCoordinator implements AutoCloseable {
    public record BatchStatus(int completed, int total, int failed, int cancelled,
                              String message) { }
    private ExecutorService workers; // TODO Phase 6: bounded pool, created/shut down deliberately.
    private final ProcessingPolicy policy = new ProcessingPolicy();
    private final VectorizationService vectorization;
    private final ExportService export;
    private final Map<ImageDocument, Task<?>> previews = new HashMap<>();
    private final List<Task<?>> active = new ArrayList<>();
    public ProcessingCoordinator(VectorizationService vectorization) {
        this.vectorization = vectorization;
        this.export = new ExportService(vectorization);
    }

    /**
     * Import paths without decoding/ZIP work on the JavaFX Application Thread.
     * TODO Phase 3/6: wrap ImageImportService.importPaths in a one-shot JavaFX
     * Task on the bounded pool; deliver success/failure callbacks on FX; track
     * active work and cancellation. A partial ImportResult is not a total failure.
     * Test rapid UI interaction during import and corrupt file/ZIP reporting.
     * FR-001–FR-003, NFR-001–NFR-003.
     */
    public void importAsync(List<Path> paths, ImageImportService importer,
                            Consumer<ImageImportService.ImportResult> success,
                            Consumer<String> failure) {
        throw new UnsupportedOperationException("TODO Phase 3/6: async import");
    }

    /** Phase 9 support: run preset I/O off FX and hand results/errors back safely.
     * TODO: use the same bounded worker pool and active-task cleanup; test failures. */
    public <T> void runBackground(Callable<T> work, Consumer<T> success,
                                  Consumer<String> failure) {
        throw new UnsupportedOperationException("TODO Phase 6/9: background operation");
    }

    /** Phase 6: cooperatively cancel active Tasks and CLI processes without
     * erasing completed siblings; do not mutate JavaFX controls from workers.
     * Test pending and running tasks plus application shutdown. */
    public void cancelActive() {
        throw new UnsupportedOperationException("TODO Phase 6: cancellation");
    }

    /**
     * Pre-generate Medium/Custom/2 previews and detected colors after multi-drop.
     * TODO Phase 6: snapshot each ImageDocument's settings/generation, create
     * independent JavaFX Tasks for decode/analyze/quantize/trace/validate, consult
     * ONLY ProcessingPolicy for graded overlap, and send per-file/aggregate stage
     * progress to FX. Individual failures must not stop siblings; the selected
     * preview appears as soon as ready. NFR-001 forbids blocking event handlers.
     * Test overlapping traces, responsive Back/Next and partial failure.
     * FR-013–FR-014; both preprocessing rubric points.
     */
    public void preprocess(List<ImageDocument> documents, Runnable changed,
                           Consumer<BatchStatus> progress) {
        throw new UnsupportedOperationException("TODO Phase 6: batch preprocessing");
    }

    /**
     * Schedule a current preview immediately after one image's settings change.
     * TODO Phase 5/6: cancel the old preview, capture immutable settings/source,
     * increment/check generation, and reject obsolete completion before changing
     * ImageDocument. Task listeners update state on FX. Test rapid detail/colors/
     * background changes; only newest SVG may appear. FR-010.
     */
    public void refresh(ImageDocument document, Runnable changed) {
        throw new UnsupportedOperationException("TODO Phase 5/6: live preview");
    }

    /**
     * Export every image off FX to a folder already chosen by DirectoryChooser.
     * TODO Phase 6/7: snapshot settings/cache; make independent ExportService
     * jobs, consult ProcessingPolicy for Medium/Custom multi-image overlap, bound
     * worker count, publish aggregate/per-image progress, preserve successes after
     * one failure, and support cancellation. Each successful SVG must open normally.
     * Test parallel final tracing/writes, mixed settings, filename collisions,
     * inaccessible folder and partial batch failure. FR-014–FR-017.
     */
    public void exportAll(List<ImageDocument> documents, Path folder,
                          Consumer<BatchStatus> progress, Consumer<String> changed) {
        throw new UnsupportedOperationException("TODO Phase 6/7: parallel Export All");
    }

    /** Phase 6/8: cancel tasks, terminate subprocesses through their cancellation
     * suppliers, shutdown/await the bounded pool, then permit ZIP cleanup. Avoid
     * leaving worker threads behind. Test close during a running batch. */
    @Override public void close() {
        // TODO Phase 6: until worker creation is implemented there is nothing
        // to stop. Once jobs exist, cancellation/shutdown is mandatory.
    }
}
