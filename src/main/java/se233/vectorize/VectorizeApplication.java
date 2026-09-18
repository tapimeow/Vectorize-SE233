package se233.vectorize;

import javafx.application.Application;
import javafx.stage.Stage;
import se233.vectorize.adapter.AutoTraceVectorEngine;
import se233.vectorize.concurrency.ProcessingCoordinator;
import se233.vectorize.controller.VectorizeController;
import se233.vectorize.service.ImageImportService;
import se233.vectorize.service.VectorizationService;
import se233.vectorize.service.ZipImportService;
import se233.vectorize.view.VectorizeView;

public final class VectorizeApplication extends Application {
    private ProcessingCoordinator coordinator;
    private ZipImportService zip;

    @Override public void start(Stage stage) {
        zip = new ZipImportService();
        coordinator = new ProcessingCoordinator(new VectorizationService(new AutoTraceVectorEngine()));
        new VectorizeController(new VectorizeView(stage), coordinator, new ImageImportService(zip));
    }

    @Override public void stop() {
        if (coordinator != null) coordinator.close();
        if (zip != null) zip.close();
    }

    public static void main(String[] args) { launch(args); }
}
