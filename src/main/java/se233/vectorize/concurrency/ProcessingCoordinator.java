// Team ownership: Day (Backend/Logic). See README.md "Team split".
package se233.vectorize.concurrency;

import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import se233.vectorize.exception.ConversionException;
import se233.vectorize.model.ImageDocument;
import se233.vectorize.model.ProcessingStatus;
import se233.vectorize.model.VectorizationSettings;
import se233.vectorize.service.ExportService;
import se233.vectorize.service.ImageImportService;
import se233.vectorize.service.VectorizationService;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** Phase 6 coordinator boundary; FX callers schedule, workers perform heavy work. */
public final class ProcessingCoordinator implements AutoCloseable {
    public record BatchStatus(int completed, int total, int failed, int cancelled,
                              String message) { }
    private static final int POOL_SIZE =
            Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors()));
    // Bounded pool for overlapping work; single thread for jobs the policy keeps sequential.
    private final ExecutorService workers = Executors.newFixedThreadPool(POOL_SIZE, daemon("vectorize-worker"));
    private final ExecutorService serial = Executors.newSingleThreadExecutor(daemon("vectorize-serial"));
    private final ProcessingPolicy policy = new ProcessingPolicy();
    private final VectorizationService vectorization;
    private final ExportService export;
    private final Map<ImageDocument, Task<?>> previews = new HashMap<>();   // FX thread only
    private final List<Task<?>> active = new ArrayList<>();                 // FX thread only
    public ProcessingCoordinator(VectorizationService vectorization) {
        this.vectorization = vectorization;
        this.export = new ExportService(vectorization);
    }

    /**
     * Import paths without decoding/ZIP work on the JavaFX Application Thread.
     * FR-001–FR-003, NFR-001–NFR-003. Call on FX; callbacks arrive on FX.
     */
    public void importAsync(List<Path> paths, ImageImportService importer,
                            Consumer<ImageImportService.ImportResult> success,
                            Consumer<String> failure) {
        List<Path> snapshot = List.copyOf(paths);
        runBackground(() -> importer.importPaths(snapshot), success, failure);
    }

    /** Phase 9 support: run preset I/O off FX and hand results/errors back safely. */
    public <T> void runBackground(Callable<T> work, Consumer<T> success,
                                  Consumer<String> failure) {
        Task<T> task = new Task<>() {
            @Override protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(event -> success.accept(task.getValue()));
        task.setOnFailed(event -> failure.accept(message(task.getException())));
        task.setOnCancelled(event -> failure.accept("Cancelled."));
        submit(task, workers);
    }

    /** Phase 6: cooperatively cancel active Tasks; the engine polls isCancelled and
     * kills its subprocess. Completed results are left untouched. FX thread only. */
    public void cancelActive() {
        for (Task<?> task : new ArrayList<>(active)) task.cancel(true);
    }

    /**
     * Pre-generate previews and detected colors after multi-drop (FR-013–FR-014).
     * Eligible documents (ProcessingPolicy) overlap on the pool; the others run one
     * at a time. One failure never stops siblings. Call on FX.
     */
    public void preprocess(List<ImageDocument> documents, Runnable changed,
                           Consumer<BatchStatus> progress) {
        List<VectorizationSettings> settings = documents.stream().map(ImageDocument::settings).toList();
        boolean overlap = policy.hasParallelPair(settings);
        Batch batch = new Batch(documents.size(), "Preparing previews");
        for (ImageDocument document : documents) {
            boolean parallel = overlap && policy.isParallelCandidate(documents.size(), document.settings());
            schedulePreview(document, changed, batch, progress, parallel ? workers : serial);
        }
        if (documents.isEmpty()) progress.accept(batch.status());
    }

    /**
     * Schedule a current preview immediately after one image's settings change.
     * The old preview is cancelled and an obsolete completion is ignored. FR-010.
     */
    public void refresh(ImageDocument document, Runnable changed) {
        schedulePreview(document, changed, null, status -> { }, workers);
    }

    /**
     * Export every image off FX to a folder already chosen by DirectoryChooser.
     * Eligible Medium/Custom/2 jobs overlap; failures are reported per image and
     * successes are kept. FR-014–FR-017. Call on FX; callbacks arrive on FX.
     */
    public void exportAll(List<ImageDocument> documents, Path folder,
                          Consumer<BatchStatus> progress, Consumer<String> changed) {
        List<VectorizationSettings> settings = documents.stream().map(ImageDocument::settings).toList();
        boolean overlap = policy.hasParallelPair(settings);
        Batch batch = new Batch(documents.size(), "Exporting");
        for (ImageDocument document : documents) {
            // Snapshot on FX: the worker never touches the mutable ImageDocument.
            boolean current = document.hasCurrentResult();
            ExportService.ExportItem item = new ExportService.ExportItem(document.source(),
                    document.displayName(), document.settings(),
                    current ? document.svg() : null,
                    current ? document.resultSourceVersion() : null,
                    current ? document.resultPipelineVersion() : null);
            Task<ExportService.ExportOutcome> task = new Task<>() {
                @Override protected ExportService.ExportOutcome call() throws Exception {
                    return export.exportOne(item, folder, this::isCancelled, this::updateMessage);
                }
            };
            task.setOnSucceeded(event -> {
                batch.completed++;
                changed.accept("Saved " + task.getValue().savedPath().getFileName());
                progress.accept(batch.status());
            });
            task.setOnFailed(event -> {
                batch.completed++; batch.failed++;
                changed.accept(document.displayName() + ": " + message(task.getException()));
                progress.accept(batch.status());
            });
            task.setOnCancelled(event -> {
                batch.completed++; batch.cancelled++;
                changed.accept(document.displayName() + ": cancelled");
                progress.accept(batch.status());
            });
            boolean parallel = overlap && policy.isParallelCandidate(documents.size(), document.settings());
            submit(task, parallel ? workers : serial);
        }
        if (documents.isEmpty()) progress.accept(batch.status());
    }

    /** Phase 6/8: cancel tasks, shut down and await both pools. */
    @Override public void close() {
        cancelActive();
        workers.shutdownNow();
        serial.shutdownNow();
        try {
            workers.awaitTermination(5, TimeUnit.SECONDS);
            serial.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** One preview Task for one document; FX thread only. */
    private void schedulePreview(ImageDocument document, Runnable changed, Batch batch,
                                 Consumer<BatchStatus> progress, ExecutorService pool) {
        Task<?> old = previews.remove(document);
        if (old != null) old.cancel(true);

        // Immutable snapshot taken on FX.
        Path source = document.source();
        VectorizationSettings settings = document.settings();
        long generation = document.generation();
        document.status(ProcessingStatus.RUNNING);
        document.progress(0);

        Task<VectorizationService.ConversionResult> task = new Task<>() {
            @Override protected VectorizationService.ConversionResult call() throws Exception {
                return vectorization.convert(source, settings, this::isCancelled, this::updateMessage);
            }
        };
        previews.put(document, task);
        boolean[] counted = {false};
        task.setOnSucceeded(event -> {
            if (previews.get(document) == task) previews.remove(document);
            if (document.generation() == generation) {
                VectorizationService.ConversionResult result = task.getValue();
                document.detectedColors(result.detectedColors());
                document.result(result.svg(), settings, result.sourceVersion(), result.pipelineVersion());
                changed.run();
            }
            finish(batch, progress, counted, false, false);
        });
        task.setOnFailed(event -> {
            if (previews.get(document) == task) previews.remove(document);
            if (document.generation() == generation) {
                document.error(message(task.getException()));
                changed.run();
            }
            finish(batch, progress, counted, true, false);
        });
        task.setOnCancelled(event -> {
            if (previews.get(document) == task) previews.remove(document);
            if (document.generation() == generation) {
                document.status(ProcessingStatus.CANCELLED);
                changed.run();
            }
            finish(batch, progress, counted, false, true);
        });
        submit(task, pool);
    }

    private void finish(Batch batch, Consumer<BatchStatus> progress, boolean[] counted,
                        boolean failed, boolean cancelled) {
        if (batch == null || counted[0]) return;
        counted[0] = true;
        batch.completed++;
        if (failed) batch.failed++;
        if (cancelled) batch.cancelled++;
        progress.accept(batch.status());
    }

    /** Track the task until it reaches a final state, then run it on the given pool. */
    private void submit(Task<?> task, ExecutorService pool) {
        active.add(task);
        task.stateProperty().addListener((observable, before, state) -> {
            if (state == Worker.State.SUCCEEDED || state == Worker.State.FAILED
                    || state == Worker.State.CANCELLED) {
                active.remove(task);
            }
        });
        pool.execute(task);
    }

    /** User-safe text: ConversionException messages are written for users already. */
    private static String message(Throwable error) {
        if (error instanceof ConversionException) return error.getMessage();
        if (error == null) return "Unknown error.";
        return "Unexpected error: " + error.getClass().getSimpleName()
                + (error.getMessage() == null ? "" : " (" + error.getMessage() + ")");
    }

    private static ThreadFactory daemon(String name) {
        AtomicInteger count = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, name + "-" + count.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    /** Aggregate counters; mutated only from Task handlers on the FX thread. */
    private static final class Batch {
        final int total; final String action;
        int completed, failed, cancelled;
        Batch(int total, String action) { this.total = total; this.action = action; }
        BatchStatus status() {
            String text = completed < total
                    ? action + ": " + completed + " of " + total
                    : action + " done: " + (completed - failed - cancelled) + " ok, "
                            + failed + " failed, " + cancelled + " cancelled";
            return new BatchStatus(completed, total, failed, cancelled, text);
        }
    }
}
