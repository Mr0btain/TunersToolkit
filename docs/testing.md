# Testing

## Running tests

```bash
./gradlew :core:jvmTest                                   # core: formats, logging, results (most tests live here)
./gradlew :core:jvmTest --tests "*.DiagnosticsTest"       # one class
./gradlew :composeApp:desktopTest                         # app tests (commonTest + desktopTest)
```

CI (`.gitea/workflows/ci.yml`, copied to `.github/workflows/ci.yml`) runs both test tasks and then builds web, desktop
and Android on every pull request and push to `main`. GitHub's copy also compiles iOS on a Mac runner.

## Where tests go

- **`core/src/commonTest`**: anything that doesn't need Compose. Parsers, writers, conversions, `TkResult`,
  `Diagnostics`, `AppLogger`. Keep fixtures small and hand-written, next to the test in `commonTest/resources`, and
  build bigger ones in code.
- **`composeApp/src/commonTest`**: app logic that lives in `composeApp`, like `WindowSizeClass`.

## Real ECU files

Real ECU binaries and A2L files are usually OEM-copyrighted, so they are **never committed**. Keep them in
`testdata/local/` (git ignored). For example, the 07K files the old test project opened go in:

```
testdata/local/07k/07K906032BR_5703.A2L
testdata/local/07k/07K906032BR_5703.hex
```

`:composeApp:run` passes that folder to the app (`tunerstoolkit.testdata`), so the drawer's 07K test entry
(`components/testing/Open07kTestProject.kt`) can load them on desktop. Other platforms log a warning instead. Tests
that use real files have to skip themselves when the files aren't there, so CI and other machines still pass.
