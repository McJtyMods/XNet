package mcjty.xnet.apiimpl;

import mcjty.rftoolsbase.api.xnet.keys.SidedConsumer;
import mcjty.xnet.modules.cables.blocks.ConnectorTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nonnull;

public class ConnectedEntity<T> extends ConnectedBlock<T> {

    @Nonnull private final BlockEntity connectedEntity;

    public ConnectedEntity(@Nonnull SidedConsumer sidedConsumer, @Nonnull T settings, @Nonnull BlockPos connectorPos,
                           @Nonnull BlockPos blockPos, @Nonnull BlockEntity connectedEntity,
                           @Nonnull ConnectorTileEntity connectorEntity) {
        super(sidedConsumer, settings, connectorPos, blockPos, connectorEntity);
        this.connectedEntity = connectedEntity;
    }

    public ConnectedEntity(@Nonnull ConnectedBlock<T> connectedBlock, @Nonnull BlockEntity connectedEntity) {
        super(
                connectedBlock.sidedConsumer(), connectedBlock.settings(), connectedBlock.connectorPos(),
                connectedBlock.getBlockPos(), connectedBlock.getConnectorEntity()
        );
        this.connectedEntity = connectedEntity;
    }

    @Nonnull
    public BlockEntity getConnectedEntity() {
        return connectedEntity;
    }

    @Override
    public boolean isLoaded() {
        return super.isLoaded() && !connectedEntity.isRemoved();
    }
}
