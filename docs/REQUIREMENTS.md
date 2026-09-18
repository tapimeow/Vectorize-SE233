# Engineering requirements

Status: Student implementation specification; the adjacent `complete-reference/` is a protected preservation copy, not the state of this skeleton. `MUST` is assignment/rubric behavior, `SHOULD` is a recommended design or UX interpretation, and `MAY` is optional. IDs remain stable; later clarifications should amend text, not recycle IDs. The lecturer's assignment/rubric outranks this normalization.

## Functional requirements

| ID | Level | Requirement / observable outcome |
| --- | --- | --- |
| FR-001 | MUST | Accept drag-and-drop of one JPG, one PNG, or multiple JPG/PNG files (case-insensitive extensions); enter the editor when at least one valid image is loaded. |
| FR-002 | MUST | Accept a dropped ZIP containing supported images, extract those images safely, and treat them as a multi-image import. A ZIP with no usable images reports a clear failure. |
| FR-003 | MUST | Decode and validate supported raster inputs; reject unsupported/corrupted files without terminating the app. A bad file in a batch must not erase successfully imported files. |
| FR-004 | MUST | Center editor shows the selected original beside its **actual vectorized SVG result**; show pending/error status while a preview is unavailable. |
| FR-005 | MUST | The editor top panel contains **only** Zoom In, Zoom Out, Zoom to Fit; these controls work on the preview, and Zoom to Fit fits the available preview area. |
| FR-006 | MUST | Right panel offers Back/Next to visit loaded images and restores the selected image's settings, original, processing status, and corresponding preview. |
| FR-007 | MUST | Detail Level offers Low, Medium, High; changing it alters vector output and refreshes the preview. |
| FR-008 | MUST | Colors offers Unlimited mode, retaining source colors as far as the chosen tracing pipeline can reasonably support. It must not silently behave as fixed 2–5-color Custom mode. |
| FR-009 | MUST | Custom color mode performs automatic color analysis, defaults to 2 colors, and allows at most 5. The chosen count changes vector output. |
| FR-010 | MUST | Changes to detail, color mode/count, or Remove Background promptly trigger an up-to-date asynchronous preview; an obsolete result must never replace a newer configuration. “Instant” means immediate UI response/refresh scheduling, not zero computation time. |
| FR-011 | MUST | Remove Background checkbox causes SVG background transparency when enabled; source PNG transparency must be respected in the tracing pipeline. |
| FR-012 | MUST | Import and navigate a batch of images; show individual and overall processing feedback while expensive work runs without freezing the UI. |
| FR-013 | MUST | On multi-image import, concurrently preprocess each usable image to analyze colors and prepare a Medium-detail, Custom, 2-color preview, clearly indicating progress. |
| FR-014 | MUST | When processing multiple files under Medium + Custom, processing is concurrent/parallel. Custom count remains max 5/default 2. One policy location decides this behavior. |
| FR-015 | MUST | Export All opens the operating system's normal output-folder selector. Canceling the selector does not start export. |
| FR-016 | MUST | Export All processes any images needing a current final result, writes one valid `.svg` per successful image, reports progress/status, and keeps the UI responsive. Under FR-014's condition, final processing/export runs concurrently. |
| FR-017 | MUST | JPG and PNG both become standalone, normally openable SVG documents composed of actual vector geometry, not raster data renamed or embedded in SVG. |
| FR-018 | MUST | Isolate the external tracing tool behind a replaceable engine contract; missing tools, failed processes, failed tracing, and invalid SVG must surface as understandable errors. |
| FR-019 | MUST | Complete one significant additional feature outside the listed mandatory features for rubric demonstration; select and implement only after mandatory work is secure. |
| FR-020 | SHOULD | Preserve deterministic image order, identify filenames and current index, and disable Back/Next at bounds. |
| FR-021 | SHOULD | Preserve each image's settings and cached result while navigating; reuse a result only when its settings/source version match. |
| FR-022 | MAY | Cap the Custom selector below 5 when fewer meaningful source colors are detected; never exceed 5 or lose the documented 2-color default. |

## Non-functional requirements

