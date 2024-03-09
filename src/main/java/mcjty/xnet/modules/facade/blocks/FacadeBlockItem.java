package mcjty.xnet.modules.facade.blocks;

import mcjty.lib.builder.TooltipBuilder;
import mcjty.lib.tooltips.ITooltipSettings;
import mcjty.lib.varia.ComponentFactory;
import mcjty.lib.varia.NBTTools;
import mcjty.lib.varia.Tools;
import mcjty.xnet.XNet;
import mcjty.xnet.modules.cables.CableModule;
import mcjty.xnet.modules.cables.blocks.ConnectorTileEntity;
import mcjty.xnet.modules.facade.FacadeModule;
import mcjty.xnet.modules.facade.IFacadeSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static mcjty.lib.builder.TooltipBuilder.gold;
import static mcjty.lib.builder.TooltipBuilder.header;
import static mcjty.lib.builder.TooltipBuilder.parameter;
import static mcjty.xnet.apiimpl.Constants.TAG_MIMIC;
import static mcjty.xnet.modules.cables.blocks.GenericCableBlock.COLOR;
import static mcjty.xnet.modules.cables.blocks.GenericCableBlock.DOWN;
import static mcjty.xnet.modules.cables.blocks.GenericCableBlock.EAST;
import static mcjty.xnet.modules.cables.blocks.GenericCableBlock.NORTH;
import static mcjty.xnet.modules.cables.blocks.GenericCableBlock.SOUTH;
import static mcjty.xnet.modules.cables.blocks.GenericCableBlock.UP;
import static mcjty.xnet.modules.cables.blocks.GenericCableBlock.WEST;
import static mcjty.xnet.utils.I18nConstants.FACADE_CURRENT_MIMIC_FORMATTED;

public class FacadeBlockItem extends BlockItem implements ITooltipSettings {

    private Lazy<TooltipBuilder> tooltipBuilder = () -> new TooltipBuilder()
            .info(header(),
                    gold(stack -> !isMimicking(stack)),
                    parameter("info", FacadeBlockItem::isMimicking, FacadeBlockItem::getMimickingString));

    private static boolean isMimicking(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_MIMIC);
    }

    private static String getMimickingString(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            CompoundTag mimic = tag.getCompound(TAG_MIMIC);
            Block value = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(mimic.getString("Name")));
            if (value != null) {
                ItemStack s = new ItemStack(value, 1);
                s.getItem();
                return s.getHoverName().getString() /* was getFormattedText() */;
            }
        }
        return "<unset>";
    }


    public FacadeBlockItem(FacadeBlock block) {
        super(block, XNet.setup.defaultProperties());
    }

    private static void userSetMimicBlock(@Nonnull ItemStack item, BlockState mimicBlock, UseOnContext context) {
        Level world = context.getLevel();
        Player player = context.getPlayer();
        setMimicBlock(item, mimicBlock);
        if (world.isClientSide) {
            player.displayClientMessage(ComponentFactory.literal(
                    String.format(FACADE_CURRENT_MIMIC_FORMATTED.i18n(), mimicBlock.getBlock().getDescriptionId())),
                    false
            );
        }
    }

    public static void setMimicBlock(@Nonnull ItemStack item, BlockState mimicBlock) {
        CompoundTag tagCompound = new CompoundTag();
        CompoundTag nbt = NbtUtils.writeBlockState(mimicBlock);
        tagCompound.put(TAG_MIMIC, nbt);
        item.setTag(tagCompound);
    }

    public static BlockState getMimicBlock(Level level, @Nonnull ItemStack stack) {
        CompoundTag tagCompound = stack.getTag();
        if (tagCompound == null || !tagCompound.contains(TAG_MIMIC)) {
            return Blocks.COBBLESTONE.defaultBlockState();
        } else {
            return NBTTools.readBlockState(level, tagCompound.getCompound(TAG_MIMIC));
        }
    }

    @Override
    protected boolean canPlace(@Nonnull BlockPlaceContext context, @Nonnull BlockState state) {
        return true;
    }

    @Nonnull
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        ItemStack itemstack = context.getItemInHand();

        if (!itemstack.isEmpty()) {

            if (block == CableModule.NETCABLE.get()) {
                FacadeBlock facadeBlock = (FacadeBlock) this.getBlock();
                BlockPlaceContext blockContext = new ReplaceBlockItemUseContext(context);
                BlockState placementState = facadeBlock.getStateForPlacement(blockContext)
                        .setValue(COLOR, state.getValue(COLOR))
                        .setValue(NORTH, state.getValue(NORTH))
                        .setValue(SOUTH, state.getValue(SOUTH))
                        .setValue(WEST, state.getValue(WEST))
                        .setValue(EAST, state.getValue(EAST))
                        .setValue(UP, state.getValue(UP))
                        .setValue(DOWN, state.getValue(DOWN))
                        ;

                if (placeBlock(blockContext, placementState)) {
                    SoundType soundtype = world.getBlockState(pos).getBlock().getSoundType(world.getBlockState(pos), world, pos, player);
                    world.playSound(player, pos, soundtype.getPlaceSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
                    BlockEntity te = world.getBlockEntity(pos);
                    if (te instanceof FacadeTileEntity) {
                        ((FacadeTileEntity) te).setMimicBlock(getMimicBlock(world, itemstack));
                    }
                    int amount = -1;
                    itemstack.grow(amount);
                }
            } else if (block == CableModule.CONNECTOR.get() || block == CableModule.ADVANCED_CONNECTOR.get()) {
                BlockEntity te = world.getBlockEntity(pos);
                if (te instanceof ConnectorTileEntity connectorTileEntity) {
                    if (connectorTileEntity.getMimicBlock() == null) {
                        connectorTileEntity.setMimicBlock(getMimicBlock(world, itemstack));
                        SoundType soundtype = world.getBlockState(pos).getBlock().getSoundType(world.getBlockState(pos), world, pos, player);
                        world.playSound(player, pos, soundtype.getPlaceSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
                        int amount = -1;
                        itemstack.grow(amount);
                    } else {
                        userSetMimicBlock(itemstack, connectorTileEntity.getMimicBlock(), context);
                    }
                }
            } else if (block == FacadeModule.FACADE.get()) {
                BlockEntity te = world.getBlockEntity(pos);
                if (!(te instanceof IFacadeSupport facade)) {
                    return InteractionResult.FAIL;
                }
                if (facade.getMimicBlock() == null) {
                    return InteractionResult.FAIL;
                }
                userSetMimicBlock(itemstack, facade.getMimicBlock(), context);
            } else {
                userSetMimicBlock(itemstack, state, context);
            }
            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.FAIL;
        }
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level worldIn, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flag) {
        super.appendHoverText(stack, worldIn, tooltip, flag);
        tooltipBuilder.get().makeTooltip(Tools.getId(this), stack, tooltip, flag);
    }
}
