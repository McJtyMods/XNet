package mcjty.xnet.modules.cables.blocks;

import mcjty.lib.items.BaseBlockItem;
import mcjty.xnet.modules.cables.CableColor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class ColorBlockItem extends BaseBlockItem {

    private final CableColor color;

    public ColorBlockItem(Block blockIn, Properties builder, CableColor color) {
        super(blockIn, builder);
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
    @Nonnull
    public String getDescriptionId() {
        // We don't want the translation key of the block
        return super.getOrCreateDescriptionId();
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, Item.TooltipContext context, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flags) {
        if (color == CableColor.ROUTING) {
            String key = getBlock() instanceof ConnectorBlock
                    ? "message.xnet.routing_connector.tooltip"
                    : "message.xnet.routing_cable.tooltip";
            tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        } else {
            super.appendHoverText(stack, context, tooltip, flags);
        }
    }

    @Override
    public List<ItemStack> getItemsForTab() {
        return Collections.singletonList(new ItemStack(this));
    }
}