| ID | Level | Requirement |
| --- | --- | --- |
| NFR-001 | MUST | JavaFX Application Thread stays responsive; all decoding, ZIP extraction, color analysis, tracing, SVG validation, and writes run off it. Scene graph/control changes run on it. |
| NFR-002 | MUST | Bound worker count, publish overall/per-image progress and outcome, isolate individual failures, and release workers/tool processes on cancellation or shutdown. |
| NFR-003 | MUST | Handle unsupported input, corrupt JPG/PNG/ZIP, empty ZIP, Zip Slip/unsafe entries, absent/failing CLI, invalid SVG, inaccessible output, write errors, and partial batch failures. Display concise user messages rather than raw stack traces; preserve causes for diagnostics. |
| NFR-004 | MUST | ZIP ingestion rejects traversal paths and controls extracted-entry count, size, and total size to avoid uncontrolled resource use; proposed limits are recorded in `ARCHITECTURE.md` and may be revised for grading machines. |
| NFR-005 | MUST | SVG output is parseable, has a valid SVG root/namespace and vector content, and is checked before preview/export. Do not trust a CLI exit code alone. |
| NFR-006 | SHOULD | Modular, maintainable, and testable separation of JavaFX presentation, state, file/ZIP I/O, image processing, concurrency, and external integration; abstractions earn their place. |
| NFR-007 | SHOULD | UI clearly distinguishes queued/running/ready/failed/cancelled, current preview freshness, and partial export success. |
| NFR-008 | SHOULD | Repeatable settings and color analysis produce predictable preview/export correspondence; tests cover representative and malformed inputs. |
| NFR-009 | SHOULD | The pair can explain each major class, thread boundary, algorithm choice, and rubric demonstration without enterprise-scale complexity. |

## Constraints and exclusions

- C-001: Java desktop/JavaFX application inside the Maven modular IntelliJ scaffold. The student build targets JDK 21 and JavaFX 21.0.6; FXML was starter scaffolding, not a lecturer requirement.
- C-002: Two students; prefer standard Java/JavaFX facilities and one real engine boundary over DI frameworks or many layers of indirection.
- C-003: The graded predicate is **image count > 1 AND Detail Level == MEDIUM AND Color Mode == CUSTOM**. Centralize it and demonstrate it on multi-drop preprocessing and export.
- C-004: Custom palette maximum **5**, default **2**. Low/Medium/High and Unlimited/Custom are typed settings, not loosely parsed UI strings.
- X-001: Vector Magic “Edit Result” is excluded.
- X-002: Vector Magic “Hand-pick Settings” is excluded.
- X-003: Traditional Download action is excluded in favor of Export All.

## Architectural interpretations and assumptions

- A-001: Settings are shared **defaults** at import, then copied per image so navigation restores edits; the assignment does not specify global versus per-image settings. See decision D-02 in `ARCHITECTURE.md`.
- A-002: Direct image drops and ZIP import yield the same ordered image-document list. The prompt explicitly says “one ZIP” and is silent about a mixed image+ZIP drop.
- A-003: “Pre-shown” means show each completed preview as soon as it is available (the selected image immediately), while remaining batch jobs progress. A busy indicator is shown before readiness.
- A-004: Unlimited means no imposed 2–5-color quantization; an engine may still approximate colors because raster-to-vector tracing is inherently lossy. Color fidelity is a test criterion, not a guarantee of every pixel color.
- A-005: A failed image remains visible with status/error; Export All writes successes and gives a partial-failure summary rather than aborting the batch. Export retry semantics need lecturer/input confirmation.
- A-006: “Instant preview” is asynchronous and supersedes stale jobs; actual tracing time may vary by image/tool.

## Acceptance criteria

