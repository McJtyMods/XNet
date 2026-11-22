package mcjty.xnet.modules.various.blocks;

import mcjty.lib.tooltips.ITooltipSettings;
import mcjty.lib.varia.ComponentFactory;
import mcjty.lib.varia.SafeClientTools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class RedstoneProxyUBlock extends RedstoneProxyBlock implements ITooltipSettings {

    public RedstoneProxyUBlock() {
        super();
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable BlockGetter worldIn, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flagIn) {
        if (SafeClientTools.isSneaking()) {
            tooltip.add(ComponentFactory.translatable("message.xnet.redstone_proxy_upd.header").withStyle(ChatFormatting.GREEN));
            tooltip.add(ComponentFactory.translatable("message.xnet.redstone_proxy_upd.gold").withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(ComponentFactory.translatable("message.xnet.shiftmessage"));
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(@Nonnull BlockState state, @Nonnull Level worldIn, @Nonnull BlockPos pos, @Nonnull Block blockIn, @Nonnull BlockPos fromPos, boolean isMoving) {
        // A queued notification from another updating proxy must not be sent back.
        if (!(blockIn instanceof RedstoneProxyUBlock)) {
            worldIn.updateNeighborsAt(pos, this);
        }
    }
}
