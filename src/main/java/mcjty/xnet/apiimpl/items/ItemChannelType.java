package mcjty.xnet.apiimpl.items;

import mcjty.rftoolsbase.api.xnet.channels.IChannelSettings;
import mcjty.rftoolsbase.api.xnet.channels.IChannelType;
import mcjty.rftoolsbase.api.xnet.channels.IConnectorSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

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
    public boolean supportsBlock(@Nonnull Level world, @Nonnull BlockPos pos, @Nullable Direction side) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te == null) {
            return false;
        }
        if (te.getCapability(ForgeCapabilities.ITEM_HANDLER, side).isPresent()) {
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
        return new ItemConnectorSettings(side);
    }

    @Nonnull
    @Override
    public IChannelSettings createChannel() {
        return new ItemChannelSettings();
    }
}
