package mcjty.xnet.blocks.generic;

import mcjty.lib.varia.OrientationTools;
import mcjty.xnet.blocks.cables.ConnectorType;
import mcjty.xnet.blocks.facade.IFacadeSupport;
import mcjty.xnet.multiblock.ColorId;
import mcjty.xnet.multiblock.WorldBlob;
import mcjty.xnet.multiblock.XNetBlobData;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.common.ToolType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class GenericCableBlock extends Block {

    // Properties that indicate if there is the same block in a certain direction.
    public static final EnumProperty<ConnectorType> NORTH = EnumProperty.<ConnectorType>create("north", ConnectorType.class);
    public static final EnumProperty<ConnectorType> SOUTH = EnumProperty.<ConnectorType>create("south", ConnectorType.class);
    public static final EnumProperty<ConnectorType> WEST = EnumProperty.<ConnectorType>create("west", ConnectorType.class);
    public static final EnumProperty<ConnectorType> EAST = EnumProperty.<ConnectorType>create("east", ConnectorType.class);
    public static final EnumProperty<ConnectorType> UP = EnumProperty.<ConnectorType>create("up", ConnectorType.class);
    public static final EnumProperty<ConnectorType> DOWN = EnumProperty.<ConnectorType>create("down", ConnectorType.class);

    public static final ModelProperty<BlockState> FACADEID = new ModelProperty<>();
    public static final EnumProperty<CableColor> COLOR = EnumProperty.<CableColor>create("color", CableColor.class);


    public static final AxisAlignedBB AABB_EMPTY = new AxisAlignedBB(0, 0, 0, 0, 0, 0);
    public static final AxisAlignedBB AABB_CENTER = new AxisAlignedBB(.4, .4, .4, .6, .6, .6);

    public static final AxisAlignedBB AABBS[] = new AxisAlignedBB[]{
            new AxisAlignedBB(.4, 0, .4, .6, .4, .6),
            new AxisAlignedBB(.4, .6, .4, .6, 1, .6),
            new AxisAlignedBB(.4, .4, 0, .6, .6, .4),
            new AxisAlignedBB(.4, .4, .6, .6, .6, 1),
            new AxisAlignedBB(0, .4, .4, .4, .6, .6),
            new AxisAlignedBB(.6, .4, .4, 1, .6, .6)
    };

    public static final AxisAlignedBB AABBS_CONNECTOR[] = new AxisAlignedBB[]{
            new AxisAlignedBB(.2, 0, .2, .8, .1, .8),
            new AxisAlignedBB(.2, .9, .2, .8, 1, .8),
            new AxisAlignedBB(.2, .2, 0, .8, .8, .1),
            new AxisAlignedBB(.2, .2, .9, .8, .8, 1),
            new AxisAlignedBB(0, .2, .2, .1, .8, .8),
            new AxisAlignedBB(.9, .2, .2, 1, .8, .8)
    };


    public GenericCableBlock(Material material, String name) {
        super(Properties.create(material)
                .hardnessAndResistance(1.0f)
                .sound(SoundType.METAL)
                .harvestLevel(0)
                .harvestTool(ToolType.PICKAXE)
        );
        setRegistryName(name);
    }

//    public static boolean activateBlock(Block block, World world, BlockPos pos, BlockState state, PlayerEntity player, Hand hand, Direction facing, float hitX, float hitY, float hitZ) {
//        return block.onBlockActivated(world, pos, state, player, hand, facing, hitX, hitY, hitZ);
//    }

    @Override
    public ItemStack getItem(IBlockReader worldIn, BlockPos pos, BlockState state) {
        ItemStack item = super.getItem(worldIn, pos, state);
        return updateColorInStack(item, state.get(COLOR));
    }

    protected ItemStack updateColorInStack(ItemStack item, CableColor color) {
        if (color != null) {
            CompoundNBT tag = item.getOrCreateTag();
            // @todo 1.14
//            CompoundNBT display = new CompoundNBT();
//            String unlocname = getUnlocalizedName() + "_" + color.getName() + ".name";
//            display.putString("LocName", unlocname);
//            tag.put("display", display);
        }
        return item;
    }

    // @todo 1.14
//    @Override
//    public int damageDropped(BlockState state) {
//        return state.getValue(COLOR).ordinal();
//    }

//    @SideOnly(Side.CLIENT)
//    public void initModel() {
//        ResourceLocation name = getRegistryName();
//        for (CableColor color : CableColor.VALUES) {
//            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(this), color.ordinal(), new ModelResourceLocation(new ResourceLocation(name.getResourceDomain(), name.getResourcePath()+"item"), "color=" + color.name()));
//        }
//    }

//    @SideOnly(Side.CLIENT)
//    public void initItemModel() {
//    }

    @Nullable
    protected BlockState getMimicBlock(IBlockReader blockAccess, BlockPos pos) {
        TileEntity te = blockAccess.getTileEntity(pos);
        if (te instanceof IFacadeSupport) {
            return ((IFacadeSupport) te).getMimicBlock();
        } else {
            return null;
        }
    }

    // @todo 1.14
//    @SideOnly(Side.CLIENT)
//    public void initColorHandler(BlockColors blockColors) {
//        blockColors.registerBlockColorHandler((state, world, pos, tintIndex) -> {
//            BlockState mimicBlock = getMimicBlock(world, pos);
//            return mimicBlock != null ? blockColors.colorMultiplier(mimicBlock, world, pos, tintIndex) : -1;
//        }, this);
//    }

//    @Override
//    @SideOnly(Side.CLIENT)
//    public AxisAlignedBB getSelectedBoundingBox(BlockState state, World worldIn, BlockPos pos) {
//        return AABB_EMPTY;
//    }

    private static class ConnectionKey {
        private final ConnectorType north;
        private final ConnectorType south;
        private final ConnectorType east;
        private final ConnectorType west;
        private final ConnectorType up;
        private final ConnectorType down;

        public ConnectionKey(ConnectorType north, ConnectorType south, ConnectorType east, ConnectorType west, ConnectorType up, ConnectorType down) {
            this.north = north;
            this.south = south;
            this.east = east;
            this.west = west;
            this.up = up;
            this.down = down;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConnectionKey that = (ConnectionKey) o;
            return north == that.north &&
                    south == that.south &&
                    east == that.east &&
                    west == that.west &&
                    up == that.up &&
                    down == that.down;
        }

        @Override
        public int hashCode() {
            return Objects.hash(north, south, east, west, up, down);
        }
    }

    private static final Map<ConnectionKey, VoxelShape> SHAPE_CACHE = new HashMap<>();

    private static final VoxelShape SHAPE_CABLE_NORTH = VoxelShapes.create(.4, .4, 0, .6, .6, .4);
    private static final VoxelShape SHAPE_CABLE_SOUTH = VoxelShapes.create(.4, .4, .6, .6, .6, 1);
    private static final VoxelShape SHAPE_CABLE_WEST = VoxelShapes.create(0, .4, .4, .4, .6, .6);
    private static final VoxelShape SHAPE_CABLE_EAST = VoxelShapes.create(.6, .4, .4, 1, .6, .6);
    private static final VoxelShape SHAPE_CABLE_UP = VoxelShapes.create(.4, .6, .4, .6, 1, .6);
    private static final VoxelShape SHAPE_CABLE_DOWN = VoxelShapes.create(.4, 0, .4, .6, .4, .6);

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context) {
        if (getMimicBlock(world, pos) != null) {
            // In mimic mode we use original block
            return getMimicBlock(world, pos).getShape(world, pos, context);
        }
        CableColor color = state.get(COLOR);
        ConnectorType north = getConnectorType(color, world, pos, Direction.NORTH);
        ConnectorType south = getConnectorType(color, world, pos, Direction.SOUTH);
        ConnectorType west = getConnectorType(color, world, pos, Direction.WEST);
        ConnectorType east = getConnectorType(color, world, pos, Direction.EAST);
        ConnectorType up = getConnectorType(color, world, pos, Direction.UP);
        ConnectorType down = getConnectorType(color, world, pos, Direction.DOWN);
        ConnectionKey key = new ConnectionKey(north, south, east, west, up, down);
        if (!SHAPE_CACHE.containsKey(key)) {
            VoxelShape shape = VoxelShapes.create(.4, .4, .4, .6, .6, .6);
            if (north != ConnectorType.NONE) {
                shape = VoxelShapes.combineAndSimplify(shape, SHAPE_CABLE_NORTH, IBooleanFunction.OR);
            }
            if (south != ConnectorType.NONE) {
                shape = VoxelShapes.combineAndSimplify(shape, SHAPE_CABLE_SOUTH, IBooleanFunction.OR);
            }
            if (west != ConnectorType.NONE) {
                shape = VoxelShapes.combineAndSimplify(shape, SHAPE_CABLE_WEST, IBooleanFunction.OR);
            }
            if (east != ConnectorType.NONE) {
                shape = VoxelShapes.combineAndSimplify(shape, SHAPE_CABLE_EAST, IBooleanFunction.OR);
            }
            if (up != ConnectorType.NONE) {
                shape = VoxelShapes.combineAndSimplify(shape, SHAPE_CABLE_UP, IBooleanFunction.OR);
            }
            if (down != ConnectorType.NONE) {
                shape = VoxelShapes.combineAndSimplify(shape, SHAPE_CABLE_DOWN, IBooleanFunction.OR);
            }
            SHAPE_CACHE.put(key, shape);
        }
        return SHAPE_CACHE.get(key);
    }


