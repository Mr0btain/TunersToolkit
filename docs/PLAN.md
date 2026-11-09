# Tuners Toolkit: Overhaul Plan

Status: draft, 2026-11-07. This plan rebuilds Tuners Toolkit into a working, tested ECU calibration editor for desktop, Android and web. It reuses TicketMan's UI kit, module layout and CI.

---

## 1. Where we are today

What exists and works, at least partly:
- An Intel HEX parser and a line-based A2L parser covering CHARACTERISTIC, AXIS_PTS, RECORD_LAYOUT, MEASUREMENT, COMPU_METHOD/TAB/VTAB/VTAB_RANGE, GROUP, FUNCTION, HEADER, MOD_PAR, MOD_COMMON and IF_DATA.
- Decoding of VALUE, VAL_BLK, ASCII, CURVE and MAP characteristics, with RAT_FUNC, TAB and formula conversion.
- Saving projects with versions as JSON files on desktop.
- Two hex editors, neither finished.

Problems found while reading the code. The overhaul has to fix these, not carry them forward:

| Problem | Where | Why it matters |
|---|---|---|
| Binary data is held as hex **strings** (`HexSegmentData.data: Map<Int, String>`) and the editor works on formatted text | `parsers/hex/HexSegmentData.kt`, `MainViewModel` | Uses several times the memory, and every edit or lookup does string math. This is the root cause of the slow scrolling. |
| The hex editor loads a hard-coded `substring(0, 8000)` and swaps to `substring(6000, 14000)` near the bottom | `components/hextexteditor/HexEditor.kt` | There's no real pagination, so you can't scroll past about 14 KB. |
| The A2L parser matches lines with `startsWith`. `"/begin COMPU_VTAB"` is checked before `"/begin COMPU_VTAB_RANGE"`, so **every COMPU_VTAB_RANGE is parsed as a COMPU_VTAB** | `parsers/a2l/helpers/ParseA2l.kt:164` | Wrong conversions. A2L syntax isn't line-based anyway: blocks can share a line, and comments, strings and `/include` break this approach. |
| CUBOID, CUBE_4 and CUBE_5 return empty data, Motorola S19 is a 3-line stub, and `RecordLayout.kt:807` / `IfDataSource.kt:26` call `TODO()` and **crash** if reached | `calibrationdata/a2l/characteristic/*`, `parsers/hex/motorolahex` | Missing formats, and the app can crash. |
| `ECU_CALIBRATION_OFFSET` is parsed but never applied | `modpar/ModPar.kt:43` | Characteristics are read from the wrong addresses on ECUs that use it. |
| Errors are swallowed (`safeCall` failures dropped, `println` used for logging) | everywhere | The user never finds out why a map is missing. |
| A single `MainViewModel` holds everything, parsers write into it, and there is no package root | app-wide | Parsers can't be tested without the UI. |
| No tests. iOS is commented out. Dependencies are from 2024 (Kotlin 2.0, CMP 1.6.11, AGP 8.2) | build | |
| 10 `.DS_Store` files and `local.properties` are committed. The test fixture is a real VW 07K A2L and HEX (8.6 MB + 4.4 MB) | repo | Junk files, an SDK path leaks, and the fixture is likely OEM-copyrighted material (see Section 12). |

---

## 2. What comparable tools do (feature research)

These are the features of WinOLS (EVC), TunerPro, and OEM and professional tools (ETAS INCA/MDA, Vector CANape/vCDM, ATI Vision) that a user will expect. Sources: EVC product pages and HP Academy (links at the end), plus general knowledge of these tools.

**WinOLS (the main reference point)**
- Projects keep the **original read plus unlimited versions**. Changed bytes are shown in color, and you can restore any original value.
- **Hex, text, 2D and 3D views** of the same data, with switchable data width (8/16/32-bit, float32/64), signed or unsigned, LSB/MSB first, and hex/decimal display.
- **Map finder**: automatic search that suggests likely maps and their axes from patterns in the data. Users confirm or rename them, and search masks can name them automatically.
- **Map properties**: start address, size, data organisation, factor/offset, axes, units and limits. These are editable by hand when no A2L is available.
- **Map editing**: set, add, multiply, add %, interpolate, smooth, and copy/paste with Excel. Undo/redo.
- **Compare**: diff against the original or another file, and copy maps from one project to another (connect windows).
- **Checksum correction** through plugins (a paid add-on per ECU family). It runs automatically after edits.
- **Import/export**: binary, Intel HEX and Motorola S-record. Map packs (`.kp`), DAMOS/A2L import, CSV/Excel export of maps and the map list, and scripts.
- A **project database** with vehicle, ECU, software number and customer fields, which can be filtered and sorted.

**TunerPro**: a free editor. It uses **XDF** definition files (a map of a binary: tables, scalars, flags, axes, math) and ADX files for data logging. A large community library of XDFs exists for many ECUs.

**OEM and professional tools**: A2L-driven. They import and export calibration data as **DCM** (DAMOS text), **CDF/CDFX** (ASAM XML), **PaCo** (ETAS XML), **CVX** (ASAM CSV) and **Intel HEX / S-record**, and they also work with measurement logs (MDF).

**What we take from this**: our advantage over WinOLS is that we're multiplatform (including web), free, A2L-first, and use open formats. We don't need to clone WinOLS. We do need its core workflow: **open → identify → find or define maps → edit visually → compare → fix checksums → export**.

---

## 3. Target architecture

Copy TicketMan's layout (`settings.gradle.kts`, `composeApp/build.gradle.kts`, the `androidApp` wrapper, `gradle.properties` versioning) and its conventions from its CLAUDE.md. Use the package root `org.mr0btain.tunerstoolkit` everywhere.

```
TunersToolkit/
├── core/          KMP library (jvm, android, wasmJs, iOS on Mac). NO Compose. Pure logic:
│                  memory image, all file format readers/writers, A2L model + parser,
│                  characteristic decode/encode, conversions, checksums, map finder,
│                  diff, project model, AppLogger expect/actual, TkResult error type.
├── composeApp/    KMP Compose UI (android library + jvm("desktop") + wasmJs [+ iOS on Mac]).
│                  Depends on the shared org.mr0btain:uikit library (Section 4.1); ui/kit here
│                  only holds this app's own extensions (hex view, map table, charts), screens, handlers,
│                  platform file access (FileKit), storage.
├── androidApp/    thin Android application module (MainActivity).
├── iosApp/        Xcode project (only compiled on Mac / GitHub CI).
└── docs/          PLAN.md (this file), architecture.md, formats.md, testing.md, releasing.md
```

Why `core` is separate: every parser and writer becomes testable with plain `kotlin.test` on the JVM, and runs on wasm, with no UI involved. It also keeps a future CLI (batch convert, checksum fix) possible.

Key design decisions:

