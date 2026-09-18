// Team ownership: Mona (Frontend/View). See README.md "Team split".
package se233.vectorize.controller;

import se233.vectorize.concurrency.ProcessingCoordinator;
import se233.vectorize.model.AppSession;
import se233.vectorize.model.ImageDocument;
import se233.vectorize.model.VectorizationSettings;
import se233.vectorize.service.ImageImportService;
import se233.vectorize.service.PresetService;
import se233.vectorize.view.VectorizeView;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

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
        // Teaching scaffold intentionally displays the layout without finished handlers.
    }

    /**
     * Begin asynchronous input handling, preserving valid siblings after bad files.
     * TODO Phase 3/6: show importing feedback, ask coordinator.importAsync to call
     * ImageImportService, add successful documents to session, transition to editor,
     * render selected original, and schedule default Medium/Custom/2 preprocessing.
     * Display per-file errors plainly. Do not block FX. FR-001–FR-003, FR-013.
     */
    public void importPaths(List<Path> paths) {
        throw new UnsupportedOperationException("TODO Phase 3: dropped-image coordination");
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
