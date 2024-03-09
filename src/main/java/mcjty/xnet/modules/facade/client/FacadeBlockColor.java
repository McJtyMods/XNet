package mcjty.xnet.modules.facade.client;

import mcjty.xnet.modules.facade.IFacadeSupport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FacadeBlockColor implements BlockColor {

    @Override
    public int getColor(@Nonnull BlockState blockState, @Nullable BlockAndTintGetter world, @Nullable BlockPos pos, int tint) {
        if (world != null) {
            BlockEntity te = world.getBlockEntity(pos);
            if (te instanceof IFacadeSupport facade) {
                BlockState mimic = facade.getMimicBlock();
                if (mimic != null) {
                    return Minecraft.getInstance().getBlockColors().getColor(mimic, world, pos, tint);
                }
            }
        }
        return -1;
    }
}
