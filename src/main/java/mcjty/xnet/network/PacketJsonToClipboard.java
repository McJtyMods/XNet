package mcjty.xnet.network;

import io.netty.buffer.ByteBuf;
import mcjty.lib.network.NetworkTools;
import mcjty.xnet.XNet;
import mcjty.xnet.blocks.controller.gui.GuiController;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketJsonToClipboard implements IMessage {

    private String json;

    @Override
    public void fromBytes(ByteBuf buf) {
        json = NetworkTools.readStringUTF8(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        NetworkTools.writeStringUTF8(buf, json);
    }

    public PacketJsonToClipboard() {
    }

    public PacketJsonToClipboard(String json) {
        this.json = json;
    }

    public static class Handler implements IMessageHandler<PacketJsonToClipboard, IMessage> {
        @Override
        public IMessage onMessage(PacketJsonToClipboard message, MessageContext ctx) {
            XNet.proxy.addScheduledTaskClient(() -> handle(message, ctx));
            return null;
        }

        private void handle(PacketJsonToClipboard message, MessageContext ctx) {
            GuiController.toClipboard(message.json);
        }
    }
}