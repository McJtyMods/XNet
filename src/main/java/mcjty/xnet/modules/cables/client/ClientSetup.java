package mcjty.xnet.modules.cables.client;

public class ClientSetup {
    public static void initClient() {
        net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents.COLLECT_SUBMITS.register(CableWorldRenderer::tick);
    }
    public static void registerModels() { CableModelLoader.register(); ObjModelLoader.register(); }
}
