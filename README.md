# Raster-to-vector assignment — student skeleton

A JavaFX desktop app that turns dropped JPG/PNG images (or a ZIP of them) into
real, scalable SVG vector files — a small Vector Magic–style tool. This is the
**intentionally incomplete** two-student assignment version for SE233
Advanced Programming. Every class/method contract is in place; almost every
method body is a `throw new UnsupportedOperationException("TODO ...")` with a
comment telling you what to build, which implementation phase it belongs to,
and which requirement it satisfies. **Nothing here compiles into a working
app until you implement the TODOs.**

Read the docs in this order before writing any code:

1. [`docs/REQUIREMENTS.md`](docs/REQUIREMENTS.md) — the graded rubric, as functional/non-functional requirement IDs (FR-xxx/NFR-xxx).
2. [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — the target design, package responsibilities, and per-area implementation TODOs.
3. [`AGENTS.md`](AGENTS.md) — working rules, the 9 implementation phases, and what's off-limits (no DI frameworks, no copying from `../complete-reference/`).

This README is the map between **what the course taught you** and **where in
this codebase you apply it**. It does not restate the rubric or the
architecture — go read those documents for the *what*; this document is the
*where do I start*.

## 1. Setup

- JDK **21** (set it as the IntelliJ Project SDK *and* the Maven runner JDK — a newer default JDK is known to lack a module JavaFX's WebView tests need).
- The Maven wrapper is committed; you don't need Maven installed separately.

```sh
# Compile the skeleton only (this must always succeed, even with every TODO unfinished):
./mvnw -DskipTests package

# Run the app once enough of Phase 2 exists for the window to appear:
./mvnw javafx:run

# Run the test suite (these are acceptance targets — they fail until you implement the matching phase):
./mvnw test
```

`./mvnw -DskipTests package` is your "did I break the skeleton" check — run it
after every change, even early ones. `./mvnw test` is your "is this phase
actually done" check — expect it to stay mostly red for a while; that is the
intended state, not a bug.

## 2. How the skeleton is written

Every unimplemented method looks like this:

```java
/**
 * <one line: what this method must accomplish and which requirement it serves>
 * TODO Phase N, coursebook §x.y <topic>: <the approach to take, the pitfalls
 * to avoid, and what to test>.
 */
public ReturnType doSomething(...) {
    throw new UnsupportedOperationException("TODO Phase N: short label");
}
```

That comment is your spec for the method — not a hint to ignore. It tells you
three things: **what** (the contract already fixes the signature), **how**
(the approach, in the comment), and **where in the coursebook** to go relearn
the underlying technique before you write code. Delete the
`throw` line and the surrounding TODO prose only as you replace it with a
real implementation; don't leave dead TODO comments behind once a method is
done.

A few files are pure data/config and need no comment-following at all
(`ColorMode`, `ProcessingStatus`, `ConversionException`, `pom.xml`) — that is
deliberate, not an oversight.

## 3. Course-to-code map

The coursebook (*SE233 – Advanced Programming*, chapters 1–5) is the syllabus
this skeleton was written against. Chapter 6 (build tooling) is **not**
assumed here — `pom.xml` is already complete and you should not need to
change it. Use this table to find, for any topic you just learned in lecture,
the exact files where it applies.

| Coursebook chapter / section | Applies to (files) | Phase |
| --- | --- | --- |
| §1.1–1.2 GUI, JavaFX, Events | `VectorizeApplication`, `Launcher`, `VectorizeView` constructor | 2 |
| §1.3.1–1.3.3 Event Driven Programming, mouse/keyboard events, drag and drop | `VectorizeView.onDrop`, zoom/setting button wiring in `VectorizeController.wire` | 2, 3, 5 |
| §1.4.1–1.4.3 Case study: Model / View / Controller components | `model/*`, `view/VectorizeView`, `controller/VectorizeController` — this project's own M/V/C split mirrors this section directly | 2, 5 |
| §1.4.4 Flexible-size container | `VectorizeView.buildEditor` (HBox/VBox growth, `Priority.ALWAYS`, pane resizing) | 2 |
| §1.4.5 Drag-and-drop functionality | `VectorizeView.onDrop`, `VectorizeController.importPaths` | 3 |
| §1.4.6 Attributes propagation | Settings flowing from `VectorizeView` controls → `VectorizationSettings` → `ImageDocument` | 5 |
| §2.1 Checked/unchecked exceptions, §2.2 try/catch/finally, throwing, custom exceptions | `exception/ConversionException` and every `throws ConversionException` boundary (`ImageImportService`, `ZipImportService`, `SvgValidationService`, `VectorizationService`, `AutoTraceVectorEngine`, `ExportService`) | 3, 4, 7 |
| §2.3 Lambda expressions and streams (extra topic) | Optional simplification tool for `ColorAnalysisService`/`RasterPreprocessor` pixel-batch processing — not required, but idiomatic where you'd otherwise write manual loops over pixel arrays | 4 |
| §3.1 Concurrency purpose, GUI responsiveness, synchronization | Every "must not block the JavaFX Application Thread" rule in `AGENTS.md`/`ARCHITECTURE.md` §9 | 6, 7 |
| §3.2 JavaFX Concurrent Framework: `Worker`, `Task`, `Service`/`ScheduledService` | `concurrency/ProcessingCoordinator` (one-shot `Task` per job on a bounded `ExecutorService`, exactly as recommended in `ARCHITECTURE.md`) | 6 |
| §3.3 Example project: download manager (background work + progress + cancellation) | The closest coursebook analogue to `ProcessingCoordinator.preprocess`/`exportAll`: swap "download" for "trace one image" | 6 |
| §3.4 Case study: multithreaded work + Loading Indicator | `VectorizeController.showBatch`, `ProcessingCoordinator.BatchStatus`, `ProgressBar` fields in `VectorizeView` | 6 |
| §4.1 Debugging methodologies: probing, logging (levels, loggers, appenders) | Use these techniques *while implementing every phase*, especially the concurrency and CLI-adapter code in Phases 4/6, where a debugger alone won't show you interleaving — log stage transitions instead | all |
| §5.1–5.2 Software testing, JUnit 5 (assertions, naming conventions, test suites, mocking, reflection) | `src/test/java/se233/vectorize/*` — read these first; they are your target behavior, written in the JUnit 5 style from the coursebook | 8 |
| §5.3 TDD, Red-Green-Refactor | The intended workflow for every phase: run `./mvnw test -Dtest=<TestClass>`, watch it fail (red), implement the minimum to pass (green), then clean up (refactor) before moving to the next TODO | all |

`ProcessingPolicy` (the `imageCount > 1 && MEDIUM && CUSTOM` rule) doesn't map
to one coursebook section — it's this assignment's specific grading
condition, layered on top of the §3.2 concurrency mechanics.

## 4. Implementation order

Follow the 9 phases in `AGENTS.md` — they are ordered so each phase only
depends on already-implemented ones. In short:

1. Requirements/architecture — already written, just read them.
2. **Domain + view layout** — enums/records in `model/`, then `VectorizeView`'s two screens (§1.1–1.4). Nothing runs before this exists.
3. **Import/ZIP** — safe file intake and drag-and-drop wiring (§1.4.5, §2.1–2.2).
4. **Color/background/vectorization/SVG gate** — the actual image pipeline and the `VectorEngine` adapter (§2.2 for the exception boundary; the algorithms themselves are this assignment's own domain, not lecture content — `ARCHITECTURE.md` §6–8 has the design).
5. **Editor behavior** — wire the controls you laid out in Phase 2 to real state changes (§1.3, §1.4.3).
6. **Concurrency** — bounded `ExecutorService` + `Task` per job, the graded parallel policy, cancellation (§3.1–3.4).
7. **Export** — final SVG writes, reusing Phase 6's concurrency machinery.
8. **Testing pass** — get `./mvnw test` green phase by phase (§5.1–5.3); this is also when Chapter 4 debugging technique pays off most.
9. **One extra feature** — `PresetService` is scaffolded as a candidate; confirm significance with your lecturer before relying on it for the point (FR-019).

Do not skip ahead to a later phase "because it looks easy" — later TODOs
assume earlier contracts are real, not stubs.

## 5. Testing notes

- Tests use plain **JUnit 5** (`junit-jupiter-api`/`junit-jupiter-engine`), matching §5.2.3 of the coursebook. There is no Mockito dependency in `pom.xml`; where the tests need a fake collaborator (see `UiFlowTest`'s fake `VectorEngine`), they hand-write a lambda implementing the one-method `VectorEngine` interface instead of a mocking framework — cheaper than Mockito here because the interface is tiny. If you find a spot where a real mock genuinely pays for itself, the coursebook's Mockito walkthrough (§5.2.3, "Mocking") and `pom.xml` snippet are the reference — add the dependency deliberately, not by default.
- `FxTestSupport` is the shared JavaFX-toolkit-startup helper the tests use, equivalent to the `initJfxRuntime()` pattern from the coursebook's `GameLoopTest`.
- Run one test class at a time while you work a phase: `./mvnw test -Dtest=ProcessingPolicyTest`.

## 6. What not to do

- Don't add a DI framework, a factory hierarchy, or a service-per-interface — `AGENTS.md` bans this explicitly, and none of it is taught in the course.
- Don't copy from `../complete-reference/` (a protected, previously completed reference build) unless the user explicitly asks you to.
- Don't implement Chapter 6 (build/dependency management) concerns — the `pom.xml` is already correct for this assignment.
- Don't treat a green `./mvnw -DskipTests package` as progress — it only proves the skeleton still compiles, not that any TODO is implemented.
