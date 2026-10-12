// Team ownership: Mona (Frontend/View). See README.md "Team split".
package se233.vectorize.controller;

import javafx.scene.layout.Region;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import se233.vectorize.concurrency.ProcessingCoordinator;
import se233.vectorize.model.AppSession;
import se233.vectorize.exception.ConversionException;
import se233.vectorize.model.ImageDocument;
import se233.vectorize.model.ProcessingStatus;
import se233.vectorize.model.VectorizationSettings;
import se233.vectorize.service.ImageImportService;
import se233.vectorize.service.PresetService;
import se233.vectorize.view.VectorizeView;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Phase 5 controller: translate FX gestures into state changes/background jobs. */
public final class VectorizeController {
    private final VectorizeView view;
    private final ProcessingCoordinator coordinator;
    private final ImageImportService importer;
    private final AppSession session = new AppSession();
    private final PresetService presets = new PresetService();
    private boolean updatingControls;
    private UUID displayedDocument;
    private String displayedSvg;
    private String notice = "";
    private final List<String> exportLog = new ArrayList<>();
    private int batchId;   // bumped by New Batch; callbacks from an older batch are ignored

    public VectorizeController(VectorizeView view, ProcessingCoordinator coordinator,
                              ImageImportService importer) {
        this.view = view; this.coordinator = coordinator; this.importer = importer;
        wire();
    }

