# Releasing

How the packages are versioned and built, by hand or by the release workflows, and the one-time setup the workflows
need.

## The version

`tunerstoolkit.version` in [`gradle.properties`](../gradle.properties) is the only place the version is written, and
every package is built from it:

| Package | Where it's used | Notes |
|---|---|---|
| Core | The generated `BuildInfo.VERSION` | Shown in Settings |
| Desktop installers | `packageVersion` in `composeApp/build.gradle.kts` | A `-rc1` suffix is dropped, since installers only take numbers. macOS refuses a version starting with 0, so 0.x app bundles say 1.0.0 inside, but the DMG's file name still has the real version |
| Android | `versionName` and `versionCode` in `androidApp/build.gradle.kts` | `versionCode` is `major * 10000 + minor * 100 + patch`, so 0.1.0 is 100 |

A release tag is `v` and the version (`v0.1.0`), and the workflows refuse a tag that doesn't match. Versions starting
with 0, and ones with a `-` suffix, are published as pre-releases.

## File names

The build names each file for the system that made it: `TunersToolkit-<OS>-<version>-<arch>.<ext>`.

| File | Task | Built on |
|---|---|---|
| `TunersToolkit-Windows-0.1.0-x64.msi` (and `-arm64`) | `:composeApp:packageMsi` | Windows |
| `TunersToolkit-macOS-0.1.0-arm64.dmg` (and `-x64`) | `:composeApp:packageDmg` | macOS |
| `TunersToolkit-Linux-0.1.0-x64.deb` (and `-arm64`) | `:composeApp:packageDeb` | Linux, with `dpkg-deb` and `fakeroot` |
| `TunersToolkit-Linux-0.1.0-x64.tar.gz` | `:composeApp:packageTarball` | Linux: the app unpacked, `bin/TunersToolkit` starts it |
| `TunersToolkit-Desktop-0.1.0.jar` | `:composeApp:packageUberJarForCurrentOS -PallPlatforms` | Anywhere: one jar for every desktop OS, run with `java -jar` on Java 21 |
| `TunersToolkit-Android-0.1.0.apk` | `:androidApp:assembleRelease` | Anywhere with the Android SDK. Ends in `-unsigned` without a signing key |
| `TunersToolkit-Web-0.1.0.zip` | `:composeApp:packageWebZip` | Anywhere: the web app, for any static host |

jpackage only builds installers for the system it runs on, so each installer needs a machine (or runner) of its own
OS and architecture. `docker/deb.Dockerfile` builds the arm64 DEB and tarball on an x64 machine through QEMU:

```bash
docker buildx build --platform linux/arm64 -f docker/deb.Dockerfile --output type=local,dest=dist .
```

The installers land in `composeApp/build/compose/binaries/main/<format>/`, the jar in `composeApp/build/compose/jars/`,
the web zip in `composeApp/build/dist/web/`, and the APK in `androidApp/build/outputs/apk/release/`.

## The icon

[`tools/make_icons.py`](../tools/make_icons.py) (Python with Pillow) draws the app icon: a tuning curve over a map's
cells, in the Bench palette. It writes every copy of it:

- `composeApp/icons/`, used for the MSI, DMG and DEB (`iconFile` in `composeApp/build.gradle.kts`)
- `tt_app_icon.png` in the Compose resources, for the desktop window
- the web favicons
- Android's adaptive icon layers, and PNGs for older launchers

To change it, edit the script and run `python3 tools/make_icons.py` from the repository root.

## Signing the APK

The release APK is signed when these are set, as environment variables or in `~/.gradle/gradle.properties`:

| Environment variable | Gradle property |
|---|---|
| `TUNERSTOOLKIT_KEYSTORE` (path to the `.jks`) | `tunerstoolkit.keystore` |
| `TUNERSTOOLKIT_KEYSTORE_PASSWORD` | `tunerstoolkit.keystorePassword` |
| `TUNERSTOOLKIT_KEY_ALIAS` | `tunerstoolkit.keyAlias` |
| `TUNERSTOOLKIT_KEY_PASSWORD` | `tunerstoolkit.keyPassword` |

Make the key once and keep it safe. Android only installs an update signed with the same key.

```bash
keytool -genkeypair -v -keystore tunerstoolkit-release.jks -alias tunerstoolkit -keyalg RSA -keysize 4096 -validity 10000
base64 -w0 tunerstoolkit-release.jks   # the value of the TUNERSTOOLKIT_KEYSTORE_BASE64 secret
```

## The workflows

| Workflow | When | What |
|---|---|---|
| `.github/workflows/ci.yml` and `.gitea/workflows/ci.yml` | Pull requests and pushes to `main` | Tests, and the web, desktop and Android builds. GitHub also compiles iOS |
| `.github/workflows/pages.yml` | Pushes to `main` | The web app on GitHub Pages |
| `.github/workflows/release.yml` | A `v*` tag | MSIs (x64 and arm64), DMGs (arm64 and x64), DEBs and tarballs (x64 and arm64), the jar, the APK and the web zip, with `SHA256SUMS`, as a GitHub release |
| `.gitea/workflows/release.yml` | A `v*` tag | The same apart from the DMGs and the arm64 MSI, as a Gitea release. The arm64 DEB is emulated, so it's slow |

You can run either release workflow by hand to build without publishing. Its inputs skip parts. For tags, setting
the repository variables `SKIP_WINDOWS`, `SKIP_MAC`, `SKIP_LINUX` (GitHub) or `SKIP_WINDOWS`, `SKIP_ARM64` (Gitea)
to `true` skips them. A release is published without the parts that were skipped, but not if anything failed.

### One-time setup

**GitHub**
- **Settings → Pages → Source**: "GitHub Actions".
- **Settings → Secrets and variables → Actions**: `TUNERSTOOLKIT_KEYSTORE_BASE64`,
  `TUNERSTOOLKIT_KEYSTORE_PASSWORD`, `TUNERSTOOLKIT_KEY_ALIAS` and `TUNERSTOOLKIT_KEY_PASSWORD`. Without them the APK is
  unsigned, with a warning.
- The arm64 runners (`ubuntu-24.04-arm`, `windows-11-arm`) are free for public repositories. A private one needs them
  on its plan, or `SKIP_*` set.

**Gitea**
- The same four secrets.
- A runner with Docker access for the arm64 DEB, and a Windows runner registered with the `windows-latest:host` label
  for the MSI. Set `SKIP_ARM64` or `SKIP_WINDOWS` to `true` while either is missing.

**Shared libraries.** CI clones Tm-UI-Kit and Tm-AppLogger from GitHub at the tags pinned in `settings.gradle.kts`.
Push a library's tag to its GitHub repository before pinning it here, or every GitHub build fails with
`Process 'command 'git'' finished with non-zero exit value 128`.

## Making a release

1. Set `tunerstoolkit.version` in `gradle.properties`, and commit it to `main`.
2. Check CI is green on that commit.
3. Tag and push it to both remotes:
   ```bash
   git tag v0.1.0
   git push origin v0.1.0     # Gitea
   git push github v0.1.0     # GitHub, under whatever name its remote has
   ```
4. Watch the release workflows. Each one publishes its release, with the generated notes on GitHub, once every part
   has built. Edit the notes afterwards if you want to.