//    @Override
//    @SideOnly(Side.CLIENT)
//    @Optional.Method(modid = "waila")
//    public List<String> getWailaBody(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
//        return currenttip;
//    }
//
//    @Override
//    @Optional.Method(modid = "theoneprobe")
//    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, PlayerEntity player, World world, BlockState blockState, IProbeHitData data) {
//        WorldBlob worldBlob = XNetBlobData.getBlobData(world).getWorldBlob(world);
//
//        if (mode == ProbeMode.DEBUG) {
//            BlobId blobId = worldBlob.getBlobAt(data.getPos());
//            if (blobId != null) {
//                probeInfo.text(TextStyleClass.LABEL + "Blob: " + TextStyleClass.INFO + blobId.getId());
//            }
//            ColorId colorId = worldBlob.getColorAt(data.getPos());
//            if (colorId != null) {
//                probeInfo.text(TextStyleClass.LABEL + "Color: " + TextStyleClass.INFO + colorId.getId());
//            }
//        }
//
//        Set<NetworkId> networks = worldBlob.getNetworksAt(data.getPos());
//        for (NetworkId network : networks) {
//            if (mode == ProbeMode.DEBUG) {
//                probeInfo.text(TextStyleClass.LABEL + "Network: " + TextStyleClass.INFO + network.getId() + ", V: " +
//                    worldBlob.getNetworkVersion(network));
//            } else {
//                probeInfo.text(TextStyleClass.LABEL + "Network: " + TextStyleClass.INFO + network.getId());
//            }
//        }
//
//        ConsumerId consumerId = worldBlob.getConsumerAt(data.getPos());
//        if (consumerId != null) {
//            probeInfo.text(TextStyleClass.LABEL + "Consumer: " + TextStyleClass.INFO + consumerId.getId());
//        }
//    }

    public boolean isAdvancedConnector() {
        return false;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        originalOnBlockPlacedBy(world, pos, state, placer, stack);
        if (!world.isRemote) {
            createCableSegment(world, pos, stack);
        }
    }

    protected void originalOnBlockPlacedBy(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
    }

    public void createCableSegment(World world, BlockPos pos, ItemStack stack) {
        XNetBlobData blobData = XNetBlobData.getBlobData(world);
        WorldBlob worldBlob = blobData.getWorldBlob(world);
        CableColor color = world.getBlockState(pos).get(COLOR);
        worldBlob.createCableSegment(pos, new ColorId(color.ordinal()+1));
        blobData.save();
    }

    @Override
    public void onReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (newState.getBlock() != state.getBlock()) {
            unlinkBlock(world, pos);
        }
        originalBreakBlock(state, world, pos, newState, isMoving);
    }

    public void unlinkBlock(World world, BlockPos pos) {
        if (!world.isRemote) {
            XNetBlobData blobData = XNetBlobData.getBlobData(world);
            WorldBlob worldBlob = blobData.getWorldBlob(world);
            worldBlob.removeCableSegment(pos);
            blobData.save();
        }
    }

    protected void originalBreakBlock(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onReplaced(state, world, pos, newState, isMoving);
    }

