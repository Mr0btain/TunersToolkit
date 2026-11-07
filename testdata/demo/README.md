# TT-DEMO-1

A made up engine controller for tests and for trying the app: `tt-demo-1.hex` (the image) and `tt-demo-1.a2l`
(its description). Nothing here comes from a real ECU, so it's free to commit, change and share.

Both files are made by `DemoEcu` (`core/src/commonTest/.../testing/DemoEcu.kt`), and `DemoFilesTest` fails if they
drift from it. After changing the generator, rewrite them with:

```bash
./gradlew :core:jvmTest --tests "*DemoFilesTest" -DregenerateDemo=true
```

What's in it: a 32K flash segment at 0x00800000 (code, then calibration from 0x00804000) and a 256 byte boot
segment at 0, little endian. Shared axes (`RpmAxis16`, `LoadAxis12`, `PedalAxis10`), maps with their own axes
(`IgnitionBase`, `VolumetricEfficiency`) and shared ones (`BoostTarget`, `LambdaTarget`), curves with standard,
shared and fixed axes, single values (bytes, signed and unsigned words, a long, an on/off verbal table), a value
block, an ASCII string, RAT_FUNC and LINEAR conversions, measurements, and functions grouping it all.

In the app, the drawer's **Open demo project** loads it (desktop, from this folder).
