# Project memory — read this first, every session

This file loads automatically at the start of every Claude Code session in
this folder. It exists so that **both team members' AI sessions behave the
same way**, even if one of us opens this project and just starts typing
without a careful setup prompt. If you are an agent reading this: the rules
below are not suggestions, they are how this specific two-person team
project is run. Follow them even if the human's request doesn't repeat them.

## What this project is

SE233 Advanced Programming, Term Project I (Chiang Mai University), a
two-student pair assignment. Build a JavaFX desktop app that batch-converts
JPG/PNG (or a ZIP of them) into real vector SVG, modeled loosely on Vector
Magic. 15 points of the final grade; code due **October 13, 2026**, onsite
demo **October 14, 2026**. The authoritative brief and scoring rubric are in
[`docs/term-project-brief.pdf`](docs/term-project-brief.pdf) — if anything in
the other docs ever seems to contradict that PDF or something a lecturer said
in class, the PDF/lecturer wins; flag the conflict and ask rather than
silently picking one.

## Read these, in this order, before writing or explaining any code

1. [`docs/term-project-brief.pdf`](docs/term-project-brief.pdf) — the actual assignment and scoring rubric.
2. [`docs/REQUIREMENTS.md`](docs/REQUIREMENTS.md) — that brief normalized into requirement IDs (FR-xxx/NFR-xxx/C-xxx).
3. [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — target design, package responsibilities, per-area TODOs, open decisions/trade-offs.
4. [`AGENTS.md`](AGENTS.md) — working rules and the 9 implementation phases.
5. [`README.md`](README.md) — course-to-code map (which coursebook chapter/section applies to which file) and the skeleton-comment convention.

## The one rule that matters most: this is a skeleton, not a task queue

Every method in `src/main/java` that isn't yet implemented is
`throw new UnsupportedOperationException("TODO Phase N: ...")` with a
doc-comment explaining what to build, which phase it belongs to, and which
coursebook section/requirement it serves. **Do not implement a TODO, advance
a phase, or write real logic unless the user's current message explicitly
asks for that specific piece of work.** A question like "what is phase 2" or
"explain the concurrency design" gets an explanation, not code. A vague
request like "finish the project" or "make it work" is not authorization to
implement everything — stop and ask which phase/piece to start with, per
`AGENTS.md`'s own phase-authorization rule. This applies equally whether the
human asking is either team member, and regardless of which one of us is
less experienced at prompting — don't infer more scope than was actually
asked for.

Do not copy implementation from `../complete-reference/` (a protected,
previously completed, fully-working reference build one directory up — see
its own `README.md` for how to run it as a behavioral oracle) unless the
user explicitly asks for that copy in this exact session. Its existence is
not standing authorization.

## The coursebook's own AI policy (read this before "just implement it")

The coursebook has a dedicated "AI Advisory" box in every one of chapters
1, 2, 3, and 5 (sourced from `se233v2026.pdf`, not paraphrased from memory —
re-read the box itself if a case is ambiguous). They are consistent with
each other and with the rule above, and they are specific enough to follow
literally for this project:

- **Ch.1 (Event-Driven/GUI, → Phases 2–3, 5):** "Do not give an AI the
  entire project and ask for the event connections to be written." "Do not
  ask an AI to link the controller to the view... At this stage of learning,
  it remains difficult to verify the correctness of generated code without a
  clear understanding of the underlying logic." "Do not accept AI-generated
  code for complex tasks like drag-and-drop without reading and
  understanding every step." Appropriate: explaining a concept *after* the
  human tried it manually; reviewing small, already-written sections.
- **Ch.2 (Exceptions, → the `ConversionException` boundary everywhere):**
  "Do not allow an AI to write the catch block for a network call without
  specifying which failure modes the application must distinguish." "Do not
  use an AI-generated stream pipeline without being able to describe the
  purpose of each intermediate and terminal operation." Appropriate:
  explaining the checked/unchecked hierarchy; verifying whether a specific
  exception is checked, against the JavaDoc.
- **Ch.3 (Concurrency, → `ProcessingCoordinator`/Phase 6, the hardest
  phase):** "The generation of a `Task` implementation via an AI tool must be
  avoided... The `Task` subclass must be constructed manually." Appropriate
  *only*: clarifying the distinction between `Task`/`Service`/
  `ScheduledService` after reading the framework docs.
- **Ch.5 (Testing, → Phase 8):** here AI is explicitly *more* welcome —
  "AI assistance is highly appropriate for boilerplate construction, such as
  generating a JUnit 5 test class skeleton... provided that the expected
  behavior is already completely understood." But: "Do not request an AI
  tool to 'write tests for this class' without explicitly specifying the
  behavior to be verified," and a generated test must trace to an actual
  requirement, not just restate what the implementation happens to do.

Net effect for this project: generating skeletons/TODOs/boilerplate/scaffolding
(what's already in this repo) is exactly the sanctioned use. Writing the real
event wiring, the real `Task`, the real catch-block logic, or "tests for
this class" on request, before the human has done it themselves, is not —
even if asked. If a team member asks for full implementation of a phase,
the right response is to point at the relevant advisory box and offer to
pair on it (explain, review small pieces, generate a test skeleton once
they've said what it should verify) rather than just writing it.

## Style and scope constraints (already decided, don't relitigate)

- No DI frameworks, factories, or enterprise layering. Interfaces are
  justified only by a real replaceable boundary (see `VectorEngine`).
- Stay inside coursebook chapters 1–5 (Event-Driven Development, Exception
  Handling, Concurrency, Debugging, Unit Testing). Chapter 6 (build tooling)
  is out of scope — `pom.xml` is already correct; don't add dependencies
  (e.g. Mockito) without the human asking for that specific change.
- Match the existing TODO-comment style exactly when adding or editing one:
  one-line contract, then `TODO Phase N, coursebook §x.y <topic>: <approach,
  pitfalls, what to test>`, then a `throw new UnsupportedOperationException`
  with a short matching label. Keep phase numbers and requirement IDs
  (FR-xxx) consistent with what's already used elsewhere in the file/package
  — don't invent a new numbering scheme.
- This repo has no CI and two humans push directly — before any
  `git push`/force-push/branch deletion, confirm with whoever is in the
  session; don't assume the other teammate's in-progress work is safe to
  overwrite.

## Verifying you haven't broken the skeleton

`./mvnw -DskipTests package` must always succeed, even with every TODO
unimplemented — it's the "does the skeleton still compile" check. `./mvnw
test` is expected to fail broadly until phases are implemented; that's the
intended state, not something to "fix" by loosening the tests.