//    @Override
//    @SideOnly(Side.CLIENT)
//    public boolean shouldSideBeRendered(BlockState blockState, IBlockAccess blockAccess, BlockPos pos, Direction side) {
//        return false;
//    }
//
//    @Override
//    public boolean isBlockNormalCube(BlockState blockState) {
//        return false;
//    }
//
//    @Override
//    public boolean isOpaqueCube(BlockState blockState) {
//        return false;
//    }
//
//    @Override
//    public boolean isFullCube(BlockState state) {
//        return false;
//    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(COLOR, NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    public void onNeighborChange(BlockState state, IWorldReader world, BlockPos pos, BlockPos neighbor) {

    }

    @Override
    public void neighborChanged(BlockState p_220069_1_, World p_220069_2_, BlockPos p_220069_3_, Block p_220069_4_, BlockPos p_220069_5_, boolean p_220069_6_) {
        super.neighborChanged(p_220069_1_, p_220069_2_, p_220069_3_, p_220069_4_, p_220069_5_, p_220069_6_);
    }

    @Override
    public void updateNeighbors(BlockState state, IWorld world, BlockPos pos, int flags) {
        super.updateNeighbors(state, world, pos, flags);
        for (Direction direction : OrientationTools.DIRECTION_VALUES) {
            BlockPos p = pos.offset(direction);
            BlockState original = world.getBlockState(p);
            BlockState newstate = original.updatePostPlacement(direction.getOpposite(), state, world, p, pos);
            replaceBlock(original, newstate, world, p, flags);

        }
    }

    @Override
    public BlockState updatePostPlacement(BlockState state, Direction direction, BlockState neighbourState, IWorld world, BlockPos current, BlockPos offset) {
        return calculateState(world, current);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getPos();
        return calculateState(world, pos);
    }

    private BlockState calculateState(IWorld world, BlockPos pos) {
        BlockState state = getDefaultState();
        CableColor color = state.get(COLOR);

        ConnectorType north = getConnectorType(color, world, pos, Direction.NORTH);
        ConnectorType south = getConnectorType(color, world, pos, Direction.SOUTH);
        ConnectorType west = getConnectorType(color, world, pos, Direction.WEST);
        ConnectorType east = getConnectorType(color, world, pos, Direction.EAST);
        ConnectorType up = getConnectorType(color, world, pos, Direction.UP);
        ConnectorType down = getConnectorType(color, world, pos, Direction.DOWN);

        return state
                .with(NORTH, north)
                .with(SOUTH, south)
                .with(WEST, west)
                .with(EAST, east)
                .with(UP, up)
                .with(DOWN, down);
    }

    protected abstract ConnectorType getConnectorType(@Nonnull CableColor thisColor, IBlockReader world, BlockPos pos, Direction facing);
}
