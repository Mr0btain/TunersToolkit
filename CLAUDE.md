# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Tuners Toolkit is a Kotlin Multiplatform / Compose Multiplatform app for ECU calibration editing. It loads an ECU firmware image (Intel HEX) plus its ASAM MCD-2 MC description (`.A2L`), decodes the calibration maps (values, curves, maps) from the binary, and lets the user browse and edit them in a hex/ASCII editor and in map grids. Targets: **desktop (JVM), Android and web (wasmJs)**, plus iOS when built on a Mac.

The app is mid-overhaul. **`docs/PLAN.md` is the roadmap** (phases, target architecture, file formats, UI kit). Check it before larger changes, and tick its boxes as phases land. Package root everywhere is `org.mr0btain.tunerstoolkit`. Dependency versions follow TicketMan's (`github.com/Mr0btain/TicketMan`), since the two will share a UI kit.

## Commands

```bash
./gradlew :composeApp:run                    # desktop app (main: org.mr0btain.tunerstoolkit.MainKt)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun   # web app in a browser
./gradlew :androidApp:assembleDebug          # Android APK
./gradlew :composeApp:packageDistributionForCurrentOS   # desktop installer (Dmg/Msi/Deb)

./gradlew :core:jvmTest                      # core tests
./gradlew :core:jvmTest --tests "*.TkResultTest"     # one test class
./gradlew :composeApp:desktopTest            # app tests (commonTest + desktopTest)

# Quick compile checks
./gradlew :composeApp:compileKotlinDesktop :composeApp:compileKotlinWasmJs :composeApp:compileAndroidMain
```

The version every package is built as is `tunerstoolkit.version` in `gradle.properties`. All modules use a JDK 21 toolchain (`jvmToolchain(21)`); Gradle downloads it through the foojay resolver if it isn't installed. CI (`.gitea/workflows/ci.yml`, copied to `.github/workflows/ci.yml` for GitHub) runs both test tasks and the web, desktop and Android builds on every pull request and push to `main`. GitHub also compiles iOS on a Mac, with `continue-on-error` until it has passed once. There's no lint task. `docs/testing.md` covers where tests go and how to use real ECU files.

**Real ECU files never go in git** (usually OEM copyrighted). They live in git-ignored `testdata/local/`. On desktop, `readLocalTestFile()` reads from there, and the drawer's 07K test project (`components/testing/Open07kTestProject.kt`) uses it.

## Modules

- **`core`**: KMP library (android, jvm, wasmJs, iOS on Mac) with **no Compose**. It's for logic testable with plain `kotlin.test`. So far it holds:
  - `logging/AppLogger`: `e/w/i/d(tag, message, throwable?)`. Each line goes to the platform log through an `expect fun platformLog`, and into a ring buffer (`AppLogger.entries`, a `StateFlow`) for a future log screen. `d` only writes when `AppLogger.debug` is on. Use hierarchical constant tags: `private const val TAG = "Thing"`, then `"$TAG:Function"`. Never log file contents.
  - `error/TkResult`: a `TkResult<D, E : Error>` (`Success`/`Failure`) with `map`, `flatMap`, `onSuccess`, `onFailure`, `getOrNull`, `valueOr`. New code returns this instead of throwing.
  - `diagnostics/Diagnostic` and `Diagnostics`: parse, decode and export problems with severity, `SourceLocation` and object name. Parsers report these instead of dropping things silently.
  - A generated `BuildInfo.VERSION`.

  Per `docs/PLAN.md`, the file formats, the A2L model and decoding move here over Phases 1 and 3.
- **`composeApp`**: KMP Compose UI and today's app logic. It's built as an Android **library** (`com.android.kotlin.multiplatform.library`) plus `jvm("desktop")` and `wasmJs` (iOS only on macOS).
- **`androidApp`**: a thin Android application around `composeApp` (`MainActivity`). It sets `appContext`, which the Android `ProjectFilesManager` reads, before `setContent`. It also does release signing from the `TUNERSTOOLKIT_KEYSTORE*` env vars or Gradle properties.
- **`iosApp`**: Xcode project. It's not compiled on Linux.

## Architecture (current app, before the overhaul)

