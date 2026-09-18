// Team ownership: Shared - both review changes here. See README.md "Team split".
package se233.vectorize;

import javafx.application.Platform;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

final class FxTestSupport {
    private static final AtomicBoolean STARTED = new AtomicBoolean();
    private FxTestSupport() { }
    static void start() throws Exception {
        if (STARTED.compareAndSet(false, true)) {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(() -> { Platform.setImplicitExit(false); latch.countDown(); });
            if (!latch.await(10, TimeUnit.SECONDS)) throw new AssertionError("JavaFX did not start");
        }
    }
    static void run(Runnable action) throws Exception {
        CompletableFuture<Void> done = new CompletableFuture<>();
        Platform.runLater(() -> {
            try { action.run(); done.complete(null); }
            catch (Throwable error) { done.completeExceptionally(error); }
        });
        done.get(10, TimeUnit.SECONDS);
    }
    static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            CompletableFuture<Boolean> value = new CompletableFuture<>();
            Platform.runLater(() -> value.complete(condition.getAsBoolean()));
            if (value.get(2, TimeUnit.SECONDS)) return;
            Thread.sleep(50);
        }
        throw new AssertionError("Timed out waiting for JavaFX state");
    }
    static void capture(Stage stage, Path file) throws Exception {
        run(() -> {
            try {
                WritableImage image = stage.getScene().getRoot().snapshot(new SnapshotParameters(), null);
                BufferedImage output = new BufferedImage((int) image.getWidth(), (int) image.getHeight(), BufferedImage.TYPE_INT_ARGB);
                for (int y = 0; y < output.getHeight(); y++) for (int x = 0; x < output.getWidth(); x++)
                    output.setRGB(x, y, image.getPixelReader().getArgb(x, y));
                Files.createDirectories(file.getParent()); ImageIO.write(output, "png", file.toFile());
            } catch (Exception error) { throw new RuntimeException(error); }
        });
    }
}
