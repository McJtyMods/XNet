package mcjty.xnet.blocks.facade;

import mcjty.xnet.XNet;
import mcjty.xnet.blocks.cables.NetCableBlock;
import mcjty.xnet.blocks.cables.NetCableSetup;
import mcjty.xnet.blocks.generic.CableColor;
import mcjty.xnet.init.ModBlocks;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class FacadeBlock extends NetCableBlock implements ITileEntityProvider {

    public static final String FACADE = "facade";

    public FacadeBlock() {
        super(Material.IRON, FACADE);
        initTileEntity();
        setHardness(0.8f);
    }

    @Override
    protected ItemBlock createItemBlock() {
        return new FacadeItemBlock(this);
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items) {
        items.add(new ItemStack(this));
    }

    protected void initTileEntity() {
        GameRegistry.registerTileEntity(FacadeTileEntity.class, XNet.MODID + "_facade");
    }

    @Nullable
    @Override
    public RayTraceResult collisionRayTrace(IBlockState blockState, World world, BlockPos pos, Vec3d start, Vec3d end) {
        IBlockState mimicBlock = getMimicBlock(world, pos);
        if (mimicBlock != null) {
            return mimicBlock.getBlock().collisionRayTrace(mimicBlock, world, pos, start, end);
        }
        // We do not want the raytracing that happens in the GenericCableBlock
        return super.originalCollisionRayTrace(blockState, world, pos, start, end);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int i) {
        return null;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState metadata) {
        return new FacadeTileEntity();
    }


    @Override
    public void harvestBlock(World worldIn, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity te, ItemStack stack) {
        ItemStack item = new ItemStack(ModBlocks.facadeBlock);
        IBlockState mimicBlock;
        if (te instanceof FacadeTileEntity) {
            mimicBlock = ((FacadeTileEntity) te).getMimicBlock();
        } else {
            mimicBlock = Blocks.COBBLESTONE.getDefaultState();
        }
        FacadeItemBlock.setMimicBlock(item, mimicBlock);

        spawnAsEntity(worldIn, pos, item);
    }

    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        CableColor color = state.getValue(COLOR);
        this.onBlockHarvested(world, pos, state, player);
        return world.setBlockState(pos, NetCableSetup.netCableBlock.getDefaultState().withProperty(COLOR, color), world.isRemote ? 11 : 3);
    }

    @Override
    public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
        IExtendedBlockState extendedBlockState = (IExtendedBlockState) state;
        IBlockState mimicBlock = getMimicBlock(world, pos);
        if (mimicBlock != null) {
            return extendedBlockState.withProperty(FACADEID, new FacadeBlockId(mimicBlock));
        } else {
            return extendedBlockState;
        }
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        // Breaking a facade has no effect on blob network
        originalBreakBlock(world, pos, state);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void initModel() {
        // To make sure that our ISBM model is chosen for all states we use this custom state mapper:
        StateMapperBase ignoreState = new StateMapperBase() {
            @Override
            protected ModelResourceLocation getModelResourceLocation(IBlockState iBlockState) {
                return FacadeBakedModel.modelFacade;
            }
        };
        ModelLoader.setCustomStateMapper(this, ignoreState);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void initItemModel() {
        // For our item model we want to use a normal json model. This has to be called in
        // ClientProxy.init (not preInit) so that's why it is a separate method.
        Item itemBlock = ForgeRegistries.ITEMS.getValue(new ResourceLocation(XNet.MODID, FACADE));
        ModelResourceLocation itemModelResourceLocation = new ModelResourceLocation(getRegistryName(), "inventory");
        final int DEFAULT_ITEM_SUBTYPE = 0;
        Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(itemBlock, DEFAULT_ITEM_SUBTYPE, itemModelResourceLocation);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        IBlockState mimicBlock = getMimicBlock(blockAccess, pos);
        return mimicBlock == null ? true : mimicBlock.shouldSideBeRendered(blockAccess, pos, side);
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World worldIn, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn, boolean isActualState) {
        IBlockState mimicBlock = getMimicBlock(worldIn, pos);
        if (mimicBlock != null) {
            mimicBlock.getBlock().addCollisionBoxToList(mimicBlock, worldIn, pos, entityBox, collidingBoxes, entityIn, isActualState);
            return;
        }
        addCollisionBoxToList(pos, entityBox, collidingBoxes, FULL_BLOCK_AABB);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World worldIn, BlockPos pos) {
        IBlockState mimicBlock = getMimicBlock(worldIn, pos);
        return mimicBlock != null ? mimicBlock.getSelectedBoundingBox(worldIn, pos) : FULL_BLOCK_AABB;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
        return true; // delegated to FacadeBakedModel#getQuads
    }

    @Override
    public boolean isBlockNormalCube(IBlockState blockState) {
        return true;
    }

    @Override
    public boolean isOpaqueCube(IBlockState blockState) {
        return true;
    }

    @Override
    public boolean doesSideBlockRendering(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing face) {
        IBlockState mimicBlock = getMimicBlock(world, pos);
        return mimicBlock == null ? true : mimicBlock.doesSideBlockRendering(world, pos, face);
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return true;
    }
}
