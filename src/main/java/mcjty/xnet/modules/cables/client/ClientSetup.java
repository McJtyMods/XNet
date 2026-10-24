package mcjty.xnet.modules.cables.client;

import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.common.NeoForge;

public class ClientSetup {
    public static void initClient() {
        NeoForge.EVENT_BUS.addListener(CableWorldRenderer::tick);
    }

    public static void modelInit(ModelEvent.ModifyBakingResult event) {
        CableModelLoader.register(event);
    }
}
