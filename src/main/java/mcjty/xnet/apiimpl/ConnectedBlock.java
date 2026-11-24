package mcjty.xnet.apiimpl;

import mcjty.lib.varia.LevelTools;
import mcjty.rftoolsbase.api.xnet.keys.SidedConsumer;
import mcjty.xnet.modules.cables.blocks.ConnectorTileEntity;
import net.minecraft.core.BlockPos;

import javax.annotation.Nonnull;


public class ConnectedBlock<T> {
    @Nonnull private final SidedConsumer sidedConsumer;
    @Nonnull private final T settings;
    @Nonnull private final BlockPos connectorPos;
    @Nonnull private final BlockPos blockPos;
    @Nonnull private final ConnectorTileEntity connectorEntity;

    public ConnectedBlock(@Nonnull SidedConsumer sidedConsumer, @Nonnull T settings, @Nonnull BlockPos connectorPos,
                          @Nonnull BlockPos blockPos, @Nonnull ConnectorTileEntity connectorEntity) {
        this.sidedConsumer = sidedConsumer;
        this.settings = settings;
        this.connectorPos = connectorPos;
        this.blockPos = blockPos;
        this.connectorEntity = connectorEntity;
    }

    @Nonnull
    public SidedConsumer sidedConsumer() {return sidedConsumer;}

    @Nonnull
    public T settings() {return settings;}

    @Nonnull
    public BlockPos connectorPos() {return connectorPos;}

    @Nonnull
    public  BlockPos getBlockPos() {
        return blockPos;
    }

    @Nonnull
    public ConnectorTileEntity getConnectorEntity() {
        return connectorEntity;
    }

    public boolean isLoaded() {
        return !connectorEntity.isRemoved()
                && LevelTools.isLoaded(connectorEntity.getLevel(), connectorPos)
                && LevelTools.isLoaded(connectorEntity.getLevel(), blockPos);
    }

}
