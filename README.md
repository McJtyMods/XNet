# XNet

Advanced item, fluid, energy, and logic networks.

The `26.2_neo` branch targets Minecraft 26.2, NeoForge 26.2.0.75, and Java 25. It is based on `26.1.2_neo`.

Place this checkout beside the 26.2 `Multi_neo` and `McJtyLib_neo` checkouts. From `Multi_neo`, run:

```sh
./gradlew :ModXNet:build
./gradlew :ModXNet:runClientData
./gradlew :workspace:runClient
./gradlew :workspace:runServer
```

The workspace discovers sibling checkouts automatically and resolves McJtyLib, RFToolsBase, and The One Probe from their local source projects. The workspace runtime includes all available mods.

The build applies the root workspace's `gradletools.gradle`, then a local copy or sibling `../Multi_neo/gradletools.gradle`, then the remote [MultiWorkspace 26.2 script](https://raw.githubusercontent.com/McJtyMods/MultiWorkspace/26.2_neo/gradletools.gradle). Shared settings select Minecraft, NeoForge, JEI, and library versions; this mod does not override NeoForge.

Standalone builds use `./gradlew build` with Java 25 and resolve McJtyLib and The One Probe from Maven. Standalone builds also need a published compatible RFToolsBase artifact, supplied with `-Prftoolsbase_version=<version>`. Use the workspace for development against unpublished local dependencies.

Validation: all three mod builds, seven mod JUnit regression tests, six McJtyLib regression checks, workspace compilation, all three data generators, and integrated GameTest server startup/ticking/shutdown passed against the local 26.2 sources. The client loaded the mods and texture atlases. The integrated server also emitted unrelated InControl loot-modifier and areas.json errors. Interactive gameplay still needs testing.
