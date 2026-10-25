package mcjty.xnet.modules.cables.client;

import net.neoforged.neoforge.client.event.RegisterBlockStateModels;
import net.neoforged.neoforge.common.NeoForge;

public class ClientSetup {
    public static void initClient() {
        NeoForge.EVENT_BUS.addListener(CableWorldRenderer::tick);
    }

    public static void registerModels(RegisterBlockStateModels event) {
        CableModelLoader.register(event);
    }
}
