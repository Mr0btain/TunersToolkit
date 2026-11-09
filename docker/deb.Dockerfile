# Builds the desktop DEB and, beside it, a .tar.gz of the same app inside a container, so a runner can make them for
# another architecture through QEMU. The build names them for the system it runs on (TunersToolkit-Linux-<version>-arm64.deb):
#
#   docker buildx build --platform linux/arm64 -f docker/deb.Dockerfile --output type=local,dest=dist .
#
# The DEB bundles the Java runtime and Compose's native graphics of the machine that builds it, so emulating arm64
# makes an arm64 DEB. Everything runs emulated, Gradle and the Kotlin compiler included, so it's slow.
# The Gitea release workflow (.gitea/workflows/release.yml) uses it, see docs/releasing.md
FROM eclipse-temurin:21-jdk AS build

# jpackage makes the DEB with these, and settings.gradle.kts clones the shared libraries with git
RUN apt-get update \
    && apt-get install -y --no-install-recommends fakeroot binutils git \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /src
COPY . .

# The cache keeps Gradle and the dependencies between builds on the same Docker daemon. The tarball is made here
# rather than from the exported folder, so it keeps the files' permissions
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon :composeApp:packageDeb :composeApp:packageTarball \
    && mkdir /out \
    && cp composeApp/build/compose/binaries/main/deb/*.deb /out/ \
    && cp composeApp/build/compose/binaries/main/tar/*.tar.gz /out/

FROM scratch
COPY --from=build /out /
