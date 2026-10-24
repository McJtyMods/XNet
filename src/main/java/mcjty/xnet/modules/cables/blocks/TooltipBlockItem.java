package mcjty.xnet.modules.cables.blocks;

import java.util.ArrayList;
import java.util.function.Consumer;
import mcjty.lib.items.BaseBlockItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

public class TooltipBlockItem extends BaseBlockItem {
    public TooltipBlockItem(Block block, Properties properties) { super(block, properties); }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> output, TooltipFlag flags) {
        super.appendHoverText(stack, context, display, output, flags);
        var lines = new ArrayList<Component>();
        if (getBlock() instanceof ConnectorBlock connector) connector.appendHoverText(stack, context, lines, flags);
        if (getBlock() instanceof mcjty.xnet.modules.various.blocks.RedstoneProxyBlock proxy) proxy.appendHoverText(stack, context, lines, flags);
        lines.forEach(output);
    }
}
