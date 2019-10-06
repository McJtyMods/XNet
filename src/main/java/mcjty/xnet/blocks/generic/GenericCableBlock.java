package mcjty.xnet.blocks.generic;

import mcjty.lib.McJtyRegister;
import mcjty.lib.blocks.DamageMetadataItemBlock;
import mcjty.lib.compat.theoneprobe.TOPInfoProvider;
import mcjty.lib.compat.waila.WailaInfoProvider;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ProbeMode;
import mcjty.theoneprobe.api.TextStyleClass;
import mcjty.xnet.XNet;
import mcjty.xnet.api.keys.ConsumerId;
import mcjty.xnet.api.keys.NetworkId;
import mcjty.xnet.blocks.cables.ConnectorType;
import mcjty.xnet.blocks.facade.FacadeProperty;
import mcjty.xnet.blocks.facade.IFacadeSupport;
import mcjty.xnet.multiblock.BlobId;
import mcjty.xnet.multiblock.ColorId;
import mcjty.xnet.multiblock.WorldBlob;
import mcjty.xnet.multiblock.XNetBlobData;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public abstract class GenericCableBlock extends Block implements WailaInfoProvider, TOPInfoProvider {

    // Properties that indicate if there is the same block in a certain direction.
    public static final UnlistedPropertyBlockType NORTH = new UnlistedPropertyBlockType("north");
    public static final UnlistedPropertyBlockType SOUTH = new UnlistedPropertyBlockType("south");
    public static final UnlistedPropertyBlockType WEST = new UnlistedPropertyBlockType("west");
    public static final UnlistedPropertyBlockType EAST = new UnlistedPropertyBlockType("east");
    public static final UnlistedPropertyBlockType UP = new UnlistedPropertyBlockType("up");
    public static final UnlistedPropertyBlockType DOWN = new UnlistedPropertyBlockType("down");

    public static final FacadeProperty FACADEID = new FacadeProperty("facadeid");
    public static final PropertyEnum<CableColor> COLOR = PropertyEnum.<CableColor>create("color", CableColor.class);


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
        super(material);
        setHardness(1.0f);
        setSoundType(SoundType.METAL);
        setHarvestLevel("pickaxe", 0);
        setUnlocalizedName(XNet.MODID + "." + name);
        setRegistryName(name);
        McJtyRegister.registerLater(this, XNet.instance, null);
        McJtyRegister.registerLater(createItemBlock().setRegistryName(name), XNet.instance);
        setCreativeTab(XNet.setup.getTab());
        setDefaultState(getDefaultState().withProperty(COLOR, CableColor.BLUE));
    }

    public static boolean activateBlock(Block block, World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        return block.onBlockActivated(world, pos, state, player, hand, facing, hitX, hitY, hitZ);
    }

    public static Collection<IProperty<?>> getPropertyKeys(IBlockState state) {
        return state.getPropertyKeys();
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        ItemStack item = super.getItem(worldIn, pos, state);
        return updateColorInStack(item, state.getValue(COLOR));
    }

    protected ItemStack updateColorInStack(ItemStack item, CableColor color) {
        if (color != null) {
            if (item.getTagCompound() == null) {
                item.setTagCompound(new NBTTagCompound());
            }
            NBTTagCompound display = new NBTTagCompound();
            String unlocname = getUnlocalizedName() + "_" + color.getName() + ".name";
            display.setString("LocName", unlocname);
            item.getTagCompound().setTag("display", display);
        }
        return item;
    }


    protected ItemBlock createItemBlock() {
        return new DamageMetadataItemBlock(this);
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(COLOR).ordinal();
    }

    @SideOnly(Side.CLIENT)
    public void initModel() {
        ResourceLocation name = getRegistryName();
        for (CableColor color : CableColor.VALUES) {
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(this), color.ordinal(), new ModelResourceLocation(new ResourceLocation(name.getResourceDomain(), name.getResourcePath()+"item"), "color=" + color.name()));
        }
    }

    @SideOnly(Side.CLIENT)
    public void initItemModel() {
    }

    @Nullable
    protected IBlockState getMimicBlock(IBlockAccess blockAccess, BlockPos pos) {
        TileEntity te = blockAccess.getTileEntity(pos);
        if (te instanceof IFacadeSupport) {
            return ((IFacadeSupport) te).getMimicBlock();
        } else {
            return null;
        }
    }

    @SideOnly(Side.CLIENT)
    public void initColorHandler(BlockColors blockColors) {
        blockColors.registerBlockColorHandler((state, world, pos, tintIndex) -> {
            IBlockState mimicBlock = getMimicBlock(world, pos);
            return mimicBlock != null ? blockColors.colorMultiplier(mimicBlock, world, pos, tintIndex) : -1;
        }, this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World worldIn, BlockPos pos) {
        return AABB_EMPTY;
    }

    @Nullable
    @Override
    public RayTraceResult collisionRayTrace(IBlockState blockState, World world, BlockPos pos, Vec3d start, Vec3d end) {
        IBlockState mimicBlock = getMimicBlock(world, pos);
        if (mimicBlock != null) {
            return mimicBlock.getBlock().collisionRayTrace(mimicBlock, world, pos, start, end);
        }
        Vec3d vec3d = start.subtract(pos.getX(), pos.getY(), pos.getZ());
        Vec3d vec3d1 = end.subtract(pos.getX(), pos.getY(), pos.getZ());
        RayTraceResult rc = checkIntersect(pos, vec3d, vec3d1, AABB_CENTER);
        if (rc != null) {
            return rc;
        }
        CableColor color = blockState.getValue(COLOR);

        for (EnumFacing facing : EnumFacing.VALUES) {
            ConnectorType type = getConnectorType(color, world, pos, facing);
            if (type != ConnectorType.NONE) {
                rc = checkIntersect(pos, vec3d, vec3d1, AABBS[facing.ordinal()]);
                if (rc != null) {
                    return rc;
                }
            }
            if (type == ConnectorType.BLOCK) {
                rc = checkIntersect(pos, vec3d, vec3d1, AABBS_CONNECTOR[facing.ordinal()]);
                if (rc != null) {
                    return rc;
                }
            }
        }
        return null;
    }

    private RayTraceResult checkIntersect(BlockPos pos, Vec3d vec3d, Vec3d vec3d1, AxisAlignedBB boundingBox) {
        RayTraceResult raytraceresult = boundingBox.calculateIntercept(vec3d, vec3d1);
        return raytraceresult == null ? null : new RayTraceResult(raytraceresult.hitVec.addVector(pos.getX(), pos.getY(), pos.getZ()), raytraceresult.sideHit, pos);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @Optional.Method(modid = "waila")
    public List<String> getWailaBody(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
        return currenttip;
    }

    @Override
    @Optional.Method(modid = "theoneprobe")
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world, IBlockState blockState, IProbeHitData data) {
        WorldBlob worldBlob = XNetBlobData.getBlobData(world).getWorldBlob(world);

        if (mode == ProbeMode.DEBUG) {
            BlobId blobId = worldBlob.getBlobAt(data.getPos());
            if (blobId != null) {
                probeInfo.text(TextStyleClass.LABEL + "Blob: " + TextStyleClass.INFO + blobId.getId());
            }
            ColorId colorId = worldBlob.getColorAt(data.getPos());
            if (colorId != null) {
                probeInfo.text(TextStyleClass.LABEL + "Color: " + TextStyleClass.INFO + colorId.getId());
            }
        }

        Set<NetworkId> networks = worldBlob.getNetworksAt(data.getPos());
        for (NetworkId network : networks) {
            if (mode == ProbeMode.DEBUG) {
                probeInfo.text(TextStyleClass.LABEL + "Network: " + TextStyleClass.INFO + network.getId() + ", V: " +
                    worldBlob.getNetworkVersion(network));
            } else {
                probeInfo.text(TextStyleClass.LABEL + "Network: " + TextStyleClass.INFO + network.getId());
            }
        }

        ConsumerId consumerId = worldBlob.getConsumerAt(data.getPos());
        if (consumerId != null) {
            probeInfo.text(TextStyleClass.LABEL + "Consumer: " + TextStyleClass.INFO + consumerId.getId());
        }
    }

    public boolean isAdvancedConnector() {
        return false;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        originalOnBlockPlacedBy(world, pos, state, placer, stack);
        if (!world.isRemote) {
            createCableSegment(world, pos, stack);
        }
    }

    protected void originalOnBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
    }

    public void createCableSegment(World world, BlockPos pos, ItemStack stack) {
        XNetBlobData blobData = XNetBlobData.getBlobData(world);
        WorldBlob worldBlob = blobData.getWorldBlob(world);
        CableColor color = world.getBlockState(pos).getValue(COLOR);
        worldBlob.createCableSegment(pos, new ColorId(color.ordinal()+1));
        blobData.save();
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        unlinkBlock(world, pos);
        originalBreakBlock(world, pos, state);
    }

    public void unlinkBlock(World world, BlockPos pos) {
        if (!world.isRemote) {
            XNetBlobData blobData = XNetBlobData.getBlobData(world);
            WorldBlob worldBlob = blobData.getWorldBlob(world);
            worldBlob.removeCableSegment(pos);
            blobData.save();
        }
    }

    protected void originalBreakBlock(World world, BlockPos pos, IBlockState state) {
        super.breakBlock(world, pos, state);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        return false;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    public boolean isBlockNormalCube(IBlockState blockState) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState blockState) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(COLOR, CableColor.VALUES[meta]);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(COLOR).ordinal();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        IProperty<?>[] listedProperties = new IProperty<?>[] { COLOR };
        IUnlistedProperty<?>[] unlistedProperties = new IUnlistedProperty<?>[] { NORTH, SOUTH, WEST, EAST, UP, DOWN,
            FACADEID};
        return new ExtendedBlockState(this, listedProperties, unlistedProperties);
    }

    @Override
    public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
        return getStateInternal(state, world, pos);
    }

    public IBlockState getStateInternal(IBlockState state, IBlockAccess world, BlockPos pos) {
        IExtendedBlockState extendedBlockState = (IExtendedBlockState) state;
        CableColor color = state.getValue(COLOR);

        ConnectorType north = getConnectorType(color, world, pos, EnumFacing.NORTH);
        ConnectorType south = getConnectorType(color, world, pos, EnumFacing.SOUTH);
        ConnectorType west = getConnectorType(color, world, pos, EnumFacing.WEST);
        ConnectorType east = getConnectorType(color, world, pos, EnumFacing.EAST);
        ConnectorType up = getConnectorType(color, world, pos, EnumFacing.UP);
        ConnectorType down = getConnectorType(color, world, pos, EnumFacing.DOWN);

        return extendedBlockState
                .withProperty(NORTH, north)
                .withProperty(SOUTH, south)
                .withProperty(WEST, west)
                .withProperty(EAST, east)
                .withProperty(UP, up)
                .withProperty(DOWN, down);
    }

    protected abstract ConnectorType getConnectorType(@Nonnull CableColor thisColor, IBlockAccess world, BlockPos pos, EnumFacing facing);
}
