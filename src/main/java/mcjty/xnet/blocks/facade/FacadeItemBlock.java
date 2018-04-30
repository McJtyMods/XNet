package mcjty.xnet.blocks.facade;

import mcjty.xnet.blocks.cables.ConnectorTileEntity;
import mcjty.xnet.blocks.cables.NetCableSetup;
import mcjty.xnet.blocks.generic.GenericCableBlock;
import mcjty.xnet.init.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class FacadeItemBlock extends ItemBlock {

    public FacadeItemBlock(FacadeBlock block) {
        super(block);
    }

    public static void setMimicBlock(@Nonnull ItemStack item, IBlockState mimicBlock) {
        NBTTagCompound tagCompound = new NBTTagCompound();
        tagCompound.setString("regName", mimicBlock.getBlock().getRegistryName().toString());
        tagCompound.setInteger("meta", mimicBlock.getBlock().getMetaFromState(mimicBlock));
        item.setTagCompound(tagCompound);
    }

    public static IBlockState getMimicBlock(@Nonnull ItemStack stack) {
        NBTTagCompound tagCompound = stack.getTagCompound();
        if (tagCompound == null || !tagCompound.hasKey("regName")) {
            return Blocks.COBBLESTONE.getDefaultState();
        } else {
            String regName = tagCompound.getString("regName");
            int meta = tagCompound.getInteger("meta");
            Block value = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(regName));
            return value.getStateFromMeta(meta);
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean canPlaceBlockOnSide(World worldIn, BlockPos pos, EnumFacing side, EntityPlayer player, ItemStack stack) {
        return true;
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        ItemStack itemstack = player.getHeldItem(hand);

        if (!itemstack.isEmpty()) {
            if (block == NetCableSetup.netCableBlock) {
                IBlockState mimicBlock = getMimicBlock(itemstack);
                if (!canMimicFit(state.getBoundingBox(world, pos), mimicBlock.getBoundingBox(world, pos))) {
                    if (world.isRemote) {
                        player.sendStatusMessage(new TextComponentString(mimicBlock.getBlock().getLocalizedName() + " is too small to fit"), false);
                    }
                    return EnumActionResult.FAIL;
                }

                int i = this.getMetadata(itemstack.getMetadata());
                FacadeBlock facadeBlock = (FacadeBlock) this.block;
                IBlockState placementState = facadeBlock.getPlacementState(world, pos, facing, hitX, hitY, hitZ, i, player).withProperty(GenericCableBlock.COLOR, state.getValue(GenericCableBlock.COLOR));

                if (placeBlockAt(itemstack, player, world, pos, facing, hitX, hitY, hitZ, placementState)) {
                    SoundType soundtype = world.getBlockState(pos).getBlock().getSoundType(world.getBlockState(pos), world, pos, player);
                    world.playSound(player, pos, soundtype.getPlaceSound(), SoundCategory.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
                    TileEntity te = world.getTileEntity(pos);
                    if (te instanceof FacadeTileEntity) {
                        ((FacadeTileEntity) te).setMimicBlock(mimicBlock);
                    }
                    int amount = -1;
                    itemstack.grow(amount);
                }
            } else if (block == NetCableSetup.connectorBlock || block == NetCableSetup.advancedConnectorBlock) {
                IBlockState mimicBlock = getMimicBlock(itemstack);
                if (!canMimicFit(state.getBoundingBox(world, pos), mimicBlock.getBoundingBox(world, pos))) {
                    if (world.isRemote) {
                        player.sendStatusMessage(new TextComponentString(mimicBlock.getBlock().getLocalizedName() + " is too small to fit"), false);
                    }
                    return EnumActionResult.FAIL;
                }
                TileEntity te = world.getTileEntity(pos);
                if (te instanceof ConnectorTileEntity) {
                    ConnectorTileEntity connectorTileEntity = (ConnectorTileEntity) te;
                    if (connectorTileEntity.getMimicBlock() == null) {
                        connectorTileEntity.setMimicBlock(mimicBlock);
                        SoundType soundtype = world.getBlockState(pos).getBlock().getSoundType(world.getBlockState(pos), world, pos, player);
                        world.playSound(player, pos, soundtype.getPlaceSound(), SoundCategory.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
                        int amount = -1;
                        itemstack.grow(amount);
                    } else {
                        return EnumActionResult.FAIL;
                    }
                }
            } else if (block == ModBlocks.facadeBlock) {
                return EnumActionResult.FAIL;
            } else {
                if (!canMimicFit(GenericCableBlock.AABB_CENTER, state.getBoundingBox(world, pos))) {
                    if (world.isRemote) {
                        player.sendStatusMessage(new TextComponentString(block.getLocalizedName() + " is too small to mimic"), false);
                    }
                    return EnumActionResult.FAIL;
                }

                setMimicBlock(itemstack, state);
                if (world.isRemote) {
                    player.sendStatusMessage(new TextComponentString("Facade is now mimicing " + block.getLocalizedName()), false);
                }
            }
            return EnumActionResult.SUCCESS;
        } else {
            return EnumActionResult.FAIL;
        }
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World playerIn, List<String> tooltip, ITooltipFlag advanced) {
        super.addInformation(stack, null, tooltip, advanced);
        NBTTagCompound tagCompound = stack.getTagCompound();
        if (tagCompound == null || !tagCompound.hasKey("regName")) {
            tooltip.add(TextFormatting.BLUE + "Right or sneak-right click on block to mimic");
            tooltip.add(TextFormatting.BLUE + "Right or sneak-right click on cable/connector to hide");
        } else {
            String regName = tagCompound.getString("regName");
            int meta = tagCompound.getInteger("meta");
            Block value = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(regName));
            if (value != null) {
                ItemStack s = new ItemStack(value, 1, meta);
                if (s.getItem() != null) {
                    tooltip.add(TextFormatting.BLUE + "Mimicing " + s.getDisplayName());
                }
            }
        }
    }

    public boolean canMimicFit(AxisAlignedBB cableBB, AxisAlignedBB mimicBB) {
        return mimicBB.minX <= cableBB.minX && mimicBB.maxX >= cableBB.maxX && mimicBB.minY <= cableBB.maxY && mimicBB.maxY >= cableBB.maxY && mimicBB.minZ <= cableBB.minZ && mimicBB.maxZ >= cableBB.maxZ;
    }
}
