package se233.vectorize;

import javafx.stage.Stage;
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
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BatchFailureTest {
    @TempDir Path temp;

    @Test @Timeout(20) void oneWorkerFailureDoesNotDiscardTheOtherPreview() throws Exception {
        FxTestSupport.start();
        Path source = temp.resolve("image.png");
        BufferedImage image = new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(image, "png", source.toFile());
        ImageDocument first = new ImageDocument(source, "first.png"), second = new ImageDocument(source, "second.png");
        AtomicInteger calls = new AtomicInteger(), completed = new AtomicInteger(), failed = new AtomicInteger();
        VectorEngine engine = (prepared, settings, ignored, cancelled) -> {
            if (calls.incrementAndGet() == 1) throw new ConversionException(ConversionException.Kind.ENGINE, "Test engine failed");
            return "<svg xmlns='http://www.w3.org/2000/svg' width='40' height='40'><path d='M0 0L20 0L20 20Z'/></svg>";
        };
        ProcessingCoordinator coordinator = new ProcessingCoordinator(new VectorizationService(engine));
        try {
            FxTestSupport.run(() -> coordinator.preprocess(List.of(first, second), () -> {}, status -> {
                completed.set(status.completed()); failed.set(status.failed());
            }));
            FxTestSupport.await(() -> completed.get() == 2);
            assertEquals(1, failed.get());
            assertTrue(first.hasCurrentResult() || second.hasCurrentResult());
            assertTrue(first.error() != null || second.error() != null);
        } finally { FxTestSupport.run(coordinator::close); }
    }
}
