// Team ownership: Mona (Frontend/View). See README.md "Team split".
package se233.vectorize.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import se233.vectorize.model.ColorMode;
import se233.vectorize.model.DetailLevel;
import se233.vectorize.model.VectorizationSettings;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Pure JavaFX presentation: controls and layout, no processing or file extraction. */
public final class VectorizeView {
    public final Button chooseFiles = new Button("Choose files");
    public final Button zoomIn = new Button("Zoom In"), zoomOut = new Button("Zoom Out"), zoomFit = new Button("Zoom to Fit");
    public final Button back = new Button("Back"), next = new Button("Next"), exportAll = new Button("Export All");
    public final Button savePreset = new Button("Save"), loadPreset = new Button("Load");
    public final Button cancelProcessing = new Button("Cancel Processing");
    public final ComboBox<DetailLevel> detail = new ComboBox<>();
    public final ComboBox<ColorMode> colorMode = new ComboBox<>();
    public final Spinner<Integer> customColors = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 5, 2));
    public final CheckBox removeBackground = new CheckBox("Remove Background");
    public final ImageView original = new ImageView();
    public final WebView vector = new WebView();
    public final Label filename = new Label(""), fileStatus = new Label(""), batchStatus = new Label("Drop images to start");
    public final Label operationMessage = new Label("");
    public final Label dropStatus = new Label("JPG, PNG, multiple images, or a ZIP archive");
    public final ProgressBar batchProgress = new ProgressBar(0), fileProgress = new ProgressBar(0);
    public final HBox detectedPalette = new HBox(5);
    private final Stage stage;
    private final BorderPane dropRoot = new BorderPane(), editorRoot = new BorderPane();
    private final StackPane originalPane = new StackPane(), vectorPane = new StackPane();
    private final Canvas checker = new Canvas();
    private double zoom = 1;
    private boolean fitMode = true;
    private boolean svgLoading;
    private double svgWidth = 640, svgHeight = 480;
    private Image currentOriginal;

    // Design tokens from DESIGN.md
    private static final String BG          = "-fx-background-color: #FAFAF9;";
    private static final String SURFACE     = "-fx-background-color: #FFFFFF; -fx-background-radius: 12;"
            + " -fx-border-color: #E7E5E4; -fx-border-radius: 12;";
    private static final String PRIMARY_BTN = "-fx-background-color: #4338CA; -fx-text-fill: white;"
            + " -fx-background-radius: 8; -fx-padding: 8 16;";
    private static final String TOOL_BTN    = "-fx-background-color: #0F766E; -fx-text-fill: white;"
            + " -fx-background-radius: 8;";
    private static final String WARN_BTN    = "-fx-background-color: #D97706; -fx-text-fill: white;"
            + " -fx-background-radius: 8; -fx-padding: 4 12;";
    private static final String TITLE_FONT  = "-fx-font-family: 'Georgia'; -fx-font-size: 26; -fx-font-weight: bold;";
    private static final String PROGRESS    = "-fx-accent: #4338CA; -fx-control-inner-background: #F5F5F4;"
            + " -fx-text-box-border: #E7E5E4; -fx-shadow-highlight-color: transparent;";

    private static final double MIN_ZOOM = 0.1;
    private static final double MAX_ZOOM = 5.0;
    private static final java.util.regex.Pattern WIDTH_PATTERN =
            java.util.regex.Pattern.compile("(?<![\\w-])width\\s*=\\s*['\"]([0-9.]+)['\"]");
    private static final java.util.regex.Pattern HEIGHT_PATTERN =
            java.util.regex.Pattern.compile("(?<![\\w-])height\\s*=\\s*['\"]([0-9.]+)['\"]");
    private static final java.util.regex.Pattern VIEWBOX_PATTERN =
            java.util.regex.Pattern.compile("viewBox\\s*=\\s*['\"]\\s*[0-9.]+\\s+[0-9.]+\\s+([0-9.]+)\\s+([0-9.]+)['\"]");


    /**
     * Assemble both screens and show the primary stage.
     * TODO Phase 2, coursebook §1.1.1 JavaFX (Stage/Scene) and §1.4.2 View
     * components: store {@code stage}, call {@link #buildDrop()} then
     * {@link #buildEditor()} to populate {@code dropRoot}/{@code editorRoot},
     * harden {@code vector} (disable JavaScript via
     * {@code vector.getEngine().setJavaScriptEnabled(false)}, disable the
     * context menu, set a transparent page fill) before any SVG is ever loaded
     * into it, then build one {@link Scene} over {@code dropRoot} at a sensible
     * starting size, set the stage title, and show it. There is no stylesheet
     * to attach — match the coursebook's own case study (Scripts 1.22–1.24),
     * which styles nodes with plain {@code pane.setStyle("-fx-background-
     * color: ...")} calls where a look actually needs to be set, not an
     * external {@code .css} file. The app always starts on the drop screen;
     * {@link #showEditor()}/{@link #showDrop()} later swap the Scene's root.
     * Test that the window actually appears and that WebView never executes
     * script from untrusted SVG content.
     */
    public VectorizeView(Stage stage) {
        this.stage = stage;
        stage.setMinWidth(850);
        stage.setMinHeight(680);
        // 1. build the two screens
        buildDrop();
        buildEditor();
        setupControls();
        // 2. make the WebView safe BEFORE any SVG goes in
        vector.getEngine().setJavaScriptEnabled(false);
        vector.setContextMenuEnabled(false);
        vector.setPageFill(Color.TRANSPARENT);
        // 3. one Scene, starting on the drop screen
        Scene scene = new Scene(dropRoot, 1100, 720);
        stage.setTitle("Vectorize");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Lay out the drop screen: a centered card with title, subtitle,
     * instructions, the {@link #chooseFiles} button and {@link #dropStatus}.
     * TODO Phase 2, coursebook §1.4.2 View components / §1.4.4 Flexible-size
     * container: build the card with a {@link VBox}, center it in
     * {@code dropRoot} with a {@link StackPane}, and add a footer label. Use
     * plain controls and, only where it genuinely helps readability, an inline
     * {@code .setStyle("-fx-...")} call — the same technique
     * {@code InventoryPane}/{@code EquipPane}/{@code CharacterPane} use in the
     * coursebook case study. This method only arranges controls; it must not
     * attach drag/drop handlers — that belongs in {@link #onDrop(Consumer)}.
     */
    private void buildDrop() {
        // 1. create 3 new Labels: title, subtitle, instructions
        Label title = new Label("Vectorize");
        title.setStyle(TITLE_FONT);
        Label subtitle = new Label("Turn raster images into clean vectors");
        subtitle.setStyle("-fx-text-fill: #57534E;");
        Label instructions = new Label("Drag and drop your images here");
        chooseFiles.setStyle(PRIMARY_BTN);

        // 2. put the 5 things in a VBox card, with spacing
        VBox card = new VBox(15, title, subtitle, instructions, chooseFiles, dropStatus);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40));
        card.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        card.setStyle(SURFACE);

        // 3. center the card
        StackPane center = new StackPane(card);

        // 4. footer
        Label footer = new Label("Supports PNG, JPG, and ZIP archive");
        BorderPane.setAlignment(footer, Pos.CENTER);
        BorderPane.setMargin(footer, new Insets(0, 0, 16, 0));

        // 5. put them in dropRoot (a BorderPane): which two areas?
        dropRoot.setCenter(center);
        dropRoot.setBottom(footer);
        dropRoot.setStyle(BG);
    }

    /**
     * Lay out the editor screen: a top toolbar with only the three zoom
     * buttons, a center row with the original/vector preview panes side by
     * side, a right settings panel, and a bottom status footer.
     * TODO Phase 2, coursebook §1.4.2 View components / §1.4.4 Flexible-size
     * container: build {@code editorRoot} with a {@link BorderPane} —
     * {@code setTop} for {@link #zoomIn}/{@link #zoomOut}/{@link #zoomFit} only
     * (FR-005 forbids adding more top controls); {@code setCenter} for
     * {@code originalPane}/{@code vectorPane} in an {@link HBox} that grows
     * with the window ({@link Priority#ALWAYS} on both sides, so resizing keeps
     * them equal); {@code setRight} for {@link #detail}, {@link #colorMode},
     * {@link #customColors}, {@link #detectedPalette}, {@link #removeBackground},
     * {@link #back}/{@link #next}, {@link #exportAll}, and
     * {@link #savePreset}/{@link #loadPreset}; {@code setBottom} for
     * {@link #filename}, {@link #fileStatus}, {@link #fileProgress},
     * {@link #batchStatus}, {@link #cancelProcessing}, {@link #batchProgress}
     * and {@link #operationMessage}. Bind {@code original}'s fit width/height to
     * its pane (minus padding) so it resizes responsively, and add
     * {@code checker} behind {@code vector} in {@code vectorPane} (its drawing
     * is {@link #drawChecker()}, a separate Phase 5 TODO). The small
     * {@link #previewColumn}/{@link #section}/{@link #spacer} helpers below are
     * provided to keep this method's structure readable — use them rather than
     * duplicating their layout. Attach no event handlers or bindings that
     * belong to Phase 5 (live preview, zoom math); this method only arranges
     * controls into panes.
     */
    private void buildEditor() {
        // ==========================================
        // PART A: TOP & CENTER AREAS
        // ==========================================

        // TOP area (only the 3 zoom buttons)
        HBox toolbar = new HBox(10, zoomIn, zoomOut, zoomFit);
        toolbar.setPadding(new Insets(12));
        editorRoot.setTop(toolbar);

        // CENTER area - Preview columns and HBox growth
        VBox originalColumn = previewColumn("Original", originalPane);
        VBox vectorColumn = previewColumn("Vector", vectorPane);

        originalColumn.setPrefWidth(0);
        vectorColumn.setPrefWidth(0);

        HBox previews = new HBox(15, originalColumn, vectorColumn);
        HBox.setHgrow(originalColumn, Priority.ALWAYS);
        HBox.setHgrow(vectorColumn, Priority.ALWAYS);
        previews.setPadding(new Insets(0, 15, 15, 15));
        editorRoot.setCenter(previews);

        // Binding for original ImageView size and ratio
        original.fitWidthProperty().bind(originalPane.widthProperty().subtract(20));
        original.fitHeightProperty().bind(originalPane.heightProperty().subtract(20));
        original.setPreserveRatio(true);
        originalPane.getChildren().add(original);

        // Layers in vectorPane (checker behind vector)
        vectorPane.getChildren().addAll(checker, vector);
        vectorPane.setMinSize(0, 0);
        // Trigger drawChecker when dimensions change
        vectorPane.widthProperty().addListener((obs, oldV, newV) -> {
            if (fitMode) applyFitZoom();
            drawChecker();
        });
        vectorPane.heightProperty().addListener((obs, oldV, newV) -> {
            if (fitMode) applyFitZoom();
            drawChecker();
        });

        // ==========================================
        // PART B: RIGHT AREA (SETTINGS PANEL)
        // ==========================================

        // Make Back and Next share the row width equally
        back.setMaxWidth(Double.MAX_VALUE);
        next.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(back, Priority.ALWAYS);
        HBox.setHgrow(next, Priority.ALWAYS);
        savePreset.setMaxWidth(Double.MAX_VALUE);
        loadPreset.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(savePreset, Priority.ALWAYS);
        HBox.setHgrow(loadPreset, Priority.ALWAYS);

        HBox navRow = new HBox(10, back, next);
        HBox presetRow = new HBox(10, savePreset, loadPreset);

        VBox settings = new VBox(10,
                section("Detail"), detail,
                section("Colors"), colorMode, customColors, detectedPalette,
                section("Background"), removeBackground,
                spacer(),
                navRow,
                exportAll,
                section("Preset"), presetRow);

        settings.setPadding(new Insets(15));
        settings.setPrefWidth(260);
        settings.setStyle(SURFACE);

        // Make wide controls fill the panel width
        detail.setMaxWidth(Double.MAX_VALUE);
        colorMode.setMaxWidth(Double.MAX_VALUE);
        customColors.setMaxWidth(Double.MAX_VALUE);
        exportAll.setMaxWidth(Double.MAX_VALUE);

        // Apply button styles from DESIGN.md
        exportAll.setStyle(PRIMARY_BTN);
        savePreset.setStyle(TOOL_BTN);
        loadPreset.setStyle(TOOL_BTN);

        // Margin around the settings panel and placement on the right
        BorderPane.setMargin(settings, new Insets(26, 15, 15, 0));
        editorRoot.setRight(settings);


        // ==========================================
        // PART C: BOTTOM AREA (STATUS BAR)
        // ==========================================

        Region fileGap = new Region();
        HBox.setHgrow(fileGap, Priority.ALWAYS);

        // Progress bars follow the Cancel button: same width (top bar) and same height (both bars)
        // Cancel button = width of the settings panel's controls (panel width minus its 15+15 padding)
        cancelProcessing.prefWidthProperty().bind(settings.widthProperty().subtract(30));
        fileProgress.prefWidthProperty().bind(cancelProcessing.widthProperty());
        fileProgress.prefHeightProperty().bind(cancelProcessing.heightProperty());
        batchProgress.prefHeightProperty().bind(cancelProcessing.heightProperty());
        batchProgress.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(batchProgress, Priority.ALWAYS);
        // Right edge of the batch bar lines up with the Vector box above:
        // the Cancel button sits 30 px right of the Vector box (previews 15 + settings margin),
        // and the HBox spacing already gives 10, so add the missing 20.
        HBox.setMargin(batchProgress, new Insets(0, 20, 0, 0));

        cancelProcessing.setStyle(WARN_BTN);
        fileProgress.setStyle(PROGRESS);
        batchProgress.setStyle(PROGRESS);

        // Show operationMessage only when it has text (no empty line)
        operationMessage.managedProperty().bind(operationMessage.textProperty().isNotEmpty());
        operationMessage.visibleProperty().bind(operationMessage.textProperty().isNotEmpty());

        HBox fileRow = new HBox(10, filename, fileStatus, fileGap, fileProgress);
        fileRow.setAlignment(Pos.CENTER_LEFT);

        HBox batchRow = new HBox(10, batchStatus, batchProgress, cancelProcessing);
        batchRow.setAlignment(Pos.CENTER_LEFT);

        VBox statusBar = new VBox(8, fileRow, batchRow, operationMessage);
        statusBar.setPadding(new Insets(15));   // same as the settings panel, so right edges line up
        statusBar.setStyle(SURFACE);

        BorderPane.setMargin(statusBar, new Insets(0, 15, 15, 15));
        editorRoot.setBottom(statusBar);


        // ==========================================
        // GENERAL STYLING & CONSTRAINTS
        // ==========================================

        editorRoot.setStyle(BG);
        originalPane.setStyle(SURFACE);
        vectorPane.setStyle(SURFACE);

        originalPane.setPadding(new Insets(10));
        vectorPane.setPadding(new Insets(10));
        originalPane.setMinSize(0, 0);

        // Register load worker listener once in buildEditor()
        vector.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                svgLoading = false;
                if (fitMode) {
                    zoomToFit();
                } else {
                    applyZoom(zoom); // Consistent with the rest of the zoom codebase
                }
            } else if (newState == javafx.concurrent.Worker.State.FAILED) {
                svgLoading = false;
                fileStatus.setText("Could not render SVG preview");
            }
        });

        // Bind checker dimensions to vectorPane minus the 20px padding (10px on each side)
        checker.widthProperty().bind(vectorPane.widthProperty().subtract(20));
        checker.heightProperty().bind(vectorPane.heightProperty().subtract(20));

    }

    private static VBox previewColumn(String heading, StackPane pane) {
        Label title = new Label(heading); title.getStyleClass().add("preview-title");
        VBox column = new VBox(9, title, pane); column.setMinWidth(250);
        VBox.setVgrow(pane, Priority.ALWAYS); column.setMaxWidth(Double.MAX_VALUE);
        return column;
    }
    private static Label section(String text) { Label label = new Label(text); label.getStyleClass().add("section-label"); return label; }
    private static Region spacer() { Region region = new Region(); VBox.setVgrow(region, Priority.ALWAYS); return region; }

    private void drawChecker() {
        // TODO Phase 5, FR-011: draw a checkerboard sized to the vector pane so
        // missing SVG background paths visibly read as transparent. This is FX
        // presentation only; do not alter vectorization or source alpha here.
        double w = checker.getWidth();
        double h = checker.getHeight();

        if (w <= 0 || h <= 0) {
            return;
        }

        GraphicsContext g = checker.getGraphicsContext2D();
        double tileSize = 16.0;

        g.setFill(Color.web("#FFFFFF"));
        g.fillRect(0, 0, w, h);

        g.setFill(Color.web("#E7E5E4"));
        int cols = (int) Math.ceil(w / tileSize);
        int rows = (int) Math.ceil(h / tileSize);

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if ((row + col) % 2 != 0) {
                    g.fillRect(col * tileSize, row * tileSize, tileSize, tileSize);
                }
            }
        }
    }

    /** Phase 3, FR-001: accept OS file drops on both screens, extract dropped
     * Paths from the dragboard, and pass them to the controller. The controller
     * delegates decoding/ZIP to a worker. Handle an empty/nonfile drag and test
     * one, multiple and ZIP drops without heavy work on FX. */

    public void onDrop(Consumer<List<Path>> receiver) {
        setupDropHandler(dropRoot, receiver);
        setupDropHandler(editorRoot, receiver);
    }

    private void setupDropHandler(Node node, Consumer<List<Path>> receiver) {
        // 1. Handle drag over: accept only if the dragboard has files
        node.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        // 2. Handle drag dropped: extract files, convert to Paths, and pass to receiver
        node.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;

            if (db.hasFiles()) {
                List<Path> paths = db.getFiles().stream()
                        .map(File::toPath)
                        .collect(Collectors.toList());

                if (!paths.isEmpty()) {
                    receiver.accept(paths);
                    success = true;
                }
            }

            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void setupControls() {
        detail.getItems().addAll(DetailLevel.values());
        detail.setValue(VectorizationSettings.defaults().detail());

        colorMode.getItems().addAll(ColorMode.values());
        colorMode.setValue(VectorizationSettings.defaults().colorMode());

        customColors.disableProperty().bind(
                colorMode.valueProperty().isEqualTo(ColorMode.UNLIMITED)
        );
    }

    public void showEditor() { stage.getScene().setRoot(editorRoot); }

    public void showDrop() { stage.getScene().setRoot(dropRoot); }

    public Stage stage() { return stage; }

    /** Phase 5, FR-004: load the selected original without blocking FX; verify
     * error handling and fit it beside the vector preview. */
    public void showOriginal(Path path) {
        if (path == null) {
            currentOriginal = null;
            original.setImage(null);
            fileStatus.setText("");
            return;
        }

        fileStatus.setText("");
        String url = path.toUri().toString();

        // Load in the background with backgroundLoading = true
        Image image = new Image(url, true);
        currentOriginal = image;

        // Listen for loading errors without blocking FX
        image.errorProperty().addListener((obs, wasError, isError) -> {
            if (isError && currentOriginal == image) {
                fileStatus.setText("Could not load image");
                original.setImage(null);
            }
        });

        original.setImage(image);
    }

    /** Phase 5, FR-004/FR-010: display only a validated, current-generation SVG
     * through WebView. Parse useful dimensions for Zoom to Fit, disable scripts,
     * and test asynchronous load plus replacement by a newer result. FX only. */
    public void showSvg(String svg) {
        if (svg == null || svg.isEmpty()) {
            clearSvg();
            return;
        }

        svgLoading = true;
        fileStatus.setText("");

        // Parse dimensions from the first SVG tag or fallback to viewBox
        parseSvgDimensions(svg);

        // Load content using the SVG content type to avoid the 8px browser HTML margin
        vector.getEngine().loadContent(svg, "image/svg+xml");
    }

    private void parseSvgDimensions(String svg) {
        try {
            // Step 1: Isolate only the opening <svg ...> tag
            int svgStart = svg.indexOf("<svg");
            if (svgStart == -1) return;
            int svgEnd = svg.indexOf('>', svgStart);
            if (svgEnd == -1) return;
            String svgTag = svg.substring(svgStart, svgEnd + 1);

            // Step 2: Check for explicit width and height attributes within the tag
            java.util.regex.Matcher wMat = WIDTH_PATTERN.matcher(svgTag);
            java.util.regex.Matcher hMat = HEIGHT_PATTERN.matcher(svgTag);

            if (wMat.find() && hMat.find()) {
                // Read into local variables first to keep updates atomic
                double w = Double.parseDouble(wMat.group(1));
                double h = Double.parseDouble(hMat.group(1)); // Fixed group(2) -> group(1)
                svgWidth = w;
                svgHeight = h;
                return;
            }

            // Step 3: Fallback to viewBox attribute if explicit width/height are missing
            java.util.regex.Matcher vbMat = VIEWBOX_PATTERN.matcher(svgTag);
            if (vbMat.find()) {
                double w = Double.parseDouble(vbMat.group(1));
                double h = Double.parseDouble(vbMat.group(2));
                svgWidth = w;
                svgHeight = h;
            }
        } catch (NumberFormatException e) {
            // Catch only specific parsing errors so other unexpected bugs fail visibly
        }
    }

    /** Phase 5: mark a stale/missing result as pending rather than displaying the
     * previous image's SVG; preserve an understandable loading/error state. */
    public void clearSvg() {
        svgLoading = false;
        svgWidth = 640;
        svgHeight = 480;
        vector.getEngine().loadContent("");
        fileStatus.setText("Preview pending...");
    }

    /** Phase 5, FR-005: Zoom In/Out changes presentation, never tracing. Clamp
     * scale reasonably and test bounds while WebView is loading. */
    public void setZoom(double value) {
        fitMode = false;
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, value));
        applyZoom(zoom);
    }

    private void applyZoom(double value) {
        vector.setZoom(value);
    }

    public double zoom() { return zoom; }
    /** Phase 5, FR-005: fit the current SVG dimensions inside vectorPane, account
     * for pane resize, and defer fit until WebView load completes. Test both tiny
     * and large SVG and a user zoom override. FX only. */
    public void zoomToFit() {
        fitMode = true;
        if (svgLoading) {
            return;
        }
        applyFitZoom();
    }

    private void applyFitZoom() {
        if (!fitMode || svgLoading) {
            return;
        }

        // Account for the 10px padding on each side of vectorPane (10 + 10 = 20)
        double availW = vectorPane.getWidth() - 20;
        double availH = vectorPane.getHeight() - 20;

        if (availW <= 0 || availH <= 0) {
            return;
        }

        double scaleX = availW / svgWidth;
        double scaleY = availH / svgHeight;
        double fitScale = Math.min(scaleX, scaleY) * 0.98;

        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, fitScale));
        applyZoom(zoom);
    }
}
