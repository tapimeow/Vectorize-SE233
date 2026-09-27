# Vectorize Design System

## Overview

The visual style for the Vectorize JavaFX app. The colors come from the
ThreadWeave palette (indigo, amber, teal on a warm off-white), adapted to a
desktop image-to-SVG converter. The forum-only rules of ThreadWeave (nested
replies, indentation levels) do not apply here and were removed.

The project uses **no `.css` file** — nodes are styled with inline
`setStyle("-fx-...")` calls, as in the coursebook case study. Keep the
styles below as `private static final String` constants at the top of
`VectorizeView` so each color is written once.

## Colors

- **Primary** (#4338CA): Indigo — main actions (`Choose files`, `Export All`)
- **Secondary** (#D97706): Amber — highlights
- **Tertiary** (#0F766E): Teal — tool actions (`Save` / `Load` preset)
- **Background** (#FAFAF9): `dropRoot` and `editorRoot` background
- **Surface** (#FFFFFF): drop card, right settings panel, preview panes
- **Border** (#E7E5E4): light grey outline so white surfaces stand out on the background
- **Success** (#16A34A): status text for a finished image
- **Warning** (#D97706): `Cancel Processing` button, warning messages
- **Error** (#DC2626): status text for a failed image / error messages
- **Info** (#2563EB): status text for loading / in-progress messages

## Typography

- **Titles** (app title, preview headings "Original" / "Vector"): **Georgia**, bold.
  Georgia is a serif font installed on both macOS and Windows. (ThreadWeave
  uses Crimson Text, but it is not installed by default, and JavaFX silently
  falls back to a default font when a font is missing — so it would not show
  on the demo machine.)
- **Everything else**: the JavaFX default font.

## Style constants (JavaFX)

```java
// Design tokens from DESIGN.md
private static final String BG          = "-fx-background-color: #FAFAF9;";
private static final String SURFACE     = "-fx-background-color: #FFFFFF; -fx-background-radius: 12;"
                                        + " -fx-border-color: #E7E5E4; -fx-border-radius: 12;";
private static final String PRIMARY_BTN = "-fx-background-color: #4338CA; -fx-text-fill: white;"
                                        + " -fx-background-radius: 8; -fx-padding: 8 16;";
private static final String TOOL_BTN    = "-fx-background-color: #0F766E; -fx-text-fill: white;"
                                        + " -fx-background-radius: 8;";
private static final String WARN_BTN    = "-fx-background-color: #D97706; -fx-text-fill: white;"
                                        + " -fx-background-radius: 8;";
private static final String TITLE_FONT  = "-fx-font-family: 'Georgia'; -fx-font-size: 26; -fx-font-weight: bold;";
```

## Where each style goes

| Node | Style |
| --- | --- |
| `dropRoot`, `editorRoot` | `BG` |
| drop card, right settings panel, `originalPane`, `vectorPane` | `SURFACE` |
| `chooseFiles`, `exportAll` | `PRIMARY_BTN` |
| `savePreset`, `loadPreset` | `TOOL_BTN` |
| `cancelProcessing` | `WARN_BTN` |
| app title, preview headings | `TITLE_FONT` (smaller size for headings) |
| `fileStatus`, `operationMessage` (Phase 5/6) | `-fx-text-fill:` Success / Error / Info color |

## Do's and Don'ts

- Do use Primary (indigo) for at most one or two main actions per screen.
- Do use `SURFACE` for anything that should look like a card or panel.
- Don't use `getStyleClass().add(...)` for looks — without a `.css` file it does nothing; use `setStyle(...)`.
- Don't expect hover colors — inline `setStyle` has no `:hover`; that is fine for this project.