| Scenario | Required evidence |
| --- | --- |
| JPG and PNG | Each yields parseable, externally openable SVG with paths/shapes and no embedded raster; originals remain visible. |
| Multi-drop/ZIP | Correct order/count; supported ZIP entries appear like direct files; empty/corrupt/unsafe ZIP gives clear feedback. |
| Editor | Side-by-side preview; top has only three zoom actions; Back/Next restore document-specific state. |
| Settings | Low/Medium/High outputs differ as intended; Unlimited and 2–5-color Custom are distinguishable; detected palette is presented; configuration updates show current result and ignore stale results. |
| Background | With Remove Background, SVG canvas is transparent and meaningful foreground remains; preview can use a checkerboard to prove transparency. |
| Preprocessing | Drop at least two images: both start Medium/Custom/2, color detection and previews run on bounded workers simultaneously, progress remains visible, UI remains interactive. |
| Export | OS chooser appears; successful results have unique `.svg` names and open normally; needed jobs run off FX thread and concurrently when predicate holds; progress/partial errors are visible. |
| Errors | Bad input/tool/write cases preserve application usability and identify affected image/action without raw stack traces. |
| Extra feature | One distinct significant feature is demonstrated and justified to lecturers. |

## Rubric traceability

| Rubric area / awarded level | Points affected | Requirements | Planned proof |
| --- | ---: | --- | --- |
| Vector format: JPG + PNG → SVG | 1 | FR-017, NFR-005 | Validate real geometry and external open on both types. |
| Zoom controls | 1 of 2 | FR-004, FR-005 | Three top controls operate correctly on preview. |
| Back/Next | second 1 of 2 | FR-006, FR-012, FR-021 | Multi-file navigation restores preview/state. |
| Detail + preview | 1 of 2 | FR-007, FR-010 | Three output levels and refreshed preview. |
| Remove Background | second 1 of 2 | FR-011 | Transparent SVG, visible checkerboard. |
| Unlimited colors | 1 of 3 | FR-008 | Unlimited output compared with constrained output. |
| Detected Custom, max 5, default 2 | second 1 of 3 | FR-009 | Palette analysis and bounded selector. |
| Instant configuration preview | third 1 of 3 | FR-010, NFR-001 | UI responds immediately; latest settings win. |
| ZIP handling | 1 | FR-002, FR-003, NFR-004 | ZIP images behave like multiple dropped files. |
| Preprocessing default | 1 of 2 | FR-013 | Colors, Medium, Custom/2, ready preview. |
| Concurrent preprocessing/feedback | second 1 of 2 | FR-013, FR-014, NFR-001, NFR-002 | Two+ simultaneous jobs and responsive progress. |
| Openable export | 1 of 2 | FR-015, FR-016, FR-017 | Folder dialog and external SVG open. |
| Parallel export/feedback | second 1 of 2 | FR-014, FR-016, NFR-001, NFR-002 | Parallel default batch, status and responsive UI. |
| Additional feature | 1 | FR-019 | Distinct feature demonstrated after mandatory criteria. |
| Subjective UX/UI | unnumbered | FR-004–FR-006, FR-010, FR-012, NFR-007, NFR-009 | Clear selected image, state, feedback, and recoverable failures. |

## Open questions / ambiguities

1. Is a mixed image+ZIP drop accepted, or must ZIP be the only dropped item? The brief explicitly lists one ZIP but not mixtures.
2. Should settings apply globally to all images or per image? The design adopts per-image copies of shared defaults; confirm if lecturers expect one global batch configuration.
3. How should the parallel predicate apply after users create **mixed per-image** configurations? The graded default batch is unambiguous; see D-03 and its conservative rule.
4. Are the selected 25-megapixel, 500-entry, 40-MB-entry, and 200-MB-total ZIP limits acceptable for demonstration, and which target platforms must run the tool?
5. What counts as acceptable Unlimited color fidelity, especially for photographs, and what vector visual quality is expected? A tracing engine inevitably approximates the source.
6. How should background be identified for photographs or multicolored/transparent edges? Is an edge-connected dominant-color heuristic acceptable, or is manual selection expected (the latter risks “Hand-pick Settings”)?
7. Does “instant” have a numerical response-time threshold, or is immediate asynchronous refresh with progress sufficient?
8. If an output filename already exists, should Export All replace, skip, or make a unique name? Design recommends unique naming without silent overwrite.
9. Will named settings presets, if implemented, count as the rubric's “significant” additional feature? Ask the lecturers before relying on that point.
