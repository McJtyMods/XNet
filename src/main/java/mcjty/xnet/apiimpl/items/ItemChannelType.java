package mcjty.xnet.apiimpl.items;

import com.mojang.serialization.MapCodec;
import mcjty.rftoolsbase.api.xnet.channels.IChannelSettings;
import mcjty.rftoolsbase.api.xnet.channels.IChannelType;
import mcjty.rftoolsbase.api.xnet.channels.IConnectorSettings;
import mcjty.rftoolsbase.api.xnet.helper.AbstractConnectorSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static mcjty.xnet.utils.I18nConstants.CHANNEL_ITEM;

public class ItemChannelType implements IChannelType {

    @Override
    public String getID() {
        return "xnet.item";
    }

    @Override
    public String getName() {
        return CHANNEL_ITEM.i18n();
    }

    @Override
    public MapCodec<? extends IChannelSettings> getCodec() {
        return ItemChannelSettings.CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, ? extends IChannelSettings> getStreamCodec() {
        return ItemChannelSettings.STREAM_CODEC;
    }

    @Override
    public MapCodec<? extends IConnectorSettings> getConnectorCodec() {
        return ItemConnectorSettings.CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, ? extends IConnectorSettings> getConnectorStreamCodec() {
        return ItemConnectorSettings.STREAM_CODEC;
    }

    @Override
    public boolean supportsBlock(@Nonnull Level world, @Nonnull BlockPos pos, @Nullable Direction side) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te == null) {
            return false;
        }
        if (mcjty.lib.varia.CapabilityTools.itemHandler(net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(world, pos, side)) != null) {
            return true;
        }
        if (te instanceof Container) {
            return true;
        }
        return false;
    }

    @Override
    @Nonnull
    public IConnectorSettings createConnector(@Nonnull Direction side) {
        return new ItemConnectorSettings(AbstractConnectorSettings.DEFAULT_SETTINGS, side);
    }

    @Nonnull
    @Override
    public IChannelSettings createChannel() {
        return new ItemChannelSettings();
    }
}
