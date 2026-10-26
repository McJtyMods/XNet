package mcjty.xnet.modules.cables.blocks;

import mcjty.lib.builder.TooltipBuilder;
import mcjty.lib.container.ContainerFactory;
import mcjty.lib.container.GenericContainer;
import mcjty.lib.gui.ManualEntry;
import mcjty.lib.tileentity.GenericTileEntity;
import mcjty.lib.tooltips.ITooltipSettings;
import mcjty.lib.varia.ComponentFactory;
import mcjty.lib.varia.EnergyTools;
import mcjty.lib.varia.Tools;
import mcjty.rftoolsbase.api.xnet.channels.IConnectable;
import mcjty.rftoolsbase.api.xnet.keys.ConsumerId;
import mcjty.rftoolsbase.tools.ManualHelper;
import mcjty.xnet.XNet;
import mcjty.xnet.modules.cables.CableColor;
import mcjty.xnet.modules.cables.CableModule;
import mcjty.xnet.modules.cables.ConnectorType;
import mcjty.xnet.modules.cables.data.CableItemData;
import mcjty.xnet.modules.controller.blocks.TileEntityController;
import mcjty.xnet.modules.facade.FacadeModule;
import mcjty.xnet.modules.facade.blocks.FacadeBlockItem;
import mcjty.xnet.modules.router.blocks.TileEntityRouter;
import mcjty.xnet.modules.various.blocks.RedstoneProxyBlock;
import mcjty.xnet.modules.wireless.blocks.TileEntityWirelessRouter;
import mcjty.xnet.multiblock.ColorId;
import mcjty.xnet.multiblock.WorldBlob;
import mcjty.xnet.multiblock.XNetBlobData;
import mcjty.xnet.setup.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import mcjty.lib.varia.Lazy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static mcjty.lib.builder.TooltipBuilder.gold;
import static mcjty.lib.builder.TooltipBuilder.header;
import static mcjty.lib.builder.TooltipBuilder.key;
import static mcjty.lib.builder.TooltipBuilder.parameter;
import static mcjty.xnet.apiimpl.Constants.TAG_CONSUMER_ID;
import static mcjty.xnet.utils.I18nConstants.BLOCK_CONNECTOR;

public class ConnectorBlock extends GenericCableBlock implements ITooltipSettings, EntityBlock {

    public static final ManualEntry MANUAL = ManualHelper.create("rftoolsbase:simple/connector");
    private final Lazy<TooltipBuilder> tooltipBuilder = Lazy.of(() -> new TooltipBuilder()
            .info(key("message.xnet.shiftmessage"))
            .infoShift(header(), gold(stack -> isAdvancedConnector()),
                    parameter("info", stack -> Integer.toString(isAdvancedConnector() ? Config.maxRfAdvancedConnector.get() : Config.maxRfConnector.get()))));

    public ConnectorBlock(CableBlockType type, net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        super(type, properties);
    }

