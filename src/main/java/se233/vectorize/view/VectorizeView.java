package se233.vectorize.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import se233.vectorize.model.ColorMode;
import se233.vectorize.model.DetailLevel;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

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
    private int svgWidth = 640, svgHeight = 480;

    public VectorizeView(Stage stage) {
        this.stage = stage;
        buildDrop(); buildEditor();
        vector.getEngine().setJavaScriptEnabled(false);
        // TODO Phase 5: after asynchronous SVG load, safely recompute fit zoom
        // if the user still selected Zoom to Fit. Do not apply an obsolete SVG.
        vector.setContextMenuEnabled(false);
        vector.setPageFill(javafx.scene.paint.Color.TRANSPARENT);
        Scene scene = new Scene(dropRoot, 1180, 740);
        scene.getStylesheets().add(getClass().getResource("/se233/vectorize/app.css").toExternalForm());
        stage.setTitle("Raster to Vector • SE233"); stage.setScene(scene); stage.show();
    }

    private void buildDrop() {
        Label title = new Label("Raster → Vector"); title.getStyleClass().add("hero-title");
        Label subtitle = new Label("Turn JPG and PNG images into real, scalable SVG paths.");
        subtitle.getStyleClass().add("muted");
        Label instruction = new Label("Drop images or one ZIP archive here"); instruction.getStyleClass().add("drop-heading");
        VBox card = new VBox(18, title, subtitle, instruction, chooseFiles, dropStatus);
        card.setAlignment(Pos.CENTER); card.setPadding(new Insets(60)); card.getStyleClass().add("drop-card");
        StackPane center = new StackPane(card); center.setPadding(new Insets(45));
        dropRoot.setCenter(center);
        dropRoot.setBottom(new Label("SE233 • Advanced Programming • JPG / PNG → SVG"));
        BorderPane.setMargin(dropRoot.getBottom(), new Insets(12, 24, 18, 24));
    }

    private void buildEditor() {
        HBox toolbar = new HBox(10, zoomIn, zoomOut, zoomFit);
        toolbar.setAlignment(Pos.CENTER_LEFT); toolbar.setPadding(new Insets(15, 22, 15, 22));
        toolbar.getStyleClass().add("toolbar"); editorRoot.setTop(toolbar);

        original.setPreserveRatio(true); original.setSmooth(true);
        original.fitWidthProperty().bind(originalPane.widthProperty().subtract(28));
        original.fitHeightProperty().bind(originalPane.heightProperty().subtract(28));
        originalPane.getChildren().add(original); originalPane.getStyleClass().add("preview-pane");
        checker.setManaged(false); checker.setMouseTransparent(true);
        vectorPane.getChildren().addAll(checker, vector); vectorPane.getStyleClass().add("preview-pane");
        // TODO Phase 5: on pane resize redraw transparency checker and, only
        // while fit mode is selected, recompute Zoom to Fit on the FX thread.
        vector.setMinWidth(0); vector.setMinHeight(0);
        vector.setMaxWidth(Double.MAX_VALUE); vector.setMaxHeight(Double.MAX_VALUE);
        VBox left = previewColumn("Original Image", originalPane);
        VBox right = previewColumn("Vectorized Result", vectorPane);
        HBox previews = new HBox(16, left, right); previews.setPadding(new Insets(18));
        left.setMinWidth(0); right.setMinWidth(0);
        left.setPrefWidth(1); right.setPrefWidth(1);
        HBox.setHgrow(left, Priority.ALWAYS); HBox.setHgrow(right, Priority.ALWAYS);
        editorRoot.setCenter(previews);

        detail.getItems().setAll(DetailLevel.values()); colorMode.getItems().setAll(ColorMode.values());
        detail.setMaxWidth(Double.MAX_VALUE); colorMode.setMaxWidth(Double.MAX_VALUE);
        customColors.setEditable(false); customColors.setMaxWidth(Double.MAX_VALUE);
        HBox navigation = new HBox(8, back, next); HBox.setHgrow(back, Priority.ALWAYS); HBox.setHgrow(next, Priority.ALWAYS);
        back.setMaxWidth(Double.MAX_VALUE); next.setMaxWidth(Double.MAX_VALUE);
        exportAll.getStyleClass().add("primary"); exportAll.setMaxWidth(Double.MAX_VALUE);
        HBox presets = new HBox(8, savePreset, loadPreset);
        savePreset.setMaxWidth(Double.MAX_VALUE); loadPreset.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(savePreset, Priority.ALWAYS); HBox.setHgrow(loadPreset, Priority.ALWAYS);
        VBox panel = new VBox(10, section("DETAIL LEVEL"), detail, section("COLORS"), colorMode,
                new Label("Custom palette size (2–5)"), customColors, section("DETECTED PALETTE"), detectedPalette,
                removeBackground, spacer(), section("IMAGES"), navigation, exportAll,
                section("EXTRA FEATURE · PRESETS"), presets);
        panel.setPadding(new Insets(22)); panel.setPrefWidth(260); panel.getStyleClass().add("side-panel");
        editorRoot.setRight(panel);

        filename.getStyleClass().add("filename"); fileStatus.getStyleClass().add("muted");
        batchStatus.getStyleClass().add("muted");
        operationMessage.getStyleClass().add("muted"); operationMessage.setWrapText(true);
        fileProgress.setMaxWidth(Double.MAX_VALUE); batchProgress.setMaxWidth(Double.MAX_VALUE);
        HBox batchLine = new HBox(12, batchStatus, spacer(), cancelProcessing);
        batchLine.setAlignment(Pos.CENTER_LEFT);
        VBox footer = new VBox(5, filename, fileStatus, fileProgress, batchLine, batchProgress, operationMessage);
        footer.setPadding(new Insets(12, 24, 12, 24)); footer.getStyleClass().add("footer");
        editorRoot.setBottom(footer);
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
    }

    /** Phase 3, FR-001: accept OS file drops on both screens, extract dropped
     * Paths from the dragboard, and pass them to the controller. The controller
     * delegates decoding/ZIP to a worker. Handle an empty/nonfile drag and test
     * one, multiple and ZIP drops without heavy work on FX. */
    public void onDrop(Consumer<List<Path>> receiver) {
        throw new UnsupportedOperationException("TODO Phase 3: JavaFX file drop zone");
    }
    public void showEditor() { stage.getScene().setRoot(editorRoot); }
    public void showDrop() { stage.getScene().setRoot(dropRoot); }
    public Stage stage() { return stage; }
    /** Phase 5, FR-004: load the selected original without blocking FX; verify
     * error handling and fit it beside the vector preview. */
    public void showOriginal(Path path) { throw new UnsupportedOperationException("TODO Phase 5: original preview"); }
    /** Phase 5, FR-004/FR-010: display only a validated, current-generation SVG
     * through WebView. Parse useful dimensions for Zoom to Fit, disable scripts,
     * and test asynchronous load plus replacement by a newer result. FX only. */
    public void showSvg(String svg) {
        throw new UnsupportedOperationException("TODO Phase 5: current SVG preview");
    }
    /** Phase 5: mark a stale/missing result as pending rather than displaying the
     * previous image's SVG; preserve an understandable loading/error state. */
    public void clearSvg() { throw new UnsupportedOperationException("TODO Phase 5: pending vector state"); }
    /** Phase 5, FR-005: Zoom In/Out changes presentation, never tracing. Clamp
     * scale reasonably and test bounds while WebView is loading. */
    public void setZoom(double value) { throw new UnsupportedOperationException("TODO Phase 5: preview zoom"); }
    private void applyZoom(double value) { throw new UnsupportedOperationException("TODO Phase 5: bounded WebView zoom"); }
    public double zoom() { return zoom; }
    /** Phase 5, FR-005: fit the current SVG dimensions inside vectorPane, account
     * for pane resize, and defer fit until WebView load completes. Test both tiny
     * and large SVG and a user zoom override. FX only. */
    public void zoomToFit() {
        throw new UnsupportedOperationException("TODO Phase 5: Zoom to Fit");
    }
    private void applyFitZoom() {
        throw new UnsupportedOperationException("TODO Phase 5: recompute fit after resize");
    }
}
