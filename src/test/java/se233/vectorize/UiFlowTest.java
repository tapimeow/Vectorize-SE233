package se233.vectorize;

import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import se233.vectorize.adapter.VectorEngine;
import se233.vectorize.concurrency.ProcessingCoordinator;
import se233.vectorize.controller.VectorizeController;
import se233.vectorize.model.DetailLevel;
import se233.vectorize.service.ImageImportService;
import se233.vectorize.service.VectorizationService;
import se233.vectorize.service.ZipImportService;
import se233.vectorize.view.VectorizeView;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** End-to-end UI → import → concurrent preprocess → preview/navigation/settings flow. */
class UiFlowTest {
    @TempDir Path temp;

    @Test @Timeout(30) void multiDropUsesParallelPreviewsAndRestoresPerImageSettings() throws Exception {
        FxTestSupport.start();
        Path first = image(temp.resolve("first.png"), Color.RED), second = image(temp.resolve("second.png"), Color.BLUE);
        AtomicInteger active = new AtomicInteger(), peak = new AtomicInteger(), traces = new AtomicInteger();
        VectorEngine engine = (prepared, settings, ignored, cancelled) -> {
            int running = active.incrementAndGet(); peak.accumulateAndGet(running, Math::max);
            try {
                try { Thread.sleep(500); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new se233.vectorize.exception.ConversionException(se233.vectorize.exception.ConversionException.Kind.CANCELLED, "Test trace cancelled", interrupted); }
                traces.incrementAndGet();
                return "<svg xmlns='http://www.w3.org/2000/svg' width='80' height='80'><path fill='#ee2211' d='M5 5L75 5L75 75Z'/></svg>";
            } finally { active.decrementAndGet(); }
        };
        ZipImportService zip = new ZipImportService();
        ProcessingCoordinator coordinator = new ProcessingCoordinator(new VectorizationService(engine));
        VectorizeView[] view = new VectorizeView[1];
        VectorizeController[] controller = new VectorizeController[1];
        try {
            FxTestSupport.run(() -> {
                view[0] = new VectorizeView(new Stage());
                controller[0] = new VectorizeController(view[0], coordinator, new ImageImportService(zip));
                controller[0].importPaths(List.of(first, second));
            });
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
            while (active.get() < 2 && System.nanoTime() < deadline) Thread.sleep(10);
            assertTrue(active.get() >= 2);
            FxTestSupport.run(() -> {
                assertTrue(active.get() > 0, "FX must accept interaction while workers trace");
                view[0].next.fire(); assertTrue(view[0].filename.getText().contains("2 / 2"));
                view[0].back.fire(); assertTrue(view[0].filename.getText().contains("1 / 2"));
            });
            FxTestSupport.await(() -> traces.get() >= 2 && view[0].filename.getText().contains("1 / 2") && view[0].fileStatus.getText().equals("READY"));
            FxTestSupport.await(() -> view[0].vector.getEngine().getLoadWorker().getState() == javafx.concurrent.Worker.State.SUCCEEDED);
            FxTestSupport.await(() -> view[0].zoom() > 2);
            FxTestSupport.capture(view[0].stage(), Path.of("target/ui-flow.png"));
            assertTrue(peak.get() >= 2, "Default Medium/Custom multi-drop must overlap tracing jobs");
            FxTestSupport.run(() -> {
                assertEquals(DetailLevel.MEDIUM, view[0].detail.getValue());
                assertEquals(se233.vectorize.model.ColorMode.CUSTOM, view[0].colorMode.getValue());
                assertEquals(2, view[0].customColors.getValue());
                double fitted = view[0].zoom(); view[0].zoomIn.fire(); assertTrue(view[0].zoom() > fitted);
                view[0].zoomOut.fire(); assertEquals(fitted, view[0].zoom(), 0.0001);
                view[0].zoomFit.fire(); assertTrue(view[0].zoom() > 0);
            });
            FxTestSupport.run(() -> { view[0].next.fire(); view[0].detail.setValue(DetailLevel.HIGH); });
            FxTestSupport.await(() -> view[0].filename.getText().contains("2 / 2") && view[0].fileStatus.getText().equals("READY"));
            FxTestSupport.run(() -> { view[0].back.fire(); assertEquals(DetailLevel.MEDIUM, view[0].detail.getValue()); });
        } finally {
            FxTestSupport.run(() -> view[0].stage().close());
            coordinator.close(); zip.close();
        }
    }

    private static Path image(Path file, Color color) throws Exception {
        BufferedImage image = new BufferedImage(80, 80, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics(); graphics.setColor(Color.WHITE); graphics.fillRect(0, 0, 80, 80);
        graphics.setColor(color); graphics.fillRect(10, 10, 60, 60); graphics.dispose();
        ImageIO.write(image, "png", file.toFile()); return file;
    }
}