1. **`MemoryImage` replaces hex strings.** It holds sparse segments (`address: Long`, `bytes: ByteArray`) with a fast address lookup (binary search over segments), typed reads and writes (`u8/s16/u32/f32/f64` with byte order), and edit tracking. It is the single source of truth for binary data. Hex text is only ever produced **for the rows on screen**.
2. **Edits are patches.** A project version is the original image plus an ordered list of `Patch(address, oldBytes, newBytes)`. That gives undo/redo, the "changed bytes" view, diff and version history for free, and saves stay small.
3. **A2L model is separate from decode.** `A2lModel` (pure data from the parser) → `CalibrationObject` (resolved: address, layout, axes, conversion, dimensions) → `CalibrationValues` (read from the `MemoryImage`, and writable back to it).
4. **One internal definition model** (`CalibrationObject`) that every definition format maps into and out of: A2L, XDF, a definition we save ourselves, and map-finder results. The hex highlighting, map list, editors and exporters all only see `CalibrationObject`.
5. **State is split into handlers**, as in TicketMan: `ProjectHandler`, `ImageHandler`, `DefinitionHandler`, `EditorHandler` and `LogHandler`, each a small state holder owned by a slim `MainViewModel` and provided with Koin. Parsers return `TkResult<T, ImportError>` plus a diagnostics list. They never touch UI state.
6. **Heavy work runs off the UI thread** (`Dispatchers.Default`) with progress and cancellation. On wasm, work is chunked with `yield()` since there are no threads.

---

## 4. UI: adopt the TicketMan kit and look

Goal: the same look and feel as TicketMan. That means a glass nav rail on wide windows and a bottom bar on phones, the `TmBackdrop` glow, solid `TmSurface` cards, and the Bench, Copper and Mono palettes in dark and light, using Barlow, Atkinson Hyperlegible or the system font, with JetBrains Mono for all numbers and hex.

### 4.1 One kit, one source: a shared `ui-kit` library

The kit is **not copied**. It moves out of TicketMan into its own repository (working name `Mr0btain/ui-kit`), and both apps depend on it.

**Repository layout**
```
ui-kit/
├── kit/           KMP Compose library: android, jvm("desktop"), wasmJs, iosArm64/iosSimulatorArm64
│                  package org.mr0btain.uikit (theme, foundation, buttons, inputs, selection,
│                  search, containers, feedback, data, navigation, shell, gallery)
│                  + composeResources/font (Barlow, Atkinson Hyperlegible, JetBrains Mono, OFL licences)
├── kit-images/    optional module: TmImage/TmImagePager/TmImageView/TmPhotoEditor/TmAvatar (pulls in Coil),
│                  so apps without photos (Tuners Toolkit) don't carry Coil
├── kit-rules/     plain JVM library: the UiKitRulesTest checks as a function,
│                  assertKitRules(sourceRoot, kitPackage), which each app calls from its desktopTest
├── gallery-app/   tiny desktop + wasm app that shows the gallery, for working on the kit by itself
└── .gitea/.github workflows: test, render the gallery to PNGs, publish on v* tags
```

**What goes in the kit, and what stays in each app**
- **Into the kit**: anything that isn't about one app's domain. That is theme tokens (`TmColors`, `TmPalettes`, `TmFonts`, `TmTypography`, `TmShapes`, `TmSpacing`), foundation (backdrop, glass, surfaces, interaction, control sizes, scrollbar with its `expect`/`actual`s), buttons, generic inputs (text, password, field frame/pair), selection, search, containers, generic feedback (badge, notice, snackbar, empty and loading states), generic data (table, stat tile, key-value rows), navigation (rail, bottom bar, top bars, stepper), the **app shell** (`WideAppShell`/`PhoneAppShell`), and the gallery framework.
- **Stays in TicketMan**: `TmCustomerForm`, `TmMessageConsent`, `TmSignature`, PIN and money inputs (`LocalCurrencySymbol`), `TmQrCode`, `TmScheduleBlock`, `TmPrintPreview`, ticket status badges and pickers, `TmLocationBadge`, `TmEditNoteDialog`. These become **app-side kit extensions** in `org.mr0btain.ticketman.ui.kit`, built only from kit primitives, so the rules still hold.
- **Stays in Tuners Toolkit**: `TmHexView`, `TmMapTable`, `TmMapChart2D/3D`, `TmAddressField`, `TmDataFormatPicker`, `TmDiffBadge` (step 3 below). If a later app needs one of them, it moves into the kit.
- **Domain-coupled pieces get generalised before they move.** For example, status badges take a `TmTone` and a label instead of a `TicketStatusType`, and the shell takes a list of `TmNavItem`s instead of TicketMan's `Destination` enum.

**How each app plugs in**
- `TmTheme(palette, font, dark, reduceTransparency, content)` stays the single entry point. Apps can **add** palettes and fonts (`TmPalettes.all + myPalettes`) without forking the kit.
- The kit's gallery takes extra sections from the app (`KitGallery(extraSections = ...)`), so each app's own components appear next to the shared ones.
- Compose resources are published with the library (`compose.resources { publicResClass = true; packageOfResClass = "org.mr0btain.uikit.resources" }`), so the fonts ship inside the artifact.

**How it's consumed: from source, at a pinned tag** (as built, 2026-11-07)
- Both libraries live in their own repos: `Tm-AppLogger` and `Tm-UI-Kit` (Gitea, with public mirrors at `github.com/Mr0btain/<name>`). The kit's modules are `tm-ui-kit` (package `org.mr0btain.uikit`) and `tm-ui-kit-rules` (`KitRules`, for each app's `UiKitRulesTest`), plus a `:gallery` desktop app. The logger is `tm-applogger` (package `org.mr0btain.applogger`), and the kit depends on it for `TmLogTerminal`.
- Each app's `settings.gradle.kts` `includeBuild`s each library from the first of: `<sharedLibsDir>/<name>` (a Gradle property, for checkouts kept elsewhere), `../<name>`, or `.deps/<name>`, cloned from the GitHub mirror at the tag pinned in `settings.gradle.kts` on the first build. Gradle swaps in the source for the `org.mr0btain:*` coordinates in `libs.versions.toml`.
- Why not Maven: Gitea's registry needs a login even to read, and so does GitHub Packages. A release published from Linux would also have no iOS klibs. Building from source needs no credentials, works on every target (iOS on any Mac), and a local checkout's changes show up immediately. Maven Central (`io.github.mr0btain`) is still possible later, if release builds get slow.
- **Releasing a library**: bump its version in `gradle.properties` and push a matching `v*` tag (its CI checks they match). Then each app changes the tag in `settings.gradle.kts` and the version in `libs.versions.toml`.

