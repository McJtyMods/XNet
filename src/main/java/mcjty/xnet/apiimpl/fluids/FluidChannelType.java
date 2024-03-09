package mcjty.xnet.apiimpl.fluids;

import mcjty.rftoolsbase.api.xnet.channels.IChannelSettings;
import mcjty.rftoolsbase.api.xnet.channels.IChannelType;
import mcjty.rftoolsbase.api.xnet.channels.IConnectorSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static mcjty.xnet.utils.I18nConstants.CHANNEL_FLUID;

public class FluidChannelType implements IChannelType {

    @Override
    public String getID() {
        return "xnet.fluid";
    }

    @Override
    public String getName() {
        return CHANNEL_FLUID.i18n();
    }

    @Override
    public boolean supportsBlock(@Nonnull Level world, @Nonnull BlockPos pos, @Nullable Direction side) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te == null) {
            return false;
        }
        if (te.getCapability(ForgeCapabilities.FLUID_HANDLER, side).isPresent()) {
            return true;
        }
        return false;
    }

    @Override
    @Nonnull
    public IConnectorSettings createConnector(@Nonnull Direction side) {
        return new FluidConnectorSettings(side);
    }

    @Nonnull
    @Override
    public IChannelSettings createChannel() {
        return new FluidChannelSettings();
    }
}
