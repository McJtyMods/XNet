package mcjty.xnet.modules.cables.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class AdvancedConnectorBlock extends ConnectorBlock {

    public AdvancedConnectorBlock(CableBlockType type, net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        super(type, properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new AdvancedConnectorTileEntity(pPos, pState);
    }

    @Override
    public boolean isAdvancedConnector() {
        return true;
    }
}
