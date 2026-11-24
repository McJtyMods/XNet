package mcjty.xnet.modules.cables.blocks;

import mcjty.xnet.modules.cables.CableColor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class ColorBlockItem extends TooltipBlockItem {

    private final CableColor color;

    public ColorBlockItem(Block blockIn, Properties builder, CableColor color) {
        super(blockIn, builder.useItemDescriptionPrefix());
        this.color = color;
    }

    @Nullable
    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState blockstate = ((GenericCableBlock)this.getBlock()).calculateState(context.getLevel(),
                context.getClickedPos(), this.getBlock().defaultBlockState().setValue(GenericCableBlock.COLOR, color));
        if (canPlace(context, blockstate)) {
            return blockstate;
        } else {
            return null;
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flags) {
        if (color == CableColor.ROUTING) {
            String key = getBlock() instanceof ConnectorBlock
                    ? "message.xnet.routing_connector.tooltip"
                    : "message.xnet.routing_cable.tooltip";
            tooltip.accept(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        } else {
            super.appendHoverText(stack, context, display, tooltip, flags);
        }
    }

    @Override
    public List<ItemStack> getItemsForTab() {
        return Collections.singletonList(new ItemStack(this));
    }
}