    @Override
    public ManualEntry getManualEntry() {
        return MANUAL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new ConnectorTileEntity(pPos, pState);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide()) {
            BlockEntity te = world.getBlockEntity(pos);
            if (te instanceof GenericTileEntity) {
                GenericTileEntity genericTileEntity = (GenericTileEntity) te;
                mcjty.lib.container.MenuTools.open(player, new MenuProvider() {
                    @Override
                    @Nonnull
                    public Component getDisplayName() {
                        return ComponentFactory.literal(BLOCK_CONNECTOR.i18n());
                    }

                    @Nonnull
                    @Override
                    public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inventory, @Nonnull Player player) {
                        return new GenericContainer(CableModule.CONTAINER_CONNECTOR, id, ContainerFactory.EMPTY, genericTileEntity, player);
                    }
                }, data -> { data.writeBlockPos(pos); });
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void playerDestroy(@Nonnull net.minecraft.server.level.ServerLevel worldIn, @Nonnull net.minecraft.server.level.ServerPlayer player, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nullable BlockEntity te, @Nonnull ItemStack stack) {
        if (te instanceof ConnectorTileEntity connectorTileEntity) {
            // If we are in mimic mode then the drop will be the facade as the connector will remain there
            if (connectorTileEntity.getMimicBlock() != null) {
                ItemStack item = new ItemStack(FacadeModule.FACADE.get());
                FacadeBlockItem.setMimicBlock(item, connectorTileEntity.getMimicBlock());
                connectorTileEntity.setMimicBlock(null);
                popResource(worldIn, pos, item);
                return;
            }
        }
        super.playerDestroy(worldIn, player, pos, state, te, stack);
    }


    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(@Nonnull BlockState state, @Nonnull Level world, @Nonnull BlockPos pos, @Nonnull Block blockIn, net.minecraft.world.level.redstone.Orientation orientation, boolean isMoving) {
        checkRedstone(world, pos);
        if (!world.isClientSide() && world.getBlockEntity(pos) instanceof ConnectorTileEntity connector) {
            for (Direction side : Direction.values()) connector.possiblyMarkNetworkDirty(pos.relative(side));
        }
        super.neighborChanged(state, world, pos, blockIn, orientation, isMoving);
    }


    private void checkRedstone(Level world, BlockPos pos) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof ConnectorTileEntity connector) {
//            int powered = world.isBlockIndirectlyGettingPowered(pos);
            int powered = world.getBestNeighborSignal(pos); // @todo 1.14 check
            connector.setPowerInput(powered);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isSignalSource(@Nonnull BlockState state) {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getSignal(@Nonnull BlockState state, @Nonnull BlockGetter world, @Nonnull BlockPos pos, @Nonnull Direction side) {
        return getRedstoneOutput(state, world, pos, side);
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getDirectSignal(@Nonnull BlockState state, @Nonnull BlockGetter world, @Nonnull BlockPos pos, @Nonnull Direction side) {
        return getRedstoneOutput(state, world, pos, side);
    }

    protected int getRedstoneOutput(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
        BlockEntity te = world.getBlockEntity(pos);
        if (state.getBlock() instanceof ConnectorBlock && te instanceof ConnectorTileEntity connector) {
            return connector.getPowerOut(side.getOpposite());
        }
        return 0;
    }

    @Override
    protected ConnectorType getConnectorType(@Nonnull CableColor color, BlockGetter world, BlockPos connectorPos, Direction facing) {
        BlockPos pos = connectorPos.relative(facing);
        BlockState state = world.getBlockState(pos);
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

    public static boolean isConnectableRouting(BlockGetter world, BlockPos pos) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te == null) {
            return false;
        }
        if (te instanceof TileEntityRouter || te instanceof TileEntityWirelessRouter) {
            return true;
        }
        return false;
    }

    public static boolean isConnectable(BlockGetter world, BlockPos connectorPos, Direction facing) {

        BlockPos pos = connectorPos.relative(facing);
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (state.isAir()) {
            return false;
        }

        BlockEntity tileEntity = world.getBlockEntity(connectorPos);
        if (!(tileEntity instanceof ConnectorTileEntity connectorTE)) {
            return false;
        }

        if (!connectorTE.isEnabled(facing)) {
            return false;
        }


        BlockEntity te = world.getBlockEntity(pos);

        if (block instanceof IConnectable connectable) {
            IConnectable.ConnectResult result = connectable.canConnect(world, connectorPos, pos, te, facing);
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
        if (block instanceof RedstoneProxyBlock || block == Blocks.REDSTONE_LAMP ||
                block == Blocks.PISTON || block == Blocks.STICKY_PISTON) {
            return true;
        }
        if (state.isSignalSource()) {
            return true;
        }
        if (te == null) {
            return false;
        }
        if (EnergyTools.isEnergyTE(te, null)) {
            return true;
        }
        Level level = te.getLevel();
        if (mcjty.lib.varia.CapabilityTools.itemHandler(net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(level, te.getBlockPos(), null)) != null) {
            return true;
        }
        if (mcjty.lib.varia.CapabilityTools.fluidHandler(net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.find(level, te.getBlockPos(), null)) != null) {
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

    // @todo 1.14
//    @Override
//    @SideOnly(Side.CLIENT)
//    public boolean shouldSideBeRendered(BlockState blockState, IBlockAccess blockAccess, BlockPos pos, Direction side) {
//        BlockState mimicBlock = getMimicBlock(blockAccess, pos);
//        if (mimicBlock == null) {
//            return false;
//        } else {
//            return mimicBlock.shouldSideBeRendered(blockAccess, pos, side);
//        }
//    }

    // @todo 1.15
//    @Override
//    public boolean canRenderInLayer(BlockState state, BlockRenderLayer layer) {
//        return true;    // delegated to GenericCableBakedModel#getQuads
//    }
//
    @Nonnull
    @Override
    public List<ItemStack> getDrops(@Nonnull BlockState state, @Nonnull LootParams.Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);
        ServerLevel world = builder.getLevel();
        for (ItemStack drop : drops) {
            WorldBlob worldBlob = XNetBlobData.get(world).getWorldBlob(world);
            Vec3 pos = builder.getOptionalParameter(LootContextParams.ORIGIN);
            ConsumerId consumer = worldBlob.getConsumerAt(new BlockPos((int) pos.x, (int) pos.y, (int) pos.z));
            if (consumer != null) {
                drop.set(CableModule.ITEM_CABLE_ITEM_DATA.get(), new CableItemData(consumer));
            }
        }
        return drops;
    }

    public void appendHoverText(@Nonnull ItemStack stack, Item.TooltipContext context, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flagIn) {
        tooltipBuilder.get().makeTooltip(Tools.getId(this), stack, tooltip, flagIn);
    }

    @Override
    public void createCableSegment(Level world, BlockPos pos, ItemStack stack) {
        ConsumerId consumer;
        if (!stack.isEmpty() && stack.get(CableModule.ITEM_CABLE_ITEM_DATA.get()) != null) {
            consumer = stack.get(CableModule.ITEM_CABLE_ITEM_DATA.get()).id();
        } else {
            XNetBlobData blobData = XNetBlobData.get(world);
            WorldBlob worldBlob = blobData.getWorldBlob(world);
            consumer = worldBlob.newConsumer();
        }
        createCableSegment(world, pos, consumer);
    }

    public void createCableSegment(Level world, BlockPos pos, ConsumerId consumer) {
        XNetBlobData blobData = XNetBlobData.get(world);
        WorldBlob worldBlob = blobData.getWorldBlob(world);
        CableColor color = world.getBlockState(pos).getValue(COLOR);
        worldBlob.createNetworkConsumer(pos, new ColorId(color.ordinal() + 1), consumer);
        blobData.save();
    }

    public static boolean isAdvancedConnector(@Nonnull Level world, @Nonnull BlockPos pos) {
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
}
