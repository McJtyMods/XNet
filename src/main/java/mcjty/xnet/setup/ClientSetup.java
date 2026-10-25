package mcjty.xnet.setup;

public class ClientSetup implements net.fabricmc.api.ClientModInitializer {
    @Override public void onInitializeClient() {
        ClientMessages.register();
        mcjty.xnet.XNet.instance.modules.initClient();
        mcjty.xnet.modules.facade.client.ClientSetup.registerBlockColor();
    }
}
