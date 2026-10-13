# Demo files, step by step

Every file is named `<segment>-<who>-<what>`. The segment number is the same
number as in `docs/demo/Vectorize-Demo-Script.docx` / `.html`, so if you sort
Finder by **Name**, the list is the order of the demo, and the name tells you
who drags it.

All of these were run through the real backend (ImageImportService,
ZipImportService, VectorizationService with AutoTrace, PresetService) on
13 Oct 2026. "You should see" below is what actually came out.

## Before you walk in

1. Copy this whole `demo-files` folder to the Desktop. In Finder: View > as List, sort by Name.
2. `09-Day-export-out` must be **empty**. Delete the SVGs from rehearsal.
3. Delete `05-Mona-preset-logo-style.properties` if it is left over from rehearsal (otherwise Save asks to replace it).
4. App open on the drop screen (`./mvnw javafx:run`), window maximised.

## The steps

| Seg | Time | Who | File(s) | Do | You should see |
|---|---|---|---|---|---|
| 1 | 0:00 | Day | none | Intro | Drop screen |
| 2 | 0:20 | Mona | `02-Mona-drop-a-logo.png` `02-Mona-drop-b-photo.jpg` `02-Mona-drop-c-transparent.png` | Select all three (Cmd+click), drag onto the window | Editor opens, label `1 / 3 • 02-Mona-drop-a-logo.png` |
| 3 | 1:05 | Mona | same 3 | Zoom In x2, Zoom Out, Fit. Next x2, Back x1 | `3 / 3 • 02-Mona-drop-c-transparent.png`, Next greyed out |
| 4 | 1:40 | Mona | `02-Mona-drop-a-logo.png` | Detail Low, High. Spinner 2 > 5 (stops at 5) > 3. Unlimited. Tick Remove Background | 5 swatches (white, navy, orange, yellow, teal). At 2 colours the logo is one flat colour, at 3 to 5 the real colours come back. Remove Background shows the checkerboard around the mountains |
| 5 | 2:40 | Mona | saves `05-Mona-preset-logo-style.properties` | Save (Preset) into demo-files with that name. Next. Load it | Message `Preset saved: ...` then `Preset loaded: ...`, controls change |
| 6 | 3:10 | Day | `06-Day-zip-images.zip` | Drag onto the editor | Total goes from 3 to 7. New images are `0_badge.png`, `1_leaf.jpg`, `2_house.png`, `3_sunset.jpg` (the app adds the number prefix). `readme.txt` inside the ZIP is skipped, and `pictures/` sub-folder is flattened |
| 7 | 3:40 | Day | (from the ZIP drop) | Point at the bottom bar, click Next / Zoom while it runs | `Preparing previews: x of 4`. Runs in parallel (7 images, Medium + Custom + 2) |
| 8 | 4:20 | Day | `02-Mona-drop-b-photo.jpg` and `02-Mona-drop-a-logo.png` | Back/Next between a JPG and a PNG | Both traced. You can also use `1_leaf.jpg` / `0_badge.png` |
| 9 | 4:45 | Day | `09-Day-export-out` | Export All, pick this folder | `Exporting: x of 7`, then 7 `.svg` files, e.g. `02-Mona-drop-a-logo.svg`, `0_badge.svg`. Open one in the browser, Cmd+Plus |
| 10 | 5:35 | Day | `10-Day-error-a-broken.jpg` `10-Day-error-b-notes.txt` `10-Day-error-c-good-star.png` | Drag **all three together** onto the editor. Then New Batch | Under the progress bar: `10-Day-error-a-broken.jpg: Unsupported image format: ...` and `10-Day-error-b-notes.txt: unsupported file type`. The star is added (total 8). New Batch goes back to the empty drop screen |
| 11 | 6:05 | Both | none | Code map in IntelliJ | |
| 12 | 6:45 | Mona | none | Wrap-up | |

## Watch out

- **Segment 10: always drop the good star together with the two bad files.**
  If *every* dropped file fails, `VectorizeController.importPaths` puts the error on the
  drop screen label (`dropStatus`), which is hidden while the editor is open, so
  nothing appears to happen. With one good file, the errors show in the editor.
- **Segment 7 can be fast.** Each image traces in about 0.3 s on this Mac, so the bar
  may finish before you point at it. Point at it right after the ZIP lands; if it is
  already done, say "it already finished, that is the parallel pool" and show the
  READY status per image.
- **Don't switch the photo to Unlimited.** The noisy photo becomes an ~8 MB SVG in
  Unlimited mode and the WebView gets slow. Use the logo for Unlimited (segment 4 does).

## 99-QA-backup (only if the lecturers ask)

| File | Drop it to show | You should see |
|---|---|---|
| `qa-zip-one-bad-member.zip` | One corrupt image inside a ZIP doesn't remove the good ones | `0_good-badge.png` and `2_good-leaf.jpg` added, plus `qa-zip-one-bad-member.zip → Unsupported image format: 1_broken-member.jpg` |
| `qa-zip-slip.zip` | Zip Slip protection | Whole ZIP rejected: `Unsafe entry path in qa-zip-slip.zip: ../../evil.png` |
| `qa-zip-no-images.zip` | ZIP with no images | `No supported images found in qa-zip-no-images.zip` |
| `qa-preset-broken.properties` | Load (Preset) with a bad file | `Preset field customColors must be between 2 and 5.` and the app keeps running |

Drop the ZIPs **on the drop screen** (after New Batch) or **together with a good image**
in the editor, for the same reason as segment 10. `qa-zip-one-bad-member.zip` is fine
anywhere, because it has good images in it.
