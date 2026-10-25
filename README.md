# XNet

Advanced item, fluid, energy, and logic networks.

The `26.1.2_fabric` branch targets Minecraft 26.1.2 with Fabric, Java 25, Gradle 9.5.1 and Fabric Loom 1.17.17. It ports the 26.1.2 NeoForge implementation using native Fabric entrypoints, lookups, attachments, menus, networking and transactional transfer APIs. Config files use Forge Config API Port.

Build with `./gradlew build`, or run `./gradlew :ModXNet:build` from the sibling `../Multi_fabric` workspace. Development runs use `:workspace:runClient` and `:workspace:runServer`; resource generation uses `:ModXNet:runDatagen`.

Every build uses Multi_fabric's shared `gradletools.gradle`: the workspace root copy, an ignored standalone copy, then the remote `26.1.2_fabric` script. Until that script is published, copy `../Multi_fabric/gradletools.gradle` here and exclude `/gradletools.gradle` through `.git/info/exclude`. Keep copies identical and do not commit them.

The shared script resolves these Jenkins Fabric artifacts for both compilation and development runtime:

- `com.github.mcjty:mcjtylib-fabric:26.1.2-10.0.1-fabric.2`
- `mcjty.theoneprobe:theoneprobe:26.1.2-14.0.0-fabric-4`

Minimum runtime mod versions omit the artifact revision suffix: McJtyLib `26.1.2-10.0.1-fabric` and RFToolsBase `26.1.2-7.0.0-fabric`. TOP remains an optional mod integration. Workspace builds use these Jenkins libraries by default; `-PsourceLibraries` opts into the local library source checkouts.

Standalone builds include the sibling `../RFToolsBase_fabric` source checkout when available. For an isolated CI checkout, first publish RFToolsBase Fabric, then select its artifact revision with `-Prftoolsbase_version=<published-version>`. The dependency artifact is `com.github.mcjty:rftoolsbase-fabric`.

Validation covers all three builds, generated resources, seven JUnit tests, and an integrated Fabric client/server test against the Jenkins libraries. From Multi_fabric, run `./gradlew -PfabricPortTests :RFToolsBase:runClientGameTest` to check native energy and inventory rollback, fluid rounding, generation, menus, facade removal and cable/antenna models. The fixture is excluded from release jars.
