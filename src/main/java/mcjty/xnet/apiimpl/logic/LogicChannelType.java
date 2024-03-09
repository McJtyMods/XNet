package mcjty.xnet.apiimpl.logic;

import mcjty.rftoolsbase.api.xnet.channels.IChannelSettings;
import mcjty.rftoolsbase.api.xnet.channels.IChannelType;
import mcjty.rftoolsbase.api.xnet.channels.IConnectorSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static mcjty.xnet.utils.I18nConstants.CHANNEL_LOGIC;

public class LogicChannelType implements IChannelType {

    @Override
    public String getID() {
        return "xnet.logic";
    }

    @Override
    public String getName() {
        return CHANNEL_LOGIC.i18n();
    }

    @Override
    public boolean supportsBlock(@Nonnull Level world, @Nonnull BlockPos pos, @Nullable Direction side) {
        return true;
    }

    @Override
    @Nonnull
    public IConnectorSettings createConnector(@Nonnull Direction side) {
        return new LogicConnectorSettings(side);
    }

    @Nonnull
    @Override
    public IChannelSettings createChannel() {
        return new LogicChannelSettings();
    }
}