**Considered and rejected**
- *Git submodule*: close to what we do, but the pinned commit is easy to forget and every clone needs `--recursive`. A tag in `settings.gradle.kts` reads better and clones itself.
- *A Maven registry*: see above.
- *Copying the kit*: two sources that drift apart. This is the problem we're solving.
- *Monorepo with both apps*: their release cycles and server code don't belong together.

**Migration order** (TicketMan stays green the whole time):
1. Create `ui-kit`. Move the generic components with history (`git filter-repo --path composeApp/src/commonMain/kotlin/org/mr0btain/ticketman/ui/kit`), rename the package to `org.mr0btain.uikit`, generalise the domain-coupled pieces, and add the gallery app and CI. Publish `0.1.0`.
2. In TicketMan, depend on `uikit`, delete the moved files, fix imports, keep its app-only extensions, and switch `UiKitRulesTest` to `kit-rules`. Its render tests should produce the same PNGs as before (a visual regression check).
3. Tuners Toolkit depends on `uikit` from day one, and adds its own domain components as extensions.
4. Keep the `Tm` prefix. Renaming every component would touch every screen in TicketMan for no gain. Read it as "theme module".

### 4.2 Using the kit in Tuners Toolkit

Steps:
1. Depend on `org.mr0btain:uikit` (composite build locally). Wrap the app in `TmTheme` and the shared app shell.
2. Call `assertKitRules` from `kit-rules` in `desktopTest`. It bans hard-coded colors, font sizes, shapes and `MaterialTheme.` usage outside kit code.
3. Add new kit extensions for this domain in `org.mr0btain.tunerstoolkit.ui.kit`. Each one gets an entry in the gallery through `extraSections`:
   - `TmHexView`: the virtualized hex/ASCII view (Section 5).
   - `TmMapTable`: an editable grid for MAP/CURVE/VAL_BLK with axis headers, a heat-map color scale (low/high tones from the palette), multi-cell selection, and changed-cell markers.
   - `TmMapChart2D` and `TmMapChart3D`: curve plot, and a 3D surface with drag-to-rotate (drawn on Canvas, no WebGL dependency).
   - `TmAddressField`: hex address input with validation.
   - `TmDataFormatPicker`: width, sign, endianness, hex/dec.
   - `TmDiffBadge`, `TmLogRow`, `TmImportReport`.
4. Keep window-size handling through `TmTheme`/`AppShell`. Drop the old `ui/theme`, `components/custom`, the Material3 drawer, and the `Compact*`/`Expanded*` screen pairs where one responsive layout will do.
5. Add render tests (`KitGalleryRenderTest`, `ScreenRenderTest` with wide and narrow variants) that save PNGs to `composeApp/build/...`, as TicketMan does.

Screens (destinations on the rail):
- **Projects**: list, search, filter by vehicle/ECU/software number. New-project wizard: pick a binary, optionally pick a definition (A2L/XDF/DCM), and fill in vehicle and ECU info, auto-filled from the A2L header and ECU ident strings.
- **Editor**: map list (tree by FUNCTION/GROUP, with search) | main pane (hex view or map table/chart, in tabs) | properties pane (object details, conversion, address, limits, comment).
- **Compare**: two images or versions side by side, with a changed-region list and per-map diff tables.
- **Versions**: timeline, add notes, restore, branch.
- **Log**: everything from Section 9, with filters, export, and copy for bug reports.
- **Settings**: theme, font, dark mode, reduce transparency, default data format, digits per row, kit gallery.

---

## 5. Hex view: smooth scrolling and pagination

**Approach: virtualize by row and never build a big string.**

- The row count is `ceil(segmentLength / bytesPerRow)`, which can be millions of rows. Use a `LazyColumn` (or a custom `LazyLayout`) with **fixed row height** and `key = rowIndex`. Each row draws its offset, hex and ASCII on **one Canvas** with a `TextMeasurer` that caches the glyph widths of a monospace font. That means no `TextField` and no `AnnotatedString` per row. The bytes come straight from `MemoryImage.read(rowAddress, bytesPerRow)`.
- Fixed row height gives O(1) jumps: `scrollToItem(address / bytesPerRow)`. Add a custom scrollbar (`TmScrollbar`) that maps thumb position to row index **proportionally**, so dragging across 4 MB is instant. Show an address tooltip while dragging.
- "Pagination" is just the lazy window plus prefetch. For segments too large for the lazy list's int indexing, or across multiple segments, use **virtual row space over all segments**, with gap rows that show "— gap 0x80000–0x9FFFF —".
- **Editing**: a cursor and selection model held as byte addresses (not text offsets). Typing nibbles writes `Patch`es to the image. Hex and ASCII columns share the selection. Keyboard: arrows, Page Up/Down, Home/End, Ctrl+G go to address, Ctrl+F find bytes or text, Ctrl+Z/Y.
- **Data width modes**: 8/16/32/64-bit and float views, signed or unsigned, LE/BE. Rendering groups bytes per mode, so the 8/16/32 toggle comes for free.
- Performance targets, measured with a benchmark test: no dropped frames at 120 Hz on desktop when flinging through a 4 MB image, and a jump to any address in under 16 ms. Memory is about 1× the image size plus definitions.

## 6. Highlighting maps in the hex data

