// Team ownership: Mona (Frontend/View). See README.md "Team split".
package se233.vectorize.controller;

import javafx.stage.FileChooser;
import se233.vectorize.concurrency.ProcessingCoordinator;
import se233.vectorize.model.AppSession;
import se233.vectorize.model.ImageDocument;
import se233.vectorize.model.VectorizationSettings;
import se233.vectorize.service.ImageImportService;
import se233.vectorize.service.PresetService;
import se233.vectorize.view.VectorizeView;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
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
                result -> {
                    if (result.documents().isEmpty()) {
                        // 2. All files failed -> stay on drop screen and display the errors
                        String errorText = "Import failed:\n" + String.join("\n", result.errors());
                        view.dropStatus.setText(errorText);
                    } else {
                        // 3. Successful documents found -> add to session and switch to editor
                        session.addAll(result.documents());
                        view.showEditor();

                        // Reset drop status back to its default instruction text for next time
                        view.dropStatus.setText("JPG, PNG, multiple images, or a ZIP archive");

                        // If there were partial failures, show them in the editor's operationMessage
                        if (result.errors() != null && !result.errors().isEmpty()) {
                            view.operationMessage.setText(String.join("\n", result.errors()));
                        } else {
                            view.operationMessage.setText(""); // Clear out any old messages
                        }
                    }
                },
                errorMessage -> {
                    // 4. Batch-level failure -> stay on drop screen, show error in dropStatus
                    view.dropStatus.setText("Error: " + errorMessage);
                }
        );
    }

    /** TODO Phase 5/6: copy changed settings to selected document, mark its cached
     * preview stale, and request coordinator.refresh. Ignore programmatic control
     * changes made while navigating. Test rapid changes and per-image restoration.
     * FR-006–FR-010; FX thread only. */
    private void changeSettings(VectorizationSettings settings) {
        throw new UnsupportedOperationException("TODO Phase 5: settings event");
    }

    /** TODO Phase 5: redraw current original/SVG or pending/error state, detail/
     * color/background controls, detected swatches, index, Back/Next enablement,
     * and per-file progress. Guard obsolete previews via document generation.
     * All WebView/control mutations must run on FX. FR-004–FR-012. */
    private void showSelected() {
        throw new UnsupportedOperationException("TODO Phase 5: selected-image view state");
    }

    /** TODO Phase 6/7: render honest completed/failed/cancelled counts and current
     * stage in footer; never pretend the CLI gives exact internal percentages.
     * FX callback only. Test partial failure and cancellation. */
    private void showBatch(String action, ProcessingCoordinator.BatchStatus status) {
        throw new UnsupportedOperationException("TODO Phase 6: batch progress display");
    }

    /** TODO Phase 7: open JavaFX DirectoryChooser on FX; if cancelled, do nothing;
     * otherwise delegate batch work to coordinator.exportAll and show results.
     * Never write SVG or invoke CLI in this handler. FR-015–FR-016. */
    private void exportAll() {
        throw new UnsupportedOperationException("TODO Phase 7: OS folder selection");
    }

    /** TODO Phase 9: choose a preset path on FX, snapshot selected settings, and
     * run PresetService.save off FX; display a concise error on failure. */
    private void savePreset() {
        throw new UnsupportedOperationException("TODO Phase 9: save preset gesture");
    }

    /** TODO Phase 9: choose/read preset off FX, validate it, apply to the intended
     * ImageDocument, then request a fresh preview. Test file chooser cancellation. */
    private void loadPreset() {
        throw new UnsupportedOperationException("TODO Phase 9: load preset gesture");
    }
}
