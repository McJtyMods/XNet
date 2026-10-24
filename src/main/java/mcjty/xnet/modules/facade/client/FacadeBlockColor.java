package mcjty.xnet.modules.facade.client;

import mcjty.xnet.modules.facade.IFacadeSupport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Resolve each tint layer from the facade's mimicked block. */
public record FacadeBlockColor(int layer) implements BlockTintSource {
    @Override public int color(BlockState state) { return -1; }
    @Override public int colorInWorld(BlockState state, BlockAndTintGetter world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof IFacadeSupport facade) {
            var mimic = facade.getMimicBlock();
            if (mimic != null && mimic.getBlock() != state.getBlock()) {
                var tint = Minecraft.getInstance().getBlockColors().getTintSource(mimic, layer);
                if (tint != null) return tint.colorInWorld(mimic, world, pos);
            }
        }
        return -1;
    }
}
