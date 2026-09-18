package se233.vectorize;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import se233.vectorize.adapter.VectorEngine;
import se233.vectorize.concurrency.ProcessingCoordinator;
import se233.vectorize.exception.ConversionException;
import se233.vectorize.model.ImageDocument;
import se233.vectorize.service.VectorizationService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ParallelExportTest {
    @TempDir Path temp;

    @Test @Timeout(20) void defaultBatchFinalProcessingAndWritesOverlap() throws Exception {
        FxTestSupport.start();
        Path source = temp.resolve("input.png");
        ImageIO.write(new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB), "png", source.toFile());
        ImageDocument first = new ImageDocument(source, "same.png"), second = new ImageDocument(source, "same.png");
        AtomicInteger running = new AtomicInteger(), peak = new AtomicInteger(), completed = new AtomicInteger(), failed = new AtomicInteger();
        VectorEngine fake = (prepared, settings, ignored, cancelled) -> {
            peak.accumulateAndGet(running.incrementAndGet(), Math::max);
            try {
                try { Thread.sleep(300); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new ConversionException(ConversionException.Kind.CANCELLED, "Cancelled", interrupted); }
                return "<svg xmlns='http://www.w3.org/2000/svg' width='50' height='50'><path d='M0 0L40 0L40 40Z'/></svg>";
            } finally { running.decrementAndGet(); }
        };
        ProcessingCoordinator coordinator = new ProcessingCoordinator(new VectorizationService(fake));
        try {
            FxTestSupport.run(() -> coordinator.exportAll(List.of(first, second), temp,
                    status -> { completed.set(status.completed()); failed.set(status.failed()); }, message -> {}));
            FxTestSupport.await(() -> completed.get() == 2);
            assertEquals(0, failed.get()); assertTrue(peak.get() >= 2);
            try (var files = Files.list(temp)) { assertEquals(2, files.filter(path -> path.toString().endsWith(".svg")).count()); }
        } finally { FxTestSupport.run(coordinator::close); }
    }
}
