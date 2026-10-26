# XNet

Advanced item, fluid, energy, and logic networks.

The `26.3_fabric` branch targets Minecraft 26.3 with Fabric, Java 25, Gradle 9.6.0 and Fabric Loom 1.17.21. It updates the 26.2 Fabric port using native Fabric entrypoints, lookups, attachments, menus, networking and transactional transfer APIs. Config files use Forge Config API Port.

Build with `./gradlew build`, or run `./gradlew :ModXNet:build` from the sibling `../Multi_fabric` workspace. Development runs use `:workspace:runClient` and `:workspace:runServer`; resource generation uses `:ModXNet:runDatagen`.

Builds apply [MultiWorkSpace's shared Fabric 26.3 script](https://github.com/McJtyMods/MultiWorkSpace/blob/26.3_fabric/gradletools.gradle) from the integrated workspace, sibling `../Multi_fabric`, nested parent, or remote branch. Mod checkouts do not keep local copies.

McJtyLib always resolves to the published Jenkins artifact. TOP uses its source project when included, otherwise its Jenkins artifact:

- `com.github.mcjty:mcjtylib-fabric:26.3-11.0.1-fabric.2`
- `mcjty.theoneprobe:theoneprobe:26.3-16.0.0-fabric-1`

Minimum runtime mod versions omit the artifact revision suffix: McJtyLib `26.3-11.0.1-fabric` and RFToolsBase `26.3-9.0.0-fabric`. TOP remains an optional mod integration. Workspace builds also use published McJtyLib; its local source checkout is excluded from the aggregate run to prevent duplicate mod IDs.

`rftoolsbase()` selects the local source project, including the sibling `../RFToolsBase_fabric` checkout in standalone builds. Until this port is published, keep that checkout available. The shared script defines `rftoolsbase_version=26.3-9.0.0-fabric-1` for the eventual Jenkins artifact fallback; update it to the actual published build before using that fallback.

Validation commands cover all three builds, generated resources, JUnit tests, and an integrated Fabric client/server test. From Multi_fabric, run `./gradlew -PfabricPortTests :RFToolsBase:runClientGameTest` to check native energy and inventory rollback, fluid rounding, generation, menus, facade removal and cable/antenna models. The fixture is excluded from release jars.

The McJtyLib Fabric checkout contains the 26.3 overlay color-target fix. Publish a new Jenkins library build and update `mcjtylib_version` in Multi_fabric to use it for cable outlines.
