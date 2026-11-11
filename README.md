# VitaPluginsSource
Source for TonicBox's VitaLite plugins. Each plugin is its own Gradle subproject; install them from the Vita Hub tab in VitaLite.

| Plugin | What it does |
|--------|--------------|
| [VitaVR](vitavr/README.md) | Play in a VR headset and switch back to the desktop at any time |
| [Vita Wintertodt](vitawintertodt/README.md) | Plays the Wintertodt minigame for you |

## Building
**Requirements:** JDK 11, and VitaLite's API in your local Maven repository (run `./gradlew buildAndPublishAll` in a VitaLite checkout).

```
./gradlew :vitavr:buildPlugin
./gradlew :vitawintertodt:build
```

Both put the plugin jar in `~/.runelite/sideloaded-plugins`. VitaVR's `buildPlugin` bundles OpenXR into the jar, so use it instead of `build`.

Plugins build against the latest RuneLite and VitaLite releases, set by `apiVersion` in `gradle.properties`.
