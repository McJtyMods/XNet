package mcjty.xnet.modules.controller.network;

import mcjty.xnet.XNet;
import mcjty.xnet.modules.controller.client.GuiController;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketJsonToClipboard(String json) implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(XNet.MODID, "jsontoclipboard");
    public static final CustomPacketPayload.Type<PacketJsonToClipboard> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, PacketJsonToClipboard> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PacketJsonToClipboard::json,
            PacketJsonToClipboard::new);

    public static PacketJsonToClipboard create(String json) {
        return new PacketJsonToClipboard(json);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            GuiController.toClipboard(json);
        });
    }
}
