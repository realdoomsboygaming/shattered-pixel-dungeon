# Native Web Port

This branch targets the browser through TeaVM and libGDX.

## Build

```shell
./gradlew :web:webBuild
```

The generated static app is written to:

```text
web/build/dist/webapp
```

## Run Locally

```shell
./gradlew :web:webRun
```

By default this serves the app on port `8080`. Override it with:

```shell
./gradlew :web:webRun -Dweb.port=8081
```

## Current Shape

- `web` is a separate Gradle module that depends on the existing Java `core`.
- `WebLauncher` boots the libGDX TeaVM backend and preloads `freetype.js`.
- `WebPlatformSupport` provides browser-safe display, storage, vibration, and FreeType font support.
- Browser settings and save data use libGDX local file/preferences backends.
- Browser saves are written as uncompressed JSON because TeaVM's `Deflater` path can fail in the browser. Bundle loading still accepts existing gzip saves.
- Web scene switches out of `GameScene` wait until actor processing is idle so browser resize callbacks do not hit TeaVM coroutine suspension points.
- Vertex buffers reallocate when a partial tile update would exceed the currently allocated WebGL buffer.
- Update/news services use the existing debug service modules for now.

## Notes

- The published `gdx-teavm` Gradle plugin marker is not resolvable from Maven Central at `1.5.6`, so this module uses the manual `TeaCompiler` builder API.
- `DeviceCompat` now uses `Gdx.app.getType()` for platform checks. That avoids JVM-native `SharedLibraryLoader.os` access and keeps the shared code compatible with TeaVM.