    /**
     * Connect drag/drop, chooser, zoom, settings, navigation, export and presets.
     * TODO Phase 3/5/7/9: handlers should delegate to methods below/services;
     * only scene graph, state selection and native chooser work belong on FX.
     * Never decode, extract ZIP, quantize, trace, or loop over batch jobs here.
     * Keep the top panel restricted to three zoom buttons and do not add excluded
     * Edit Result/Hand-pick Settings. Test each gesture and keyboard/chooser cancel.
     */
    private void wire() {
        // 1. Hook up drag and drop
        view.onDrop(this::importPaths);

        // 2. Hook up the "Choose files" button
        view.chooseFiles.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Images or ZIP");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image and ZIP Files", "*.jpg", "*.jpeg", "*.png", "*.zip")
            );

            List<File> files = fileChooser.showOpenMultipleDialog(view.stage());
            if (files != null && !files.isEmpty()) {
                List<Path> paths = files.stream()
                        .map(File::toPath)
                        .collect(Collectors.toList());
                importPaths(paths);
            }
        });

        // Hook up Zoom In and Zoom Out buttons
        view.zoomIn.setOnAction(e -> view.setZoom(view.zoom() * 1.25));
        view.zoomOut.setOnAction(e -> view.setZoom(view.zoom() / 1.25));
        view.zoomFit.setOnAction(e -> view.zoomToFit());

        // Navigation
        view.back.setOnAction(e -> { if (session.canBack()) { session.back(); showSelected(); } });
        view.next.setOnAction(e -> { if (session.canNext()) { session.next(); showSelected(); } });

        // Settings: every control change becomes one settings event
        view.detail.valueProperty().addListener((obs, was, now) -> changeSettings(readSettings()));
        view.colorMode.valueProperty().addListener((obs, was, now) -> changeSettings(readSettings()));
        view.customColors.valueProperty().addListener((obs, was, now) -> changeSettings(readSettings()));
        view.removeBackground.selectedProperty().addListener((obs, was, now) -> changeSettings(readSettings()));

        // Export, cancel and presets
        view.exportAll.setOnAction(e -> exportAll());
        view.cancelProcessing.setDisable(true);
        view.cancelProcessing.setOnAction(e -> {
            coordinator.cancelActive();
            view.batchStatus.setText("Cancelling...");
        });
        view.savePreset.setOnAction(e -> savePreset());
        view.loadPreset.setOnAction(e -> loadPreset());

        // Additional feature: go back to the drop screen without restarting the app
        view.newBatch.setOnAction(e -> newBatch());
    }
    /**
     * Begin asynchronous input handling, preserving valid siblings after bad files.
     * TODO Phase 3/6: show importing feedback, ask coordinator.importAsync to call
     * ImageImportService, add successful documents to session, transition to editor,
     * render selected original, and schedule default Medium/Custom/2 preprocessing.
     * Display per-file errors plainly. Do not block FX. FR-001–FR-003, FR-013.
     */
    public void importPaths(List<Path> paths) {
        if (paths == null || paths.isEmpty()) {
            return;
        }

        // 1. Show importing feedback on the drop screen label
        view.dropStatus.setText("Importing " + paths.size() + " item(s)...");

        coordinator.importAsync(paths, importer,
                sameBatch(result -> {
                    if (result.documents().isEmpty()) {
                        // 2. All files failed -> stay on drop screen and display the errors
                        String errorText = "Import failed:\n" + String.join("\n", result.errors());
                        view.dropStatus.setText(errorText);
                    } else {
                        // 3. Successful documents found -> add to session and switch to editor
                        session.addAll(result.documents());
                        view.showEditor();
                        coordinator.preprocess(result.documents(), sameBatch(this::showSelected),
                                sameBatch(status -> showBatch("Preparing previews", status)));

                        // Reset drop status back to its default instruction text for next time
                        view.dropStatus.setText("JPG, PNG, multiple images, or a ZIP archive");

                        // If there were partial failures, show them in the editor's operationMessage
                        if (result.errors() != null && !result.errors().isEmpty()) {
                            notice = String.join("\n", result.errors());
                        } else {
                            notice = ""; // Clear out any old messages
                        }
                        showSelected();
                    }
                }),
                sameBatch(errorMessage -> {
                    // 4. Batch-level failure -> stay on drop screen, show error in dropStatus
                    view.dropStatus.setText("Error: " + errorMessage);
                })
        );
    }

    /** Build settings from the current controls; a half-edited spinner keeps the old count. */
    private VectorizationSettings readSettings() {
        Integer colors = view.customColors.getValue();
        return new VectorizationSettings(view.detail.getValue(), view.colorMode.getValue(),
                colors == null ? VectorizationSettings.defaults().customColors() : colors,
                view.removeBackground.isSelected());
    }

    /** Copy changed settings to the selected document, mark its preview stale and
     * request a fresh one. Programmatic control changes (navigation) are ignored. */
    private void changeSettings(VectorizationSettings settings) {
        if (updatingControls || session.isEmpty()) return;
        if (settings.detail() == null || settings.colorMode() == null) return;
        ImageDocument document = session.selected();
        if (settings.equals(document.settings())) return;
        document.settings(settings);
        showSelected();
        coordinator.refresh(document, sameBatch(this::showSelected));
    }

    /** Redraw everything that depends on the selected document. */
    private void showSelected() {
        if (session.isEmpty()) return;
        ImageDocument document = session.selected();

        if (!document.id().equals(displayedDocument)) {
            displayedDocument = document.id();
            displayedSvg = null;
            view.showOriginal(document.source());
        }

        // Only a result that matches the current settings may be shown
        if (document.hasCurrentResult()) {
            if (!document.svg().equals(displayedSvg)) {
                displayedSvg = document.svg();
                view.showSvg(displayedSvg);
            }
        } else if (displayedSvg != null || document.status() != ProcessingStatus.READY) {
            displayedSvg = null;
            view.clearSvg();
        }

        updatingControls = true;
        try {
            VectorizationSettings settings = document.settings();
            view.detail.setValue(settings.detail());
            view.colorMode.setValue(settings.colorMode());
            view.customColors.getValueFactory().setValue(settings.customColors());
            view.removeBackground.setSelected(settings.removeBackground());
        } finally {
            updatingControls = false;
        }

        view.detectedPalette.getChildren().clear();
        for (int rgb : document.detectedColors()) {
            Region swatch = new Region();
            swatch.setPrefSize(24, 24);
            swatch.setStyle(String.format("-fx-background-color: #%06X; -fx-background-radius: 4;"
                    + " -fx-border-color: #E7E5E4; -fx-border-radius: 4;", rgb & 0xFFFFFF));
            view.detectedPalette.getChildren().add(swatch);
        }

        view.filename.setText((session.selectedIndex() + 1) + " / " + session.size()
                + " \u2022 " + document.displayName());
        view.fileStatus.setText(document.status().name());
        view.fileProgress.setProgress(document.status() == ProcessingStatus.RUNNING
                ? javafx.scene.control.ProgressIndicator.INDETERMINATE_PROGRESS : document.progress());
        view.back.setDisable(!session.canBack());
        view.next.setDisable(!session.canNext());
        view.operationMessage.setText(document.status() == ProcessingStatus.FAILED
                && document.error() != null ? document.error() : notice);
    }

    /** Render honest completed/failed/cancelled counts; the CLI gives no exact percentages. */
    private void showBatch(String action, ProcessingCoordinator.BatchStatus status) {
        String text = status.message() == null || status.message().isEmpty() ? action : status.message();
        view.batchStatus.setText(text);
        boolean running = status.completed() < status.total();
        view.batchProgress.setProgress(status.total() == 0 ? 0
                : (double) status.completed() / status.total());
        view.cancelProcessing.setDisable(!running);
    }

    /** Ask for a folder on FX, then hand the whole batch to the coordinator. */
    private void exportAll() {
        if (session.isEmpty()) return;
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Export SVG files to...");
        File folder = chooser.showDialog(view.stage());
        if (folder == null) return;

        exportLog.clear();
        coordinator.exportAll(session.documents(), folder.toPath(),
                sameBatch(status -> showBatch("Exporting", status)),
                sameBatch(message -> {
                    exportLog.add(message);
                    int from = Math.max(0, exportLog.size() - 4);
                    notice = String.join("\n", exportLog.subList(from, exportLog.size()));
                    showSelected();
                }));
    }

    private static FileChooser presetChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Vectorize preset", "*.properties"));
        return chooser;
    }

    /** Snapshot the selected settings and write them off FX. */
    private void savePreset() {
        if (session.isEmpty()) return;
        File chosen = presetChooser("Save preset").showSaveDialog(view.stage());
        if (chosen == null) return;
        Path file = chosen.toPath();
        if (!file.getFileName().toString().contains(".")) {
            file = file.resolveSibling(file.getFileName() + ".properties");
        }
        Path target = file;
        VectorizationSettings snapshot = session.selected().settings();
        coordinator.runBackground(() -> {
                    try {
                        presets.save(target, snapshot);
                    } catch (java.io.IOException e) {
                        throw new ConversionException(ConversionException.Kind.OUTPUT,
                                "Could not save preset: " + e.getMessage(), e);
                    }
                    return target;
                },
                sameBatch(saved -> { notice = "Preset saved: " + saved.getFileName(); showSelected(); }),
                sameBatch(error -> { notice = error; showSelected(); }));
    }

    /** Read a preset off FX and apply it to the document that was selected at click time. */
    private void loadPreset() {
        if (session.isEmpty()) return;
        File chosen = presetChooser("Load preset").showOpenDialog(view.stage());
        if (chosen == null) return;
        Path file = chosen.toPath();
        ImageDocument target = session.selected();
        coordinator.runBackground(() -> {
                    try {
                        return presets.load(file);
                    } catch (java.io.IOException e) {
                        throw new ConversionException(ConversionException.Kind.INPUT, e.getMessage(), e);
                    }
                },
                sameBatch(settings -> {
                    notice = "Preset loaded: " + file.getFileName();
                    if (!settings.equals(target.settings())) {
                        target.settings(settings);
                        coordinator.refresh(target, sameBatch(this::showSelected));
                    }
                    showSelected();
                }),
                sameBatch(error -> { notice = error; showSelected(); }));
    }

    /**
     * Additional feature: return to the drop screen so a new batch can be imported
     * without restarting the app. Cancels running previews/exports, forgets the
     * current documents, and bumps batchId so late callbacks from the old batch
     * (cancelled previews, a still-running import) cannot touch the new one. FX only.
     */
    private void newBatch() {
        batchId++;
        coordinator.cancelActive();
        session.clear();
        displayedDocument = null;
        displayedSvg = null;
        notice = "";
        exportLog.clear();
        view.resetEditor();
        view.showDrop();
    }

    /** Wrap a callback so it only runs if no New Batch happened since it was scheduled. */
    private Runnable sameBatch(Runnable action) {
        int id = batchId;
        return () -> { if (id == batchId) action.run(); };
    }

    private <T> Consumer<T> sameBatch(Consumer<T> action) {
        int id = batchId;
        return value -> { if (id == batchId) action.accept(value); };
    }
}
