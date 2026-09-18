# Student project guidance — raster-to-vector JavaFX app

## Purpose and source hierarchy

This is the intentionally incomplete teaching/assignment version for two Advanced Programming students. Implement a JPG/PNG/ZIP → genuine vector SVG desktop app guided by the rubric, not a production editor. Read [`docs/term-project-brief.pdf`](docs/term-project-brief.pdf) (the official Chiang Mai University SE233 Term Project I assignment/scoring PDF), then `docs/REQUIREMENTS.md`, then `docs/ARCHITECTURE.md`, then this file before changing contracts. The PDF and live lecturer clarification outrank all three normalized docs; update the docs, not just your local understanding, before coding against new clarification.

The sibling `../complete-reference/` is a **protected, fully implemented preservation/reference version**. Do not modify it during student work. **Future Codex sessions must NOT copy implementation, algorithms, handlers, tests-as-solutions, or CLI command code from complete-reference into student unless the user explicitly requests that copying.** Its existence is not authorization to finish TODOs. Develop and test the student version independently. The original `lab/uploader` and its parent Git history are also preserved.

## Mandatory rubric behavior

- Import direct JPG/PNG or one ZIP safely; preserve good images when one fails.
- Center original and current SVG side by side; top **only** Zoom In/Out/Fit; right Detail, Colors, Remove Background, Back/Next, Export All. Exclude Edit Result and Hand-pick Settings.
- Low/Medium/High change actual output. Unlimited avoids a 2–5 palette cap; detected Custom defaults to 2 and maxes at 5. Remove Background yields transparent SVG.
- Multi-drop prepares Medium/Custom/2 color analysis and previews concurrently with responsive, visible feedback. Centralize `imageCount > 1 && MEDIUM && CUSTOM` in `ProcessingPolicy`; apply it to graded export too.
- Export All uses the OS folder chooser, off-FX final work, partial-failure reporting and normally openable standalone vector SVG. Never embed or rename raster data.
- One significant extra feature must be justified to lecturers; named presets are only a candidate.

## Architecture and thread rules

Keep the compact layered packages already scaffolded: domain/session, JavaFX view/controller, import/preprocessing/vectorization/export services, `VectorEngine` adapter, coordinator/policy, typed error. Interfaces are justified only by a replaceable boundary; avoid factories, DI frameworks and enterprise patterns. Controllers translate gestures; they do not perform ZIP, analysis, tracing, CLI or batch loops.

JavaFX Application Thread: chooser, controls, scene graph, WebView, result/status handoff. Bounded workers: decode, ZIP, palette, preprocessing, CLI, XML validation, writes. Use immutable settings snapshots, generation checks for stale preview rejection, honest per-image/aggregate stages, isolated failures, cancellation and shutdown. Show actionable user messages, retain technical causes. The Maven starter now targets JDK 21 / JavaFX 21.0.6; `./mvnw -DskipTests package` is the skeleton compile check. Full tests intentionally fail until TODOs are implemented. Do not install or bundle an external engine without verifying behavior and license.

## Phases and authorization

1. Requirements/architecture — documented.
2. Foundation/domain/view layout — build scaffold and basic data declarations exist; `VectorizeView`'s drop/editor screen layout (coursebook §1.1–§1.4 Event Driven Development) and detail policy are TODO.
3. Import/ZIP — TODO.
4. Color/background/vectorization/SVG gate — TODO.
5. Editor handlers/navigation/zoom/live preview — behavior TODO, once Phase 2's layout exists.
6. Graded batch concurrency/progress/cancellation — TODO.
7. Export All/final SVG writes — TODO.
8. Automated/manual rubric verification and packaging — existing tests serve as acceptance targets; TODO.
9. Additional feature/UX — preset service and controls scaffolded; TODO and lecturer approval.

**Current phase:** intentionally incomplete student skeleton. Do not automatically advance phases or implement a later phase merely because this file lists it. The user's next request must authorize scope. Preserve student work and never skeletonize the only working copy.

## Testing, pair work, documentation, Definition of Done

Trace each change to a requirement ID/rubric row. Test pure policy boundaries, ZIP safety, palette bounds, JPG/PNG real SVG/alpha, stale previews, navigation, parallel overlap, partial failures, export naming and external opening. Use flat art, photos, corrupt files and unsafe ZIPs; manually verify FX responsiveness. Tests should probe behavior, not restate code. Split the pair by coherent service/UI contracts, review each other's work and integrate small slices.

Maintain requirement IDs, decisions, open questions and phase status when behavior changes. Whole-assignment Done means all mandatory rubric paths demonstrated, valid external SVGs, no graded-run UI freeze, graceful recovery and a lecturer-accepted extra feature. Skeleton Done means package/class/method contracts and detailed TODO guidance remain, core algorithms/handlers are absent, the student tree compiles, and the protected reference stays intact. Future Codex sessions must inspect status, read these docs, work only in authorized scope, verify proportionally and stop at the phase boundary.
