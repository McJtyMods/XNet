package mcjty.xnet.setup;

import mcjty.xnet.modules.controller.network.PacketControllerError;
import mcjty.xnet.modules.controller.network.PacketJsonToClipboard;

public class ClientMessages {
    public static void register() {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(PacketJsonToClipboard.TYPE, (packet, context) -> packet.handle(new mcjty.lib.network.PayloadContext(context.player(), context.client())));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(PacketControllerError.TYPE, (packet, context) -> packet.handle(new mcjty.lib.network.PayloadContext(context.player(), context.client())));
    }
}
