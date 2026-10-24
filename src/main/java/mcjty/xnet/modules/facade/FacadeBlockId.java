package mcjty.xnet.modules.facade;

import mcjty.lib.varia.Tools;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class FacadeBlockId {
    private final String registryName;

    public FacadeBlockId(BlockState mimicBlock) {
        Block block = mimicBlock.getBlock();
        this.registryName = Tools.getId(block).toString();
    }

    public BlockState getBlockState() {
        return Tools.getBlock(Identifier.parse(registryName)).defaultBlockState();
    }

    @Override
    public String toString() {
        return registryName;
    }
}
