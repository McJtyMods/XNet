package mcjty.xnet.setup;

import mcjty.xnet.modules.controller.network.PacketControllerError;
import mcjty.xnet.modules.controller.network.PacketJsonToClipboard;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import mcjty.lib.network.Networking;

public class XNetMessages {
    public static void registerMessages() {
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(PacketJsonToClipboard.TYPE, PacketJsonToClipboard.CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(PacketControllerError.TYPE, PacketControllerError.CODEC);
    }

    public static <T extends CustomPacketPayload> void sendToPlayer(T packet, Player player) {
        Networking.sendToPlayer(packet, player);
    }

    public static <T extends CustomPacketPayload> void sendToServer(T packet) {
        Networking.sendToServer(packet);
    }
}
