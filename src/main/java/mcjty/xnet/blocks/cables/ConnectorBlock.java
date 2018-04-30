package mcjty.xnet.blocks.cables;

import mcjty.lib.varia.EnergyTools;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ProbeMode;
import mcjty.theoneprobe.api.TextStyleClass;
import mcjty.xnet.XNet;
import mcjty.xnet.api.channels.IConnectable;
import mcjty.xnet.api.keys.ConsumerId;
import mcjty.xnet.blocks.controller.TileEntityController;
import mcjty.xnet.blocks.facade.FacadeBlockId;
import mcjty.xnet.blocks.facade.FacadeItemBlock;
import mcjty.xnet.blocks.generic.CableColor;
import mcjty.xnet.blocks.generic.GenericCableBakedModel;
import mcjty.xnet.blocks.generic.GenericCableBlock;
import mcjty.xnet.blocks.redstoneproxy.RedstoneProxyBlock;
import mcjty.xnet.blocks.router.TileEntityRouter;
import mcjty.xnet.config.GeneralConfiguration;
import mcjty.xnet.gui.GuiProxy;
import mcjty.xnet.init.ModBlocks;
import mcjty.xnet.multiblock.ColorId;
import mcjty.xnet.multiblock.WorldBlob;
import mcjty.xnet.multiblock.XNetBlobData;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ConnectorBlock extends GenericCableBlock implements ITileEntityProvider {

    public static final String CONNECTOR = "connector";

    public ConnectorBlock() {
        this(CONNECTOR);
    }

    public ConnectorBlock(String name) {
        super(Material.IRON, name);
        initTileEntity();
    }

    protected void initTileEntity() {
        GameRegistry.registerTileEntity(ConnectorTileEntity.class, XNet.MODID + "_connector");
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items) {
        for (CableColor value : CableColor.VALUES) {
            if (value != CableColor.ROUTING || this != NetCableSetup.advancedConnectorBlock) {
                items.add(updateColorInStack(new ItemStack(this, 1, value.ordinal()), value));
            }
        }
    }


    @Override
    public TileEntity createNewTileEntity(World world, int i) {
        return null;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState metadata) {
        return new ConnectorTileEntity();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            player.openGui(XNet.instance, GuiProxy.GUI_CONNECTOR, world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    public void harvestBlock(World worldIn, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity te, ItemStack stack) {
        if (te instanceof ConnectorTileEntity) {
            // If we are in mimic mode then the drop will be the facade as the connector will remain there
            ConnectorTileEntity connectorTileEntity = (ConnectorTileEntity) te;
            if (connectorTileEntity.getMimicBlock() != null) {
                ItemStack item = new ItemStack(ModBlocks.facadeBlock);
                FacadeItemBlock.setMimicBlock(item, connectorTileEntity.getMimicBlock());
                connectorTileEntity.setMimicBlock(null);
                spawnAsEntity(worldIn, pos, item);
                return;
            }
        }
        super.harvestBlock(worldIn, player, pos, state, te, stack);
    }


    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof ConnectorTileEntity) {
            ConnectorTileEntity connectorTileEntity = (ConnectorTileEntity) te;
            if (connectorTileEntity.getMimicBlock() == null) {
                this.onBlockHarvested(world, pos, state, player);
                return world.setBlockState(pos, net.minecraft.init.Blocks.AIR.getDefaultState(), world.isRemote ? 11 : 3);
            } else {
                // We are in mimic mode. Don't remove the connector
                this.onBlockHarvested(world, pos, state, player);
                if(player.capabilities.isCreativeMode) {
                    connectorTileEntity.setMimicBlock(null);
                }
            }
        } else {
            return super.removedByPlayer(state, world, pos, player, willHarvest);
        }
        return true;
    }


    @Override
    @Optional.Method(modid = "theoneprobe")
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world, IBlockState blockState, IProbeHitData data) {
        super.addProbeInfo(mode, probeInfo, player, world, blockState, data);
        TileEntity te = world.getTileEntity(data.getPos());
        if (te instanceof ConnectorTileEntity) {
            String name = ((ConnectorTileEntity) te).getConnectorName();
            if (!name.isEmpty()) {
                probeInfo.text(TextStyleClass.LABEL + "Name: " + TextStyleClass.INFO + name);
            }
        }
    }


    @Override
    public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
        IExtendedBlockState extendedBlockState = (IExtendedBlockState) super.getExtendedState(state, world, pos);
        IBlockState mimicBlock = getMimicBlock(world, pos);
        if (mimicBlock != null) {
            return extendedBlockState.withProperty(FACADEID, new FacadeBlockId(mimicBlock));
        } else {
            return extendedBlockState;
        }
    }


    @Override
    @SideOnly(Side.CLIENT)
    public void initModel() {
        super.initModel();
        // To make sure that our ISBM model is chosen for all states we use this custom state mapper:
        StateMapperBase ignoreState = new StateMapperBase() {
            @Override
            protected ModelResourceLocation getModelResourceLocation(IBlockState iBlockState) {
                return GenericCableBakedModel.modelConnector;
            }
        };
        ModelLoader.setCustomStateMapper(this, ignoreState);
    }

