package mcjty.xnet.setup;

import mcjty.xnet.XNet;
import mcjty.xnet.modules.controller.network.PacketControllerError;
import mcjty.xnet.modules.controller.network.PacketJsonToClipboard;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class XNetMessages {

    public static void registerMessages(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(XNet.MODID)
                .versioned("1.0")
                .optional();

        registrar.playToClient(PacketJsonToClipboard.TYPE, PacketJsonToClipboard.CODEC, PacketJsonToClipboard::handle);
        registrar.playToClient(PacketControllerError.TYPE, PacketControllerError.CODEC, PacketControllerError::handle);
    }

    public static <T extends CustomPacketPayload> void sendToPlayer(T packet, Player player) {
        PacketDistributor.sendToPlayer((ServerPlayer)player, packet);
    }

    public static <T extends CustomPacketPayload> void sendToServer(T packet) {
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(packet);
    }
}
