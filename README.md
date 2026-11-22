# VitaPluginsSource
Source for TonicBox's VitaLite plugins. Each plugin is its own Gradle subproject; install them from the Vita Hub tab in VitaLite.

| Plugin | What it does |
|--------|--------------|
| [Vita Arceuus Library](vitaarceuuslibrary/README.md) | Plays the Arceuus Library minigame for Magic or Runecraft XP |
| [Vita Bank Valuer](vitabankvaluer/README.md) | Shows where the value in your bank is |
| [Vita MTA](vitamta/README.md) | Plays the Mage Training Arena rooms for pizazz points |
| [Vita Multi-Client Util](vitamulticlient/README.md) | Has your other logged-in clients follow, gather or act together |
| [Vita Tempoross](vitatempoross/README.md) | Plays the Tempoross skilling boss on mass worlds |
| [Vita Tithe Farm](vitatithefarm/README.md) | Plays the Tithe Farm minigame for Farming XP and reward points |
| [VitaVR](vitavr/README.md) | Play in a VR headset and switch back to the desktop at any time |
| [Vita Wintertodt](vitawintertodt/README.md) | Plays the Wintertodt minigame for you |

## Building
**Requirements:** JDK 11, and VitaLite's API in your local Maven repository (run `./gradlew buildAndPublishAll` in a VitaLite checkout).

```
./gradlew :vitavr:build
./gradlew :vitaarceuuslibrary:build
./gradlew :vitabankvaluer:build
./gradlew :vitamta:build
./gradlew :vitamulticlient:build
./gradlew :vitatempoross:build
./gradlew :vitatithefarm:build
./gradlew :vitawintertodt:build
```

Each puts the plugin jar in `~/.runelite/sideloaded-plugins`. VitaVR's jar comes from its `buildPlugin` task, which bundles OpenXR and writes to `vitavr/build/plugin`.

Plugins build against the latest RuneLite and VitaLite releases, set by `apiVersion` in `gradle.properties`.
