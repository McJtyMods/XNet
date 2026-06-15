package mcjty.xnet.modules.various.blocks;

import mcjty.lib.gui.ManualEntry;
import mcjty.lib.tooltips.ITooltipSettings;
import mcjty.lib.varia.ComponentFactory;
import mcjty.lib.varia.SafeClientTools;
import mcjty.rftoolsbase.tools.ManualHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class RedstoneProxyBlock extends Block implements ITooltipSettings {

    public static final ManualEntry MANUAL = ManualHelper.create("xnet:simple/redstone_proxy");

    public RedstoneProxyBlock() {
        super(Properties.of()
                .strength(2.0f)
                .sound(SoundType.METAL)
        );
    }

    @Override
    public ManualEntry getManualEntry() {
        return MANUAL;
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable BlockGetter worldIn, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flagIn) {
        if (SafeClientTools.isSneaking()) {
            tooltip.add(ComponentFactory.translatable("message.xnet.redstone_proxy.header").withStyle(ChatFormatting.GREEN));
            tooltip.add(ComponentFactory.translatable("message.xnet.redstone_proxy.gold").withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(ComponentFactory.translatable("message.xnet.shiftmessage"));
        }
    }

}
