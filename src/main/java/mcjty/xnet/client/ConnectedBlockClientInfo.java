package mcjty.xnet.client;

import mcjty.lib.blockcommands.ISerializer;
import mcjty.lib.network.NetworkTools;
import mcjty.lib.varia.OrientationTools;
import mcjty.rftoolsbase.api.xnet.keys.SidedPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ConnectedBlockClientInfo {

    /// The position of the block we are connecting too
    @Nonnull
    private final SidedPos pos;
    /// The itemstack representing the block
    @Nonnull private final ItemStack connectedBlock;

    /// The name of the connector
    @Nonnull private final String name;

    /// The name of the block
    @Nonnull private final String blockName;

    public static class Serializer implements ISerializer<ConnectedBlockClientInfo> {
        @Override
        public Function<RegistryFriendlyByteBuf, ConnectedBlockClientInfo> getDeserializer() {
            return buf -> {
                if (buf.readBoolean()) {
                    return new ConnectedBlockClientInfo(buf);
                } else {
                    return null;
                }
            };
        }

        @Override
        public BiConsumer<RegistryFriendlyByteBuf, ConnectedBlockClientInfo> getSerializer() {
            return (buf, info) -> {
                if (info == null) {
                    buf.writeBoolean(false);
                } else {
                    buf.writeBoolean(true);
                    info.writeToBuf(buf);
                }
            };
        }
    }

    public ConnectedBlockClientInfo(@Nonnull SidedPos pos, @Nonnull ItemStack connectedBlock, @Nonnull String name) {
        this.pos = pos;
        this.connectedBlock = connectedBlock;
        this.name = name;
        this.blockName = getStackUnlocalizedName(connectedBlock);
    }

    public ConnectedBlockClientInfo(@Nonnull RegistryFriendlyByteBuf buf) {
        pos = new SidedPos(buf.readBlockPos(), OrientationTools.DIRECTION_VALUES[buf.readByte()]);
        connectedBlock = NetworkTools.readItemStack(buf);
        name = NetworkTools.readStringUTF8(buf);
        blockName = NetworkTools.readStringUTF8(buf);
    }

    public void writeToBuf(@Nonnull RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos.pos());
        buf.writeByte(pos.side().ordinal());
        NetworkTools.writeItemStack(buf, connectedBlock);
        NetworkTools.writeStringUTF8(buf, name);
        NetworkTools.writeStringUTF8(buf, blockName);
    }

    @Nonnull
    public String getName() {
        return name;
    }

    @Nonnull
    public String getBlockUnlocName() {
        return blockName;
    }

    @Nonnull
    public SidedPos getPos() {
        return pos;
    }

    @Nonnull
    public ItemStack getConnectedBlock() {
        return connectedBlock;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConnectedBlockClientInfo that = (ConnectedBlockClientInfo) o;

        return pos.equals(that.pos);
    }

    @Override
    public int hashCode() {
        return pos.hashCode();
    }

    private static String getStackUnlocalizedName(ItemStack stack) {
        // @todo 1.21 support for custom names
        return stack.getItem().getDescriptionId();
    }
}
