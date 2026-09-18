# Raster-to-vector assignment — student skeleton

This Maven/JavaFX JDK 21 project is intentionally incomplete. It preserves the package/class contracts and editor layout while leaving importing, ZIP safety, color analysis, background masking, tracing, asynchronous previews, navigation, concurrency, export and presets as guided TODOs. Begin with [requirements](docs/REQUIREMENTS.md), [architecture](docs/ARCHITECTURE.md), and [working rules](AGENTS.md). Implement phase by phase; do not treat a compiling scaffold as a functioning converter.

The sibling `../complete-reference/` preserves the previously completed application. It is **not** an automatic source of code for this folder; copying from it requires an explicit user request. The original `lab/uploader` remains untouched.

Use JDK 21 as the IntelliJ project SDK and Maven runner. The Maven wrapper is retained. To check skeleton compilation without running intentionally unmet acceptance tests:

```sh
./mvnw -DskipTests package
```

The tests in `src/test` are preserved behavior targets and are expected to fail until relevant TODOs are implemented; `./mvnw test` is the eventual verification. The UI layout can launch, but handlers and processing are deliberately absent. AutoTrace is not invoked by this skeleton adapter. Before choosing/installing an engine, verify genuine color SVG paths, transparent backgrounds, platform availability and licensing. If the lecturer clarifies ambiguous global/per-image settings, mixed parallel jobs, quality or extra-feature significance, update the docs before changing contracts.
