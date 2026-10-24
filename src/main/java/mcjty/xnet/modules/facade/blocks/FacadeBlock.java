package mcjty.xnet.modules.facade.blocks;

import mcjty.xnet.modules.cables.CableColor;
import mcjty.xnet.modules.cables.CableModule;
import mcjty.xnet.modules.cables.blocks.NetCableBlock;
import mcjty.xnet.modules.facade.FacadeModule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FacadeBlock extends NetCableBlock implements EntityBlock {

    public FacadeBlock(CableBlockType type, net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        super(type, properties);
        // @todo 1.14
//        setHardness(0.8f);
    }

//    @Override
//    protected ItemBlock createItemBlock() {
//        return new FacadeItemBlock(this);
//    }

//    @Override
//    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items) {
//        items.add(new ItemStack(this));
//    }

//    protected void initTileEntity() {
//        GameRegistry.registerTileEntity(FacadeTileEntity.class, XNet.MODID + ":facade");
//    }

    // @todo 1.14
//    @Nullable
//    @Override
//    public RayTraceResult collisionRayTrace(BlockState blockState, World world, BlockPos pos, Vector3d start, Vector3d end) {
//        // We do not want the raytracing that happens in the GenericCableBlock
//        return super.originalCollisionRayTrace(blockState, world, pos, start, end);
//    }

    // @todo 1.15
//    @Override
//    public boolean canRenderInLayer(BlockState state, BlockRenderLayer layer) {
//        return true;    // delegated to GenericCableBakedModel#getQuads
//    }


    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new FacadeTileEntity(pPos, pState);
    }

    @Override
    public void playerDestroy(@Nonnull Level worldIn, @Nonnull Player player, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nullable BlockEntity te, @Nonnull ItemStack stack) {
        ItemStack item = new ItemStack(FacadeModule.FACADE.get());
        BlockState mimicBlock;
        if (te instanceof FacadeTileEntity) {
            mimicBlock = ((FacadeTileEntity) te).getMimicBlock();
        } else {
            mimicBlock = Blocks.COBBLESTONE.defaultBlockState();
        }
        FacadeBlockItem.setMimicBlock(item, mimicBlock);

        popResource(worldIn, pos, item);
    }

    private boolean replaceWithCable(LevelAccessor world, BlockPos pos, BlockState state) {
        CableColor color = state.getValue(COLOR);
        BlockState defaultState = CableModule.NETCABLE.get().defaultBlockState().setValue(COLOR, color);
        BlockState newState = this.calculateState(world, pos, defaultState);
        return world.setBlock(pos, newState, world.isClientSide()
                ? Block.UPDATE_ALL + Block.UPDATE_IMMEDIATE
                : Block.UPDATE_ALL);
    }

    @Override
    public void destroy(LevelAccessor world, @Nonnull BlockPos pos, @Nonnull BlockState state) {
        if (world.isClientSide()) {
            replaceWithCable(world, pos, state);
        }
    }


    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level world, BlockPos pos, Player player, ItemStack tool, boolean willHarvest, FluidState fluid) {
        return replaceWithCable(world, pos, state);
    }

    // @todo 1.14
//    @Override
//    public BlockState getExtendedState(BlockState state, IBlockAccess world, BlockPos pos) {
//        IExtendedBlockState extendedBlockState = (IExtendedBlockState) state;
//        BlockState mimicBlock = getMimicBlock(world, pos);
//        if (mimicBlock != null) {
//            return extendedBlockState.withProperty(FACADEID, new FacadeBlockId(mimicBlock));
//        } else {
//            return extendedBlockState;
//        }
//    }


    @Override
    public void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel world, BlockPos pos, boolean isMoving) {
        // Breaking a facade has no effect on blob network
        super.affectNeighborsAfterRemoval(state, world, pos, isMoving);
    }


    // @todo 1.14
//    @Override
//    @SideOnly(Side.CLIENT)
//    public boolean shouldSideBeRendered(BlockState blockState, IBlockAccess blockAccess, BlockPos pos, Direction side) {
//        BlockState mimicBlock = getMimicBlock(blockAccess, pos);
//        return mimicBlock == null ? true : mimicBlock.shouldSideBeRendered(blockAccess, pos, side);
//    }
//
//    @Override
//    @SideOnly(Side.CLIENT)
//    public boolean canRenderInLayer(BlockState state, BlockRenderLayer layer) {
//        return true; // delegated to FacadeBakedModel#getQuads
//    }

    // @todo 1.14
//    @Override
//    public boolean isBlockNormalCube(BlockState blockState) {
//        return true;
//    }
//
//    @Override
//    public boolean isOpaqueCube(BlockState blockState) {
//        return true;
//    }
//
//    @Override
//    public boolean doesSideBlockRendering(BlockState state, IBlockAccess world, BlockPos pos, Direction face) {
//        BlockState mimicBlock = getMimicBlock(world, pos);
//        return mimicBlock == null ? true : mimicBlock.doesSideBlockRendering(world, pos, face);
//    }
//
//    @Override
//    public boolean isFullCube(BlockState state) {
//        return true;
//    }


}