### State: one big `MainViewModel`
`MainViewModel.kt` holds nearly all app state as Compose `mutableStateOf` / `mutableStateMapOf` fields:
- parsed `a2lData`
- `hexDataSegments` (segment id → `HexSegmentData`)
- `mapData` and `mapListItems`
- project info
- editor `TextFieldState`s
- dialog visibility flags
- settings

`App.kt` creates it with `remember { MainViewModel() }` and passes it explicitly down through navigation, screens and parsers. Parsers write their results straight into it. Koin is set up but not used for it. The plan replaces this with small handlers.

### Navigation and adaptive layout
- `App.kt` picks `SideMenuDrawer` on desktop (`getPlatform().name == "Java"`) or on wide windows, and `DrawerNavigation` on compact widths.
- `navigation/components/NavigationHost.kt` defines the routes from `screens/Screens.kt` (MAIN, EDITOR, SETTINGS).
- Each screen comes as a `Compact*` / `Expanded*` pair, chosen by `components/screenconfig/AdaptiveScreen.kt`.
- Window size classes come from our own `components/screenconfig/WindowSizeClass.kt`, built on `LocalWindowInfo`. The chrisbanes multiplatform library crashes on Compose 1.12, so don't add it back.

### Data pipeline (in `composeApp`, moving to `core`)
1. **HEX**: `components/parsers/hex/ProcessHexFile.kt` sniffs Intel HEX. Then `intel/ParseIntelHex.kt` builds `HexSegmentData` maps that hold the data as **hex strings**, which the plan replaces with a byte-based `MemoryImage`.
2. **A2L**: `components/parsers/a2l/helpers/ParseA2l.kt` is a line-based `startsWith` dispatcher over `/begin` blocks. Each keyword has a package (`characteristic/`, `axis/`, `compumethod/`, `recordlayout/`, …) with a data class, an `*Opt` enum and a `build*`/`validate` pair, and the result is `A2lData`. Known bug: the `COMPU_VTAB` branch also catches `COMPU_VTAB_RANGE`. The plan replaces this with a tokenizer.
3. **Decode**: `components/parsers/a2l/ParseAndComputeA2lData.kt` → `components/calibrationdata/a2l/GetCharacteristicData.kt`, which dispatches on `CharacteristicType` and produces `MapDataValues`. CUBOID, CUBE_4 and CUBE_5 are stubs.
4. The decoded maps are aligned to the code segment and drawn as overlays (`components/maptextoverlay/`), with a grid popup (`components/mappopup/`).

### Hex editors
There are two text-field editors built on `androidx.compose.foundation.text.input` (`BasicTextField(state = TextFieldState)` + `InputTransformation`):
- the older one in `screens/editor/components/hexview` and `ascii`
- the work-in-progress one in `components/hextexteditor` (`HexEditor`, shown on the expanded main screen)

Both are kept in sync through `MainViewModel.updateHexEditorText` / `updateAsciiEditorText`. Both get replaced by the virtualized `TmHexView` in Phase 2. Don't invest in them.

### Platform-specific code (`expect`/`actual`)
- `Platform.kt` → `getPlatform()`. The name is "Java", "Android", "Web" or iOS.
- `components/datastorage/ProjectFilesManager.kt` (expect class):
  - **Desktop** saves under the OS user data dir (`<userDataDir>/TunersToolkit/projects/<name>/<version>/`, through JNA `DirectoryResolver`s), one JSON file per `ProjectFileType`.
  - **Android** uses `appContext.getDir("projects")`.
  - **Web** keeps projects in memory only, with settings in `localStorage`. OPFS is planned for Phase 2.
- `components/testing/LocalTestFile.kt` → `readLocalTestFile()`, desktop only.
- `components/datastorage/FilePicker.kt` is **common** and uses FileKit's `rememberFilePickerLauncher` (all files, since A2L/HEX have no MIME type on Android).
- Anything in `commonMain` must also compile for wasm, so use `Dispatchers.Default`, not `Dispatchers.IO`, and no `java.*`.

### Resources
Compose resources live in `composeApp/src/commonMain/composeResources/` (`drawable/`, `font/` with JetBrains Mono), accessed through the generated `tunerstoolkit.composeapp.generated.resources.Res`. Web page shell: `composeApp/src/wasmJsMain/resources/index.html`. Android launcher icons, strings and the manifest are in `androidApp/src/main/`.
