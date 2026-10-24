# XNet

Advanced item, fluid, energy, and logic networks.

The `26.1.2_neo` branch targets Minecraft 26.1.2 with NeoForge 26.1.2.99 and Java 25. It was ported from `1.21_neo` (Minecraft 1.21.1).

Build standalone with `./gradlew build`, or place this checkout beside `Multi_neo` and run `./gradlew :ModXNet:build` from the workspace. Standalone RFToolsPower and XNet builds use the published RFToolsBase Maven artifact `26.1.2-7.0.0-1`, with minimum runtime mod version `26.1.2-7.0.0`.

Workspace builds use available McJtyLib and RFToolsBase source projects. The build uses the workspace's `gradletools.gradle` when included there, and otherwise applies the published [MultiWorkspace 26.1.2 script](https://raw.githubusercontent.com/McJtyMods/MultiWorkspace/26.1.2_neo/gradletools.gradle). NeoForge .99 overrides the script's .94 default because the configured JEI runtime requires it.

Standalone builds resolve the published McJtyLib Maven artifact `com.github.mcjty:mcjtylib:26.1.2-10.0.0.1`. The minimum runtime mod dependency remains `26.1.2-10.0.0`. The One Probe uses the 26.1.2 release configured by the shared script.

For integrated development, use `:workspace:runClient` or `:workspace:runServer` from `Multi_neo`. Generate resources with `:ModXNet:runClientData`.

Validation completed with the published McJtyLib artifact: all three workspace builds, seven JUnit regression tests, each mod's data generation, integrated GameTest server startup/ticking/shutdown, and client startup with model/texture loading. RFToolsBase also passed a standalone build using the remote shared script. XNet and RFToolsPower subsequently passed standalone builds using the published RFToolsBase artifact, and the integrated server check passed with both foundation mods loaded from Maven. Full interactive gameplay remains to be tested.
