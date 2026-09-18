package se233.vectorize;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import se233.vectorize.view.VectorizeView;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/** Visual QA artifact and a real WebView SVG-rendering check on the project JDK. */
class UiSnapshotTest {
    @Test @Timeout(30) void editorHasOnlyThreeTopActionsAndRendersSvg() throws Exception {
        FxTestSupport.start();
        CompletableFuture<Path> snapshot = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                Stage stage = new Stage();
                VectorizeView view = new VectorizeView(stage); view.showEditor();
                BorderPane editor = (BorderPane) stage.getScene().getRoot();
                assertEquals(3, ((HBox) editor.getTop()).getChildren().size());
                view.filename.setText("1 / 2 • test.png");
                view.fileStatus.setText("READY");
                view.showSvg("<svg xmlns='http://www.w3.org/2000/svg' width='150' height='100'><path fill='#ee2211' d='M10 10L140 10L140 90L10 90Z'/></svg>");
                AtomicBoolean captured = new AtomicBoolean();
                Runnable captureAfterLoad = () -> {
                    if (!captured.compareAndSet(false, true)) return;
                    PauseTransition pause = new PauseTransition(Duration.millis(600));
                    pause.setOnFinished(event -> {
                        try {
                            WritableImage image = editor.snapshot(new SnapshotParameters(), null);
                            BufferedImage output = new BufferedImage((int) image.getWidth(), (int) image.getHeight(), BufferedImage.TYPE_INT_ARGB);
                            for (int y = 0; y < output.getHeight(); y++)
                                for (int x = 0; x < output.getWidth(); x++)
                                    output.setRGB(x, y, image.getPixelReader().getArgb(x, y));
                            Path file = Path.of("target/ui-editor.png"); Files.createDirectories(file.getParent());
                            ImageIO.write(output, "png", file.toFile());
                            stage.close(); snapshot.complete(file);
                        } catch (Exception e) { snapshot.completeExceptionally(e); }
                    });
                    pause.play();
                };
                view.vector.getEngine().getLoadWorker().stateProperty().addListener((o, before, state) -> {
                    if (state == Worker.State.SUCCEEDED) captureAfterLoad.run();
                    else if (state == Worker.State.FAILED) snapshot.completeExceptionally(new AssertionError("WebView failed to render SVG"));
                });
                if (view.vector.getEngine().getLoadWorker().getState() == Worker.State.SUCCEEDED) captureAfterLoad.run();
            } catch (Exception e) { snapshot.completeExceptionally(e); }
        });
        assertTrue(Files.isRegularFile(snapshot.get(20, TimeUnit.SECONDS)));
    }
}
