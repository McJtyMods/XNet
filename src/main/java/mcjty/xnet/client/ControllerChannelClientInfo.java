package mcjty.xnet.client;

import mcjty.lib.blockcommands.ISerializer;
import mcjty.lib.network.NetworkTools;
import mcjty.rftoolsbase.api.xnet.channels.IChannelType;
import mcjty.xnet.XNet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ControllerChannelClientInfo {
    @Nonnull private final String channelName;
    @Nonnull private final String publishedName;
    @Nonnull private final BlockPos pos;
    @Nonnull private final IChannelType channelType;
    private final boolean remote;      // If this channel was made available through a wireless router
    private final int index;        // Index of the channel within that controller (0 through 7)

    public static class Serializer implements ISerializer<ControllerChannelClientInfo> {
        @Override
        public Function<RegistryFriendlyByteBuf, ControllerChannelClientInfo> getDeserializer() {
            return buf -> {
                if (buf.readBoolean()) {
                    return new ControllerChannelClientInfo(buf);
                } else {
                    return null;
                }
            };
        }

        @Override
        public BiConsumer<RegistryFriendlyByteBuf, ControllerChannelClientInfo> getSerializer() {
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

    public ControllerChannelClientInfo(@Nonnull String channelName, @Nonnull String publishedName, @Nonnull BlockPos pos, @Nonnull IChannelType channelType, boolean remote, int index) {
        this.channelName = channelName;
        this.publishedName = publishedName;
        this.pos = pos;
        this.channelType = channelType;
        this.remote = remote;
        this.index = index;
    }

    public ControllerChannelClientInfo(@Nonnull FriendlyByteBuf buf) {
        channelName = NetworkTools.readStringUTF8(buf);
        publishedName = NetworkTools.readStringUTF8(buf);
        String id = buf.readUtf(32767);
        IChannelType t = XNet.xNetApi.findType(id);
        if (t == null) {
            throw new RuntimeException("Bad type: " + id);
        }
        channelType = t;
        pos = buf.readBlockPos();
        remote = buf.readBoolean();
        index = buf.readInt();
    }

    public void writeToBuf(@Nonnull FriendlyByteBuf buf) {
        NetworkTools.writeStringUTF8(buf, channelName);
        NetworkTools.writeStringUTF8(buf, publishedName);
        buf.writeUtf(channelType.getID());
        buf.writeBlockPos(pos);
        buf.writeBoolean(remote);
        buf.writeInt(index);
    }

    @Nonnull
    public String getChannelName() {
        return channelName;
    }

    @Nonnull
    public String getPublishedName() {
        return publishedName;
    }

    @Nonnull
    public BlockPos getPos() {
        return pos;
    }

    @Nonnull
    public IChannelType getChannelType() {
        return channelType;
    }

    public int getIndex() {
        return index;
    }

    public boolean isRemote() {
        return remote;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ControllerChannelClientInfo other
                && remote == other.remote && index == other.index
                && channelName.equals(other.channelName) && publishedName.equals(other.publishedName)
                && pos.equals(other.pos) && channelType.equals(other.channelType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelName, publishedName, pos, channelType, remote, index);
    }
}