- Build a **sorted interval index** of `AddressRange → CalibrationObject` (an interval tree, or a sorted array with a binary search for the first overlap) when definitions load. The rows on screen query it for `[rowStart, rowEnd)`, which is O(log n + k).
- Each object covers several ranges: the values block plus its axis blocks (AXIS_PTS / STD_AXIS inline data / NO_AXIS_PTS counters). Each range gets a **role** (values, x-axis, y-axis, counter or other) so the editor can tint values and axes differently.
- Colors: a hue picked from a stable hash of the object name, mapped onto the palette's tone colors. Fill at low alpha, an outline at the range's start and end, a stronger outline for the selected object, and changed bytes drawn on top in `TmColors.warning`.
- Interaction: hovering a highlighted byte shows a tooltip (name, cell [x,y], physical value, unit). Clicking selects the object. Double-click opens its map editor. Selecting a map in the list scrolls the hex view to it and flashes the range.
- A **minimap strip** beside the scrollbar shows where defined objects, changes and diffs sit across the whole image (like a code editor's overview ruler).
- Overlaps (bad A2Ls do this) are drawn striped and listed in the import report.

---

## 7. File formats

### 7.1 ASAP2 / A2L: aim for full ASAM MCD-2 MC coverage (1.5.x, 1.6, 1.7.x)

Replace the line-based parser with a **tokenizer + recursive-descent parser**:
- **Lexer**: idents, numbers (decimal, hex `0x`, float with exponent), strings with `\"` / `""` escapes, `/* */` and `//` comments, and `/begin` / `/end` tokens. Track line and column for every token so diagnostics point at the right place.
- **`/include`** with relative paths, resolved through a `FileResolver` interface. On web, the user picks a folder or a zip.
- **Encoding detection**: UTF-8 with or without BOM, UTF-16, or Latin-1 fallback.
- **A2ML**: parse the `/begin A2ML` grammar so IF_DATA blocks are kept as a typed tree. Unknown IF_DATA is kept **raw** so writing the file back loses nothing.
- **Grammar coverage checklist**, with a test for each:
  - Structure: `ASAP2_VERSION`, `A2ML_VERSION`, `PROJECT`, `HEADER`, `MODULE` (multiple modules), `MOD_PAR` (`MEMORY_SEGMENT`, `MEMORY_LAYOUT`, `ECU_CALIBRATION_OFFSET`, `CALIBRATION_METHOD`, `SYSTEM_CONSTANT`, `ADDR_EPK`, `EPK`, …), `MOD_COMMON` (`BYTE_ORDER`, `ALIGNMENT_*`, `DEPOSIT`, `DATA_SIZE`, `S_REC_LAYOUT`).
  - Objects: `CHARACTERISTIC` (VALUE, CURVE, MAP, CUBOID, CUBE_4, CUBE_5, VAL_BLK, ASCII), `AXIS_PTS`, `MEASUREMENT`, `BLOB`, `INSTANCE`, `TYPEDEF_CHARACTERISTIC/AXIS/MEASUREMENT/STRUCTURE/BLOB` (1.7), `STRUCTURE_COMPONENT`, `TRANSFORMER`.
  - Layouts: `RECORD_LAYOUT` with every keyword (`FNC_VALUES`, `AXIS_PTS_X..5`, `NO_AXIS_PTS_*`, `SRC_ADDR_*`, `RIP_ADDR_*`, `SHIFT_OP_*`, `OFFSET_*`, `DIST_OP_*`, `AXIS_RESCALE_*`, `NO_RESCALE_*`, `FIX_NO_AXIS_PTS_*`, `ALIGNMENT_*`, `RESERVED`, `STATIC_RECORD_LAYOUT`, `STATIC_ADDRESS_OFFSETS`), including **column- versus row-direction** and **inline versus external axes**.
  - Axes: `AXIS_DESCR` of type STD_AXIS, COM_AXIS, FIX_AXIS (`FIX_AXIS_PAR`, `_DIST`, `_LIST`), RES_AXIS, CURVE_AXIS.
  - Conversion: `COMPU_METHOD` IDENTICAL, LINEAR, RAT_FUNC, FORM (with the full formula grammar: `sysc()`, `X1..Xn`, bit ops, math functions), TAB_INTP, TAB_NOINTP, TAB_VERB, `STATUS_STRING_REF`. Plus `COMPU_TAB`, `COMPU_VTAB`, `COMPU_VTAB_RANGE` (fixing the bug above), `UNIT`, and `REF_UNIT`.
  - Organisation: `FUNCTION`, `GROUP`, `FRAME`, `USER_RIGHTS`, `VARIANT_CODING` (variant address selection).
  - Data types: UBYTE … A_UINT64/A_INT64, FLOAT16_IEEE/32/64, with `BYTE_ORDER` per object.
  - Optional keywords on every object: `BIT_MASK`, `BIT_OPERATION`, `MATRIX_DIM`, `NUMBER`, `EXTENDED_LIMITS`, `READ_ONLY`, `DISPLAY_IDENTIFIER`, `FORMAT`, `PHYS_UNIT`, `MAX_REFRESH`, `ANNOTATION`, `COMPARISON_QUANTITY`, `DEPENDENT_CHARACTERISTIC`, `VIRTUAL_CHARACTERISTIC`, `SYMBOL_LINK`, `MODEL_LINK`, `ENCODING`, `CALIBRATION_ACCESS`, `GUARD_RAILS`, `MONOTONY`, `STEP_SIZE`.
- **Lenient mode**: real A2Ls break the spec. Unknown keywords are kept and reported as warnings. A broken object is skipped with an error that points at its line, and the rest of the file still loads.
- **Writer**: write the model back to A2L. Round trip should be **semantically identical**, and byte-identical where we kept the original source text. This makes A2L export possible: a subset of objects, or definitions created by the map finder.
- **Performance**: parse the 8.6 MB fixture in under 2 s on desktop, streaming tokens and never `split("\n")` on the whole file.

### 7.2 Binary images (import and export)

| Format | Import | Export | Notes |
|---|---|---|---|
| Intel HEX | ✅ | ✅ | All record types 00–05. Checksum validation per record, reporting bad lines. Gaps kept. Export uses 16 or 32 bytes per line with the right 02/04 records. |
| Motorola S-record (S19/S28/S37) | ✅ | ✅ | S0 header, S1/S2/S3 data, S5/S6 count, S7/S8/S9 start. |
| Raw BIN | ✅ | ✅ | The user (or the A2L `MEMORY_SEGMENT`) gives the base address. Export fills gaps with a configurable fill byte (0xFF default). |
| VBF (Volvo), ODX/FRF/SGO (VW flash containers) | later | — | Read-only and possibly encrypted. Research item. |
| Zipped files | ✅ | ✅ | Open a `.zip` holding a bin/hex + A2L pair. |

Sniff the format from content, not the file extension.

### 7.3 Calibration data and definition exchange

| Format | What it is | Import | Export | Priority |
|---|---|---|---|---|
| **DCM** (DAMOS data exchange, ETAS/Bosch text) | Calibration **values** by name: `FESTWERT`, `FESTWERTEBLOCK`, `KENNLINIE`, `KENNFELD`, `GRUPPENKENNLINIE/-FELD`, `STUETZSTELLENVERTEILUNG`, `TEXTSTRING`, `FESTKENNLINIE/-FELD` | ✅ | ✅ | P1 |
| **CDF / CDFX** (ASAM CDF 2.x, XML) | The open ASAM standard for calibration **values**, plus quality/maturity metadata | ✅ | ✅ | P1 |
| **TunerPro XDF** (XML) | Definition file: tables, constants, flags, axes and math for a bin. Big community library | ✅ | ✅ | P1 |
| **CSV / clipboard (Excel)** | Maps and the map list | ✅ | ✅ | P1 |
| **CVX** (ASAM calibration values exchange, CSV) | Values | ✅ | ✅ | P2 |
| **PaCo** (ETAS XML) | Values | ✅ | ⏳ | P2 |
| **Our own definition JSON** (`.ttdef`) | Definitions made in-app (map finder / manual) without an A2L | ✅ | ✅ | P1 |
| WinOLS `.kp` / `.ols` | Proprietary and undocumented | ❌ | ❌ | Not planned. Offer XDF/A2L/CSV instead. |
| MDF4 / CSV logs | Measurement data for the future logging/graph feature | later | — | P3 |

> **Open question:** "the one open source format" from the request is assumed to be **TunerPro XDF** (open XML, free, widely used for definitions). If ASAM **CDF/CDFX** (the open standard for calibration *values*) was meant instead, both are in P1, so the plan works either way. Please confirm which one to do first.

Value exchange (DCM/CDF/CVX/PaCo) always goes **by name** through the active definition. Importing applies the values to the image as patches and reports any names that are missing, have the wrong dimensions, or are out of range. Exporting writes physical values with units.

---

## 8. Editing features to add, roughly in priority order

1. Map table editing with **physical** values. Writing goes back through the conversion (LINEAR/RAT_FUNC inverse, TAB_VERB lookup, FORM with a numeric inverse or kept read-only), with limit checks (`LOWER/UPPER_LIMIT`, `EXTENDED_LIMITS`) and rounding to the raw type.
2. Selection operations: set, +, −, ×, +%, interpolate between corners, smooth, and copy/paste with Excel (TSV).
3. Undo/redo across every editor (from the patch list).
4. **Versions**: original plus named versions with notes. Diff any two. Restore a single map or byte range from another version.
5. **Compare** another file: a list of changed ranges, mapped to object names where definitions exist. "Copy map from other file".
6. **Map finder**: heuristics over the image (monotonic axis runs followed by blocks of the right size, 16-bit LE/BE patterns, count-prefixed axes as Bosch/Siemens layouts do). Suggestions come with a confidence score. The user confirms one and it becomes a `.ttdef` object.
7. **Manual definitions**: create or edit a map's address, dimensions, data type, endianness, factor/offset and axes with no A2L. This is WinOLS's main workflow for files without a definition.
8. **Checksums**: a `ChecksumAlgorithm` plugin interface. Ship generic, user-configurable ones (sum8/16/32, CRC16/32 with any polynomial, ranges, store address and endianness), plus a "verify" view. ECU-specific modules come later, community-provided, behind the same interface. A warning appears on export when checksums haven't been run.
9. **ECU identification**: read the EPK (`ADDR_EPK` / `EPK`) and compare it with the A2L to catch a **mismatched A2L and binary** before showing values. Also scan for common ident strings (software or hardware numbers) to auto-fill project info.
10. Search: by object name or description, by value, by byte pattern, or by address.
11. 2D/3D views of maps (Section 4), with the original shown as a ghost overlay next to the modified data.
12. Later: project notes and tags, a batch CLI (`core` makes this cheap), MDF log viewer and overlay of logged operating points on maps.

---

## 9. Error logging and diagnostics

Copy TicketMan's pattern:
- `AppLogger.{e,w,i,d}(tag, message, throwable?)` with hierarchical constant tags (`"$TAG:ParseRecordLayout"`), `expect`/`actual` per platform. Desktop writes a **rotating log file** in the app data dir (5 × 2 MB). Android uses Logcat plus a file. Web uses the console plus an in-memory ring buffer.
- `TkResult<T, E : Error>` plus per-area error enums (`ImportError`, `DecodeError`, `ExportError`, `StorageError`), each with a readable message. No more silent `safeCall` drops: every failure becomes either a result or a **diagnostic**.
- **Import report**: every parse or decode run produces a list of `Diagnostic(severity, file, line, column, objectName, message)`. It is shown as a summary after loading ("1,842 objects, 1,830 decoded, 12 skipped") with a drill-down that jumps to the object or source line.
- **Log screen**: live log with level and tag filters, plus "Copy for bug report" (app version, platform, last N lines, and import summary, **never file contents**).
- Uncaught exception handler on desktop and Android writes a crash file and shows it on the next start.
- `d` (debug) logging is enabled only in debug builds (`isDebug()`, as TicketMan does).

---

## 10. Testing strategy

TicketMan's split, adapted:
- **`:core:jvmTest`** (most tests live here, and also run on wasm with `:core:wasmJsTest`):
  - Lexer and parser unit tests, one per A2L keyword family, using small handwritten `.a2l` snippets in `core/src/commonTest/resources/a2l/`.
  - **Golden tests**: parse the fixture → JSON snapshot of the resolved model, compared to a checked-in snapshot (review changes on purpose).
  - **Round trips**: A2L parse → write → parse equals the original. Intel HEX, S-record, BIN, DCM, CDFX, XDF, CVX and CSV import → export → import are identical, and `MemoryImage` contents are byte-equal.
  - Decode tests for every characteristic type × record layout variant × byte order × conversion. Values are computed by hand in the test.
  - Inverse-conversion tests: physical → raw → physical stays within half an LSB.
  - Malformed-input tests: truncated files, bad checksums, wrong record types, missing `/end`, recursive `/include`. The rule is **never crash, always diagnose**.
  - Fuzz and property tests for the HEX and S-record readers and the A2L lexer (random bytes must not throw).
  - Interval index, patch/undo and diff tests.
  - Performance smoke tests: the large fixture parses in under N seconds. Runs in CI but is reported, not flaky-failed.
- **`:composeApp:desktopTest`**: handler tests, `UiKitRulesTest`, kit gallery and screen render PNGs, and a hex view scroll benchmark.
- **Fixtures**: build **synthetic** fixtures with a small generator (`core/src/jvmTest/.../FixtureBuilder.kt`) that writes a matching A2L, HEX and DCM for every object type. Keep any real OEM files **out of git** (see Section 12) and load them only in local "real-file" tests that are skipped when the files are missing.

Commands (to put in CLAUDE.md once they exist):
```bash
./gradlew :core:jvmTest                                  # all format/parser tests
./gradlew :core:jvmTest --tests "*.A2lLexerTest"         # one class
./gradlew :composeApp:desktopTest                        # UI rules, renders, handlers
./gradlew :core:wasmJsTest                               # same core tests in a headless browser
```

---

## 11. Build, web target and CI/CD

**Build modernisation** (TicketMan versions as the baseline: Kotlin 2.4.x, CMP 1.12.x, AGP 9.x, Koin 4.x, FileKit 0.16, Haze 2.x):
- `settings.gradle.kts`: include `:core`, `:composeApp` and `:androidApp`, plus the foojay toolchain resolver. Type-safe project accessors.
- `composeApp`: `androidMultiplatformLibrary` plugin, `jvm("desktop")`, `wasmJs { browser() }`, and iOS targets only when building on macOS. `applyDefaultHierarchyTemplate()` with a `jvmShared` group for desktop and Android.
- The version lives in `gradle.properties` as `tunerstoolkit.version`. Release tags `v*` must match it.
- Replace the LWJGL/tinyfd and JNA directory code with **FileKit** for open and save dialogs on every platform (including the browser download on web) and the app data directory.
- Remove `mpfilepicker` and `calf-file-picker`, the iOS stubs that don't compile, the `.DS_Store` files, and `local.properties` from git.

**Web (wasmJs)**:
- Open files with FileKit (browser picker or drag and drop). Read them whole (`ByteArray`); files up to about 64 MB are fine.
- Store projects in **OPFS** (Origin Private File System), falling back to IndexedDB, behind the same `ProjectStore` interface. Offer export or import of a project as a `.ttproj` zip so work can move between devices.
- Save through a browser download. Parse on the main thread in chunks with `yield()` (or a Web Worker once Kotlin/Wasm worker support is solid).
- Deploy a static build (`:composeApp:wasmJsBrowserDistribution`) as a release artifact, and optionally to GitHub Pages or Gitea Pages.

**CI** (`.gitea/workflows/ci.yml`, mirrored to `.github/workflows/ci.yml`, on every pull request and on push to `main`):
1. JDK 21 + Gradle setup (`gradle/actions/setup-gradle` pinned to a commit, since `act_runner` can't resolve tags).
2. `:core:jvmTest`
3. Skia libs (`libgl1 libegl1 libx11-6 libfontconfig1`) → `:composeApp:desktopTest`
4. `:composeApp:compileKotlinWasmJs` + `:composeApp:compileAndroidMain` (Android SDK via `android-actions/setup-android`)
5. Upload test reports on failure, and screen renders always.
6. GitHub only: an iOS compile job on `macos-latest`.

**Release** (`.gitea/workflows/release.yml` + `.github/workflows/release.yml`, on a `v*` tag):
- A version check job (the tag must match `gradle.properties`).
- DEB (x64, plus arm64 later), MSI (Windows runner), DMG (GitHub macOS runners, x64 and arm64), tarball, an uber jar, a signed Android APK (keystore from secrets), and the web bundle zip.
- Artifact names follow TicketMan's pattern: `TunersToolkit-<OS>-<version>-<arch>.<ext>`.
- A publish job attaches everything plus `SHA256SUMS` to the Gitea or GitHub release.

---

## 12. Legal, licensing and safety notes

- **The bundled 07K A2L/HEX was probably OEM-copyrighted** (VW/Bosch). Done 2026-11-07: removed from every branch's history (`git filter-branch`, so every commit hash from June 2024 on changed) and from local copies, and replaced by the generated TT-DEMO-1 in `testdata/demo`. Old history stays on Gitea until the branches are force-pushed and the server's garbage collection runs.
- Pick a project licence. TicketMan has one, this repo has none. Add font OFL licences when copying the fonts.
- The README should say that modified calibrations can affect emissions and safety, and may be illegal for road use in some places. This is a common, non-preachy disclaimer in this category.
- Treat every imported file as untrusted. Cap sizes and nesting depth, put a depth limit on `/include` and refuse cycles, and harden the XML parser (no external entities in XDF/CDF/PaCo).

---

## 13. Phased roadmap

Each phase ends with green CI and a usable app.

**Phase 0: Cleanup and foundations**
- [x] Remove the `.DS_Store` files, untrack `local.properties`, remove the OEM fixture from the tree (moved to git-ignored `testdata/local/`; history rewrite only if approved).
- [x] New module layout (`core`, `composeApp`, `androidApp`), the `org.mr0btain.tunerstoolkit` package root, and the version catalog matching TicketMan's.
- [x] Desktop, Android and wasmJs all build. **iOS on a Mac is still unverified**: GitHub's iOS job runs with `continue-on-error` until it passes once.
- [x] `AppLogger`, `TkResult`, the `Diagnostic` model (in `core`, with tests).
- [x] CI workflow on both Gitea and GitHub.
- [x] Update CLAUDE.md to the new layout.
- [ ] Left for later phases: moving the existing `println`s and silent `safeCall` failures onto `AppLogger` and `Diagnostics` (Phase 1/3), and replacing the JNA directory code with FileKit (once the legacy screens and their `projects/` folders are gone, Phase 4). Saving web projects landed in Phase 2 (OPFS).

**Phase 1: Core data and binary formats**
- [x] `MemoryImage` with typed read/write, endianness, and patches/undo (`core/.../image/`).
- [x] Intel HEX, S-record and BIN readers and writers, format sniffing, and tests (including fuzz tests) (`core/.../formats/binary/`).
- [x] Synthetic fixture generator (`core/src/commonTest/.../testing/Fixtures.kt`).
- [ ] Left for Phase 2: the app still uses the old Intel HEX parser and hex-string segments. It moves to `BinaryImport` and `MemoryImage` together with the new hex view. The old parser records each segment's start as its first record's address minus its length; check whether the old A2L decode depends on that before swapping.

**Phase 2: Shared UI kit and the new hex view**
- [x] Create the `Tm-UI-Kit` and `Tm-AppLogger` repos and move the generic kit and the logger out of TicketMan (Section 4.1). TicketMan's move is on its `shared-ui-kit` branch.
- [x] Tag `v0.1.0` of both libraries and push them to the GitHub mirrors, so builds without a local checkout can clone them.
- [x] Tuners Toolkit's screens on the kit: `TmTheme`, the kit's new `TmAppShell` (Tm-UI-Kit 0.2.0; TicketMan still has its own shell and can move to it), `KitRules` in `UiKitRulesTest`, and `ScreenRenderTest`. The old screens live on as the Legacy place until Phases 3 and 4 replace them.
- [x] `TmHexView`: virtualized rows, Canvas drawing, address-based selection and editing, go-to address, find, data width modes, proportional scrollbar, and minimap.
- [x] Open files (FileKit) on every platform, an import report, export as Intel HEX, S-record or BIN (save dialog, or a download on the web), and a Log screen.
- [x] Projects with versions, saved on every platform: `core/.../project/` (`ProjectRepository` over a `ProjectStore`: `FileSystemProjectStore` with kotlinx-io on desktop, Android and iOS, `OpfsProjectStore` on the web, falling back to memory with a warning). A project keeps the source file untouched plus each version as patches from the original. The Files screen lists projects and versions; the editor saves in place (Ctrl+S), as a new version, or a plain file as a new project. Not yet: the `.ttproj` export/import, project info fields in the UI, and an "unsaved changes" prompt when opening something else.
- [x] Copy and paste in the hex view: Ctrl+C copies the selection as hex in the hex column and as text in the ASCII one (Ctrl+Shift+C the other way), Ctrl+V pastes over the bytes from the cursor in the same form, never past the end of the segment.

**Phase 3: A2L, done properly**
- [x] Lexer, parser and A2ML. Full keyword checklist from Section 7.1, each item with a test (`core/.../a2l/`). The parser is driven by a keyword table (`A2lSpec`), since keywords and names can't be told apart otherwise; it keeps unknown keywords with a warning, recovers from missing or mismatched `/end`, follows `/include` through an `A2lFileResolver`, and detects UTF-8, UTF-16 and Latin-1. `A2lModelBuilder` turns the tree into the typed model (names not yet resolved). 8.7 MB parses in about 0.3 s on desktop. A2ML is parsed into a grammar, and IF_DATA is kept as its source text with its tag checked against the grammar; reading IF_DATA into typed values waits for a feature that needs it (XCP settings).
- [x] The app on the new parser. The new app reads A2Ls only through `A2lImport` → `A2lResolver` → `CalibrationCodec` (`ui/definition/DefinitionHandler`), so the COMPU_VTAB_RANGE bug is gone there; the A2L is saved with the project (`definition.bin`) and opens with it. The legacy app went at the end of Phase 4, its parser with it.
- [x] Resolution into `CalibrationObject` (layouts, axes, `ECU_CALIBRATION_OFFSET`, variant coding) and decode/encode for **all** characteristic types including CUBOID, CUBE_4 and CUBE_5. `core/.../calibration/` is the format-neutral model (`CalibrationObject`, `Storage` of `RecordField`s, `CalibrationAxis` with its `AxisSource`, `Conversion`) and `CalibrationCodec`, which reads one into `CalibrationValues` and writes physical, raw, verbal and text edits back with limit and range checks. `A2lResolver` maps a module into it. Covered: every record layout keyword, static layouts, pointers (`PBYTE`..`PLONGLONG`), `INDEX_DECR`, `DEPOSIT DIFFERENCE`, STD/COM/FIX/RES/CURVE axes, offset/distance/shift axes, `BIT_MASK`, ASCII in UTF8/16/32, and every conversion including FORM (a formula evaluator with `sysc()`, inverted by `FORMULA_INV` or by search) and `STATUS_STRING_REF`. Left for later: INSTANCEs of TYPEDEFs, the `ALTERNATE_*` value orders (such objects are left out with an error), and `BIT_OPERATION`.
- [x] Interval index plus hex highlighting (Section 6), import report, and EPK check. `ObjectIndex` sorts every object's parts (`ObjectPart` with a role: values, axis points, the rest of the record) and answers overlap queries in O(log n + k). The hex view tints them by role in a color fixed per name, marks where each starts and ends, fills the selected one more strongly and shows objects on the scrollbar's minimap; the status bar names the object under the cursor and the cell and value there. The files screen reports objects, how many were read from the image, what was left out and why (filterable), overlaps, and `EpkCheck` (match, mismatch or not in the image). Hover tooltips weren't done: the status bar does that job, and touch has no hover.
- [x] A2L writer and round-trip tests. `A2lWriter.write` writes the parsed tree back (unknown keywords, A2ML and IF_DATA text included), and parsing that again gives the same tree: tested on snippets, the demo, the 8.7 MB generated A2L and any A2L in `testdata/local`. Comments and layout aren't kept, and includes come back inline. `A2lWriter.subset` writes chosen objects with everything they refer to, for sharing a few maps.

**Phase 4: Map editing**
- [x] Map list (FUNCTION/GROUP tree, search), `TmMapTable`, 2D/3D charts, physical editing with limits, selection operations, Excel copy/paste, undo/redo. The editor has the map list (`MapList`: by function, group or all, searching names and descriptions; `ObjectTree` and `A2lFolders` in core) beside tabs for the hex view and each open map. `TmMapTable` shows one 2D slice (pickers choose the slice of a cube) with axis headers, tints from low to high, changed cells marked against the image as opened, and takes typing, +/- steps of one stored unit, and spreadsheet copy and paste (TSV, comma decimals too). `MapEdit` does set, add, multiply, add %, interpolate (bilinear, by axis values when they rise steadily) and smooth (3 × 3); `writeAll` writes as one undo step, clamped to the limits, and says how many were clamped. `TmMapChart2D` draws rows as lines with the original dashed behind; `TmMapChart3D` draws the surface on a Canvas, turned by dragging. ASCII objects edit as text. Not done: editing axis points from the table (the codec can, `writeAxis`), and the extended limits as an option when editing.
- [x] Versions and compare screens. Versions lists the open project's versions as a timeline (open, compare with what's open, rename and note, start a new version from any of them, delete) and its info, which the A2L can fill in. Compare puts the open image against its original, a saved version or another file: bytes and stretches that differ, the maps that changed (each as this, other or the difference, with copy-from-other), and changes outside any map. The legacy app is gone: its screens, view model, parsers and Koin, with settings kept where they were.

**Phase 5: Exchange formats**
- [x] DCM, CDFX and XDF import and export (P1), CSV. **XDF** (`core/.../formats/xdf/`) reads constants, tables (axes as labels, in memory as shared AXIS objects, or linked to another table), flags and categories, with MATH equations through `Expression` (`^` is a power there; linear ones become exactly linear). Its offsets count from the image's first byte plus BASEOFFSET. It writes any definition XDF can express and lists what it couldn't (text, cubes, conversions other than linear or a one-variable formula, values not stored in a packed grid). `mmedtypeflags` are read as 0x01 signed, 0x02 LSB first, 0x04 column major, 0x10000 float: checked against TunerPro's behaviour as remembered, not a spec, so check them against real XDFs. Not read: equations with more than one VAR, strides, checksums and patches (reported). **Values** go by name through `formats/values/CalibrationData` (records out of the image; applied back as one undo step, clamped to limits, with names missing, sizes that differ and shared or fixed axes that differ all reported). **DCM** 2.0 reads and writes FESTWERT, FESTWERTEBLOCK, (FEST/GRUPPEN)KENNLINIE and KENNFELD, STUETZSTELLENVERTEILUNG and TEXTSTRING. **CDFX** writes CDF 2.0 SW-INSTANCEs; maps as a VG per x point holding that column's y values (cubes nest outer axes around that), which is the reading of the standard used here, so check it against a tool that reads CDFX. **CSV**: a map as a grid both ways (tab, semicolon or comma, comma decimals; text that would run as a formula is escaped), and the map list out. The files screen imports and exports values (all maps or only changed ones) and saves the definition as .ttdef or XDF; each map pane saves and reads a CSV. A small XML reader and writer (`formats/xml`) serves XDF, CDFX and PaCo.
- [x] Our own `.ttdef` definitions and manual map definition. `.ttdef` is the calibration model as JSON (`formats/ttdef/TtDef`), read and written exactly. Maps made by hand (`ManualMap`: value, curve, map or block at an address, its type, byte order, size, order, factor and offset, limits, and axes as positions, evenly spaced or in memory) come from the editor's New map button, can be changed or deleted, sit beside whatever definition is open (or alone), and are saved with the project as `additions.ttdef`. The app opens A2L, XDF and .ttdef alike (by content).
- [ ] CVX and PaCo (P2). PaCo is read through the CDFX reader (it keeps values in the same SW-INSTANCE structure), untested against a real PaCo file. CVX isn't done: no reliable description of its layout was found, so it waits for the standard or a sample file.

**Phase 6: Pro features**
- [x] Checksum plugin interface plus generic algorithms and a verify view. `core/.../checksum/`: generic algorithms (sums and XORs over bytes or words, any CRC up to 32 bits with the catalogue presets, Adler-32); checks whose result is stored as a value, a complement, both, or forced to a target through a correction word (CRCs solved over GF(2)); fixing as one undo step. ECU families are **modules**: `.ttsum` JSON for fixed checks (add a family without code), or Kotlin `ChecksumModule`s for families whose blocks have to be found by reading the image. Each module is marked verified, not verified or your own, with its source, and fixing with an unverified one asks first. The Checksums screen shows the module (found from the image, picked, or loaded), every check, Fix all and checks of your own; the module and your checks are saved with the project (through `ProjectAttachment`, the general way to save something with a project). Exporting warns when checksums are wrong. The demo ECU now has real checksums and its module is built in; the families researched (MED17/EDC17, ME7.x, Motronic, EDC15/16) are in `docs/checksums.md` with what's publicly known, waiting for real binaries to be verified against.
- [x] Map finder. `core/.../finder/MapFinder` looks for **counted records** (axis counts, then rising axis points, then values; both Bosch layouts, and curves) and **bare axes** (one or two rising runs followed by a block of exactly the right size, every length of the run tried, since a run can carry on into the values), in UBYTE/UWORD/ULONG axes, UBYTE/SBYTE/UWORD/SWORD/FLOAT32 values and both byte orders. Values are scored by smoothness against their 10th to 90th percentile span (which also picks row or column order); erased flash, constant blocks, and words bunched at both ends of their range (negative numbers read unsigned) score down; two counted axes back to back aren't taken for a curve. Overlaps keep the best, and memory the definition covers is skipped. The Find maps screen runs it in the background with progress and Stop, lists suggestions by confidence, previews one as a table and chart, and adds it as a map made by hand (raw values; scale it afterwards with Change). On the demo it finds the maps with their own axes exactly, and nothing in 512 KB of random bytes. Not found: maps with no axes next to them (ones on shared axes), and tables with gaps between values.
- [x] ECU ident scanning. `core/.../ident/EcuIdent` reads the image's printable text and matches the makers' numbering: Bosch hardware (0261/0281) and software (1037/1039) numbers, VW group part numbers (906/907/909/997), Siemens/Continental (5WK/5WP/A2C), Toyota (89661-) and Denso (275xxx-xxxx) numbers, ECU family names (MED17, EDC16/17, ME7, SIMOS, SID…), slash separated calibration IDs and EPKs, and VINs. Each finding has its address, count, maker, what the pattern means and a confidence; more patterns go in `EcuIdent.PATTERNS`. The Files screen lists them and fills in an open project's ECU and software number; saving a new project fills them in automatically; Versions has "Fill in from the image". The A2L's EPK check (Phase 3) covers the other half of this item.

**Phase 7: Some fixes/changes**
- [x] tabs in the editor should have a small X on the actual tab to close them quickly.
- [x] When the mouse hovers over things we should have tooltips and map names when hovering over things.
- [x] Make screen renders for each screen as a test, desktop and mobile, and check them to make sure everything works as it should.
- [x] Hex editor seems like the selected hex highlighting doesn't line up with where the text actually is, the further to the right end of the line you go the more it is offset, this needs checked with renders and fixed, and checked as screen size changes to ensure it still aligns when screen shrinks or expands.
- [x] Change the settings for the UI theme and stuff to match the way we do it in ticketman, see the example screenshot here: ![Screenshot From 2026-11-08 09-26-15.png](../../../../Pictures/Screenshots/Screenshot%20From%202026-11-08%2009-26-15.png) We want to keep those three example pictures of the theme to show people what they can expect.
- [x] Map overlays should have an outline, with rounded corners
- [ ] Test the entire application each tab and such in Desktop and mobile. that everything works looks right.
- [x] When selecting/clicking once something in the map list, we should just go to it in the hex editor and select the first hex inside it, double-clicking we can go to it in the hex editor but open it as a tab and go to that tab like it currently does on single click.
- [x] Generate icons for the app for everything. Matching the general overall look and feel. (`tools/make_icons.py`: the app icon for Android, the desktop window and installers, and the web favicon, plus Settings and Gallery icons of their own)
- [x] Allow users to add custom checksums they can use. (Keep as a module: your own checks, and any .ttsum you load, are kept in the app's store for every image, `ModuleLibrary`)
- [x] Setup deploy web to github pages automatically, and a release build for each OS as well. (`.github/workflows/pages.yml` on pushes to main; `release.yml` on v* tags drafts a GitHub release with the DMG, MSI, DEB, APK and a web zip)

**Phase 8: Release**
- [x] Release workflows producing every artifact (`release.yml` on GitHub and Gitea, files named by the build, `SHA256SUMS`; see `docs/releasing.md`).
- [ ] First tagged release `v0.1.0`.
- [x] **Rewrite the README** (features, screenshots from the render tests, supported formats table, build and run commands, disclaimer) once the app is in a good state.
- [x] `docs/` pages: architecture, formats (with spec references), testing, releasing.

---

## 14. Open questions for you

1. ~~Which format was "the open source one": TunerPro XDF or ASAM CDF/CDFX?~~ XDF first (2026-11-08); both are done in Phase 5.
2. ~~Should the OEM 07K files be removed from git history?~~ Yes, done 2026-11-07 (Section 12).
3. ~~Licence for the project (MIT / Apache-2.0 / GPL-3.0)?~~ MIT (2026-11-08): `LICENSE`, and the desktop installers carry it.
4. ~~Should web deploy anywhere automatically (GitHub Pages or Gitea Pages), or ship only as a release zip?~~ GitHub Pages from main, and a zip with each release (2026-11-08).
5. ~~Checksums: generic algorithms only, or ECU-specific modules?~~ Both (2026-11-08): generic algorithms now, and a module format so families can be added over time (`docs/checksums.md`).
6. Shared kit: is the Gitea instance publicly reachable, so it can host the `uikit` Maven artifacts for both CI systems? Or should we use Maven Central? And is `ui-kit` / `org.mr0btain.uikit` a good name?

---

### Sources consulted
- TicketMan repository (UI kit, module layout, CLAUDE.md, CI and release workflows): https://github.com/Mr0btain/TicketMan
- EVC WinOLS product pages: https://www.evc.de/en/product/ols/software/ , https://evc.de/winols
- HP Academy, WinOLS overview: https://hpacademy.com/winols/winols-software
- HP Academy, "What is an XDF file?": https://www.hpacademy.com/blog/what-is-an-xdf-file/
- TunerPro definition downloads: https://tunerpro.net/downloadBinDefs.htm
- ASAM CDF: https://www.asam.net/standards/detail/cdf/wiki/
- Vector vCDM (formats supported by OEM tooling): https://www.vector.com/en/product/vcdm/vcdmstudio/