//    @Override
//    @SideOnly(Side.CLIENT)
//    public void initItemModel() {
        // For our item model we want to use a normal json model. This has to be called in
        // ClientProxy.init (not preInit) so that's why it is a separate method.
//        Item itemBlock = ForgeRegistries.ITEMS.getValue(getRegistryName());
//        ModelResourceLocation itemModelResourceLocation = new ModelResourceLocation(getRegistryName(), "inventory");
//        final int DEFAULT_ITEM_SUBTYPE = 0;
//        Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(itemBlock, DEFAULT_ITEM_SUBTYPE, itemModelResourceLocation);
//    }


    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
        checkRedstone(world, pos);
    }

    @Override
    public void onNeighborChange(IBlockAccess blockAccess, BlockPos pos, BlockPos neighbor) {
        if (blockAccess instanceof World) {
            World world = (World) blockAccess;
            if (!world.isRemote) {
                TileEntity te = world.getTileEntity(pos);
                if (te instanceof ConnectorTileEntity) {
                    ConnectorTileEntity connector = (ConnectorTileEntity) te;
                    connector.possiblyMarkNetworkDirty(neighbor);
                }
            }
        }
    }

    @Override
    public boolean shouldCheckWeakPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return false;
    }

    private void checkRedstone(World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof ConnectorTileEntity) {
            int powered = world.isBlockIndirectlyGettingPowered(pos);
            ConnectorTileEntity genericTileEntity = (ConnectorTileEntity) te;
            genericTileEntity.setPowerInput(powered);
        }
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    @Override
    public int getWeakPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return getRedstoneOutput(state, world, pos, side);
    }

    @Override
    public int getStrongPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return getRedstoneOutput(state, world, pos, side);
    }

    protected int getRedstoneOutput(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        TileEntity te = world.getTileEntity(pos);
        if (state.getBlock() instanceof ConnectorBlock && te instanceof ConnectorTileEntity) {
            ConnectorTileEntity connector = (ConnectorTileEntity) te;
            return connector.getPowerOut(side.getOpposite());
        }
        return 0;
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        // When our block is placed down we force a re-render of adjacent blocks to make sure their ISBM model is updated
        world.markBlockRangeForRenderUpdate(pos.add(-1, -1, -1), pos.add(1, 1, 1));
        return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer);
    }

    @Override
    protected ConnectorType getConnectorType(@Nonnull CableColor color, IBlockAccess world, BlockPos connectorPos, EnumFacing facing) {
        BlockPos pos = connectorPos.offset(facing);
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if ((block instanceof NetCableBlock || block instanceof ConnectorBlock) && state.getValue(COLOR) == color) {
            return ConnectorType.CABLE;
        } else if (isConnectable(world, connectorPos, facing) && color != CableColor.ROUTING) {
            return ConnectorType.BLOCK;
        } else if (isConnectableRouting(world, pos) && color == CableColor.ROUTING) {
            return ConnectorType.BLOCK;
        } else {
            return ConnectorType.NONE;
        }
    }

    public static boolean isConnectableRouting(IBlockAccess world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if (te == null) {
            return false;
        }
        if (te instanceof TileEntityRouter) {
            return true;
        }
        return false;
    }

    public static boolean isConnectable(IBlockAccess world, BlockPos connectorPos, EnumFacing facing) {

        BlockPos pos = connectorPos.offset(facing);
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (block.isAir(state, world, pos)) {
            return false;
        }

        ConnectorTileEntity connectorTE = (ConnectorTileEntity) world.getTileEntity(connectorPos);
        if (connectorTE == null) {
            return false;
        }

        if (!connectorTE.isEnabled(facing)) {
            return false;
        }


        TileEntity te = world.getTileEntity(pos);

        if (block instanceof IConnectable) {
            IConnectable.ConnectResult result = ((IConnectable) block).canConnect(world, connectorPos, pos, te, facing);
            if (result == IConnectable.ConnectResult.NO) {
                return false;
            } else if (result == IConnectable.ConnectResult.YES) {
                return true;
            }
        }
        for (IConnectable connectable : XNet.xNetApi.getConnectables()) {
            IConnectable.ConnectResult result = connectable.canConnect(world, connectorPos, pos, te, facing);
            if (result == IConnectable.ConnectResult.NO) {
                return false;
            } else if (result == IConnectable.ConnectResult.YES) {
                return true;
            }
        }

        if (block instanceof ConnectorBlock) {
            return false;
        }
        if (block instanceof RedstoneProxyBlock || block == Blocks.REDSTONE_LAMP || block == Blocks.LIT_REDSTONE_LAMP ||
                block == Blocks.PISTON || block == Blocks.STICKY_PISTON) {
            return true;
        }
        if (block.canConnectRedstone(state, world, pos, null) || state.canProvidePower()) {
            return true;
        }
        if (te == null) {
            return false;
        }
        if (te instanceof IInventory) {
            return true;
        }
        if (EnergyTools.isEnergyTE(te)) {
            return true;
        }
        if (te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
            return true;
        }
        if (te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null)) {
            return true;
        }
        if (te instanceof TileEntityController) {
            return true;
        }
        if (te instanceof TileEntityRouter) {
            return true;
        }
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        IBlockState mimicBlock = getMimicBlock(blockAccess, pos);
        if (mimicBlock == null) {
            return false;
        } else {
            return mimicBlock.shouldSideBeRendered(blockAccess, pos, side);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
        return true; // delegated to GenericCableBakedModel#getQuads
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess blockAccess, BlockPos pos, IBlockState state, int fortune) {
        super.getDrops(drops, blockAccess, pos, state, fortune);
        if (blockAccess instanceof World) {
            World world = (World) blockAccess;
            for (ItemStack drop : drops) {
                if (!drop.hasTagCompound()) {
                    drop.setTagCompound(new NBTTagCompound());
                }
                WorldBlob worldBlob = XNetBlobData.getBlobData(world).getWorldBlob(world);
                ConsumerId consumer = worldBlob.getConsumerAt(pos);
                if (consumer != null) {
                    drop.getTagCompound().setInteger("consumerId", consumer.getId());
                }
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, ITooltipFlag adv) {
        super.addInformation(stack, player, tooltip, adv);

        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            tooltip.add(TextFormatting.BLUE + "Place connector next to block or");
            tooltip.add(TextFormatting.BLUE + "machine that should be connected");
            tooltip.add(TextFormatting.BLUE + "to the network");
            boolean advanced = this == NetCableSetup.advancedConnectorBlock;
            int maxrf = advanced ? GeneralConfiguration.maxRfAdvancedConnector : GeneralConfiguration.maxRfConnector;
            tooltip.add(TextFormatting.GRAY + "" + TextFormatting.BOLD + "Max RF: " + TextFormatting.WHITE + maxrf);
            if (advanced) {
                tooltip.add(TextFormatting.GRAY + "Allow access to different sides");
                tooltip.add(TextFormatting.GRAY + "Supports faster item transfer");
            }
        } else {
            tooltip.add(TextFormatting.WHITE + XNet.SHIFT_MESSAGE);
        }

    }

    @Override
    public void createCableSegment(World world, BlockPos pos, ItemStack stack) {
        ConsumerId consumer;
        if (!stack.isEmpty() && stack.hasTagCompound() && stack.getTagCompound().hasKey("consumerId")) {
            consumer = new ConsumerId(stack.getTagCompound().getInteger("consumerId"));
        } else {
            XNetBlobData blobData = XNetBlobData.getBlobData(world);
            WorldBlob worldBlob = blobData.getWorldBlob(world);
            consumer = worldBlob.newConsumer();
        }
        createCableSegment(world, pos, consumer);
    }

    public void createCableSegment(World world, BlockPos pos, ConsumerId consumer) {
        XNetBlobData blobData = XNetBlobData.getBlobData(world);
        WorldBlob worldBlob = blobData.getWorldBlob(world);
        CableColor color = world.getBlockState(pos).getValue(COLOR);
        worldBlob.createNetworkConsumer(pos, new ColorId(color.ordinal()+1), consumer);
        blobData.save(world);
    }

    public static boolean isAdvancedConnector(World world, BlockPos pos) {
        Block block = world.getBlockState(pos).getBlock();
        if (block instanceof GenericCableBlock) {
            return ((GenericCableBlock) block).isAdvancedConnector();
        }
        return false;
    }

    @Override
    public boolean isAdvancedConnector() {
        return false;
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World worldIn, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn, boolean isActualState) {
        IBlockState mimicBlock = getMimicBlock(worldIn, pos);
        if (mimicBlock != null) {
            mimicBlock = mimicBlock.getActualState(worldIn, pos);
            mimicBlock.getBlock().addCollisionBoxToList(mimicBlock, worldIn, pos, entityBox, collidingBoxes, entityIn, true);
            return;
        }

        addCollisionBoxToList(pos, entityBox, collidingBoxes, AABB_CENTER);

        for (EnumFacing facing : EnumFacing.VALUES) {
            ConnectorType connectorType = getConnectorType(state.getValue(COLOR), worldIn, pos, facing);
            if (connectorType == ConnectorType.CABLE) {
                addCollisionBoxToList(pos, entityBox, collidingBoxes, AABBS[facing.getIndex()]);
            } else if (connectorType == ConnectorType.BLOCK) {
                addCollisionBoxToList(pos, entityBox, collidingBoxes, AABBS_CONNECTOR[facing.getIndex()]);
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World worldIn, BlockPos pos) {
        IBlockState mimicBlock = getMimicBlock(worldIn, pos);
        return mimicBlock != null ? mimicBlock.getSelectedBoundingBox(worldIn, pos) : super.getSelectedBoundingBox(state, worldIn, pos);
    }
}
