package mcjty.xnet.modules.cables.blocks;

import mcjty.xnet.modules.cables.CableColor;
import mcjty.xnet.modules.cables.CableSetup;
import mcjty.xnet.modules.cables.ConnectorType;
import mcjty.xnet.modules.facade.IFacadeSupport;
import mcjty.xnet.modules.facade.blocks.FacadeBlock;
import mcjty.xnet.multiblock.ColorId;
import mcjty.xnet.multiblock.WorldBlob;
import mcjty.xnet.multiblock.XNetBlobData;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.common.ToolType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public abstract class GenericCableBlock extends Block {

    // Properties that indicate if there is the same block in a certain direction.
    public static final EnumProperty<ConnectorType> NORTH = EnumProperty.<ConnectorType>create("north", ConnectorType.class);
    public static final EnumProperty<ConnectorType> SOUTH = EnumProperty.<ConnectorType>create("south", ConnectorType.class);
    public static final EnumProperty<ConnectorType> WEST = EnumProperty.<ConnectorType>create("west", ConnectorType.class);
    public static final EnumProperty<ConnectorType> EAST = EnumProperty.<ConnectorType>create("east", ConnectorType.class);
    public static final EnumProperty<ConnectorType> UP = EnumProperty.<ConnectorType>create("up", ConnectorType.class);
    public static final EnumProperty<ConnectorType> DOWN = EnumProperty.<ConnectorType>create("down", ConnectorType.class);

    public static final EnumProperty<CableColor> COLOR = EnumProperty.<CableColor>create("color", CableColor.class);

    public static final ModelProperty<BlockState> FACADEID = new ModelProperty<>();

    private static VoxelShape[] shapeCache = null;

    private static final VoxelShape SHAPE_CABLE_NORTH = VoxelShapes.create(.4, .4, 0, .6, .6, .4);
    private static final VoxelShape SHAPE_CABLE_SOUTH = VoxelShapes.create(.4, .4, .6, .6, .6, 1);
    private static final VoxelShape SHAPE_CABLE_WEST = VoxelShapes.create(0, .4, .4, .4, .6, .6);
    private static final VoxelShape SHAPE_CABLE_EAST = VoxelShapes.create(.6, .4, .4, 1, .6, .6);
    private static final VoxelShape SHAPE_CABLE_UP = VoxelShapes.create(.4, .6, .4, .6, 1, .6);
    private static final VoxelShape SHAPE_CABLE_DOWN = VoxelShapes.create(.4, 0, .4, .6, .4, .6);

    private static final VoxelShape SHAPE_BLOCK_NORTH = VoxelShapes.create(.2, .2, 0, .8, .8, .1);
    private static final VoxelShape SHAPE_BLOCK_SOUTH = VoxelShapes.create(.2, .2, .9, .8, .8, 1);
    private static final VoxelShape SHAPE_BLOCK_WEST = VoxelShapes.create(0, .2, .2, .1, .8, .8);
    private static final VoxelShape SHAPE_BLOCK_EAST = VoxelShapes.create(.9, .2, .2, 1, .8, .8);
    private static final VoxelShape SHAPE_BLOCK_UP = VoxelShapes.create(.2, .9, .2, .8, 1, .8);
    private static final VoxelShape SHAPE_BLOCK_DOWN = VoxelShapes.create(.2, 0, .2, .8, .1, .8);

    private final CableBlockType type;

    public static enum CableBlockType {
        CABLE,
        CONNECTOR,
        ADVANCED_CONNECTOR,
        FACADE
    }

    public GenericCableBlock(Material material, CableBlockType type) {
        super(Properties.create(material)
                .hardnessAndResistance(1.0f)
                .sound(SoundType.METAL)
                .harvestLevel(0)
                .harvestTool(ToolType.PICKAXE)
        );
        makeShapes();
        this.type = type;
    }

    private int calculateShapeIndex(ConnectorType north, ConnectorType south, ConnectorType west, ConnectorType east, ConnectorType up, ConnectorType down) {
        int l = ConnectorType.values().length;
        return ((((south.ordinal() * l + north.ordinal()) * l + west.ordinal()) * l + east.ordinal()) * l + up.ordinal()) * l + down.ordinal();
    }

    private void makeShapes() {
        if (shapeCache == null) {
            int length = ConnectorType.values().length;
            shapeCache = new VoxelShape[length * length * length * length * length * length];

            for (ConnectorType up : ConnectorType.VALUES) {
                for (ConnectorType down : ConnectorType.VALUES) {
                    for (ConnectorType north : ConnectorType.VALUES) {
                        for (ConnectorType south : ConnectorType.VALUES) {
                            for (ConnectorType east : ConnectorType.VALUES) {
                                for (ConnectorType west : ConnectorType.VALUES) {
                                    int idx = calculateShapeIndex(north, south, west, east, up, down);
                                    shapeCache[idx] = makeShape(north, south, west, east, up, down);
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    private VoxelShape makeShape(ConnectorType north, ConnectorType south, ConnectorType west, ConnectorType east, ConnectorType up, ConnectorType down) {
        VoxelShape shape = VoxelShapes.create(.4, .4, .4, .6, .6, .6);
        shape = combineShape(shape, north, SHAPE_CABLE_NORTH, SHAPE_BLOCK_NORTH);
        shape = combineShape(shape, south, SHAPE_CABLE_SOUTH, SHAPE_BLOCK_SOUTH);
        shape = combineShape(shape, west, SHAPE_CABLE_WEST, SHAPE_BLOCK_WEST);
        shape = combineShape(shape, east, SHAPE_CABLE_EAST, SHAPE_BLOCK_EAST);
        shape = combineShape(shape, up, SHAPE_CABLE_UP, SHAPE_BLOCK_UP);
        shape = combineShape(shape, down, SHAPE_CABLE_DOWN, SHAPE_BLOCK_DOWN);
        return shape;
    }

    private VoxelShape combineShape(VoxelShape shape, ConnectorType connectorType, VoxelShape cableShape, VoxelShape blockShape) {
        if (connectorType == ConnectorType.CABLE) {
            return VoxelShapes.combineAndSimplify(shape, cableShape, IBooleanFunction.OR);
        } else if (connectorType == ConnectorType.BLOCK) {
            return VoxelShapes.combineAndSimplify(shape, blockShape, IBooleanFunction.OR);
        } else {
            return shape;
        }
    }

    private Item getItem(CableColor color) {
        switch (type) {
            case CABLE:
                switch (color) {
                    case BLUE: return CableSetup.NETCABLE_BLUE.get();
                    case RED: return CableSetup.NETCABLE_RED.get();
                    case YELLOW: return CableSetup.NETCABLE_YELLOW.get();
                    case GREEN: return CableSetup.NETCABLE_GREEN.get();
                    case ROUTING: return CableSetup.NETCABLE_ROUTING.get();
                }
                break;
            case CONNECTOR:
                switch (color) {
                    case BLUE: return CableSetup.CONNECTOR_BLUE.get();
                    case RED: return CableSetup.CONNECTOR_RED.get();
                    case YELLOW: return CableSetup.CONNECTOR_YELLOW.get();
                    case GREEN: return CableSetup.CONNECTOR_GREEN.get();
                    case ROUTING: return CableSetup.CONNECTOR_ROUTING.get();
                }
                break;
            case ADVANCED_CONNECTOR:
                switch (color) {
                    case BLUE: return CableSetup.ADVANCED_CONNECTOR_BLUE.get();
                    case RED: return CableSetup.ADVANCED_CONNECTOR_RED.get();
                    case YELLOW: return CableSetup.ADVANCED_CONNECTOR_YELLOW.get();
                    case GREEN: return CableSetup.ADVANCED_CONNECTOR_GREEN.get();
                    case ROUTING: return CableSetup.ADVANCED_CONNECTOR_ROUTING.get();
                }
                break;
        }
        return Items.AIR;
    }

    @Override
    public ItemStack getItem(IBlockReader worldIn, BlockPos pos, BlockState state) {
        return new ItemStack(getItem(state.get(COLOR)));
    }

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

    @Override
    public boolean isSolid(BlockState state) {
        return false;
    }

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
        int index = calculateShapeIndex(north, south, west, east, up, down);
        return shapeCache[index];
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
        BlockState blockState = calculateState(world, pos, state);
        if (state != blockState) {
            world.setBlockState(pos, blockState);
        }
    }

    protected void originalOnBlockPlacedBy(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
    }

    public void createCableSegment(World world, BlockPos pos, ItemStack stack) {
        XNetBlobData blobData = XNetBlobData.get(world);
        WorldBlob worldBlob = blobData.getWorldBlob(world);
        CableColor color = world.getBlockState(pos).get(COLOR);
        worldBlob.createCableSegment(pos, new ColorId(color.ordinal()+1));
        blobData.save();
    }

    @Override
    public void onReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (newState.getBlock() != state.getBlock() && !(newState.getBlock() instanceof FacadeBlock)) {
            unlinkBlock(world, pos);
        }
        super.onReplaced(state, world, pos, newState, isMoving);
    }

    public void unlinkBlock(World world, BlockPos pos) {
        if (!world.isRemote) {
            XNetBlobData blobData = XNetBlobData.get(world);
            WorldBlob worldBlob = blobData.getWorldBlob(world);
            worldBlob.removeCableSegment(pos);
            blobData.save();
        }
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(COLOR, NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

//    @Override
//    public void onNeighborChange(BlockState state, IWorldReader world, BlockPos pos, BlockPos neighbor) {
//
//    }
//
//    @Override
//    public void neighborChanged(BlockState p_220069_1_, World p_220069_2_, BlockPos p_220069_3_, Block p_220069_4_, BlockPos p_220069_5_, boolean p_220069_6_) {
//        super.neighborChanged(p_220069_1_, p_220069_2_, p_220069_3_, p_220069_4_, p_220069_5_, p_220069_6_);
//    }

//    @Override
//    public void updateNeighbors(BlockState state, IWorld world, BlockPos pos, int flags) {
//        super.updateNeighbors(state, world, pos, flags);
//        for (Direction direction : OrientationTools.DIRECTION_VALUES) {
//            BlockPos p = pos.offset(direction);
//            BlockState original = world.getBlockState(p);
//            BlockState newstate = original.updatePostPlacement(direction.getOpposite(), state, world, p, pos);
//            replaceBlock(original, newstate, world, p, flags);
//
//        }
//    }


    @Override
    public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) {
        return super.getDrops(state, builder);
    }

    @Override
    public BlockState updatePostPlacement(BlockState state, Direction direction, BlockState neighbourState, IWorld world, BlockPos current, BlockPos offset) {
        return calculateState(world, current, state);
    }

//    @Override
//    public void onNeighborChange(BlockState state, IWorldReader world, BlockPos pos, BlockPos neighbor) {
//
//    }
//
//    @Override
//    public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
//        super.neighborChanged(state, worldIn, pos, blockIn, fromPos, isMoving);
//    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getPos();
        BlockState state = calculateState(world, pos, getDefaultState());
        System.out.println("state = " + state);
        return state;
    }

    @Nonnull
    public BlockState calculateState(IWorld world, BlockPos pos, BlockState state) {
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
