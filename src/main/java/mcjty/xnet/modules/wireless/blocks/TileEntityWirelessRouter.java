package mcjty.xnet.modules.wireless.blocks;

import mcjty.lib.api.container.DefaultContainerProvider;
import mcjty.lib.bindings.GuiValue;
import mcjty.lib.blocks.BaseBlock;
import mcjty.lib.builder.BlockBuilder;
import mcjty.lib.builder.TooltipBuilder;
import mcjty.lib.container.GenericContainer;
import mcjty.lib.tileentity.Cap;
import mcjty.lib.tileentity.CapType;
import mcjty.lib.tileentity.GenericEnergyStorage;
import mcjty.lib.tileentity.TickingTileEntity;
import mcjty.lib.varia.LevelTools;
import mcjty.rftoolsbase.api.xnet.channels.IChannelType;
import mcjty.rftoolsbase.api.xnet.channels.IConnectorSettings;
import mcjty.rftoolsbase.api.xnet.keys.NetworkId;
import mcjty.rftoolsbase.api.xnet.keys.SidedConsumer;
import mcjty.rftoolsbase.tools.ManualHelper;
import mcjty.xnet.client.ControllerChannelClientInfo;
import mcjty.xnet.compat.XNetTOPDriver;
import mcjty.xnet.logic.LogicTools;
import mcjty.xnet.modules.cables.CableColor;
import mcjty.xnet.modules.router.blocks.TileEntityRouter;
import mcjty.xnet.modules.wireless.WirelessRouterModule;
import mcjty.xnet.multiblock.ColorId;
import mcjty.xnet.multiblock.WirelessChannelKey;
import mcjty.xnet.multiblock.WorldBlob;
import mcjty.xnet.multiblock.XNetBlobData;
import mcjty.xnet.multiblock.XNetWirelessChannels;
import mcjty.xnet.setup.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static mcjty.lib.api.container.DefaultContainerProvider.empty;
import static mcjty.xnet.apiimpl.Constants.TAG_INFO;
import static mcjty.xnet.modules.controller.blocks.TileEntityController.ERROR;
import static mcjty.xnet.modules.wireless.WirelessRouterModule.TYPE_WIRELESS_ROUTER;
import static mcjty.xnet.utils.I18nConstants.BLOCK_WIRELESS_ROUTER;

public final class TileEntityWirelessRouter extends TickingTileEntity {

    public static final int TIER_INVALID = -1;
    public static final int TIER_1 = 0;
    public static final int TIER_2 = 1;
    public static final int TIER_INF = 2;

    private boolean error = false;
    private int counter = 10;

    @GuiValue(name = "public")
    private boolean publicAccess = false;

    private int globalChannelVersion = -1;      // Used to detect if a wireless channel has been published and we might need to recheck

    @Cap(type = CapType.ENERGY)
    private final LazyOptional<GenericEnergyStorage> energyHandler = LazyOptional.of(() -> new GenericEnergyStorage(this, true, Config.wirelessRouterMaxRF.get(), Config.wirelessRouterRfPerTick.get()));

    @Cap(type = CapType.CONTAINER)
    private final LazyOptional<MenuProvider> screenHandler = LazyOptional.of(() -> new DefaultContainerProvider<GenericContainer>(BLOCK_WIRELESS_ROUTER.i18n())
            .containerSupplier(empty(WirelessRouterModule.CONTAINER_WIRELESS_ROUTER, this))
            .setupSync(this));

    public TileEntityWirelessRouter(BlockPos pos, BlockState state) {
        super(TYPE_WIRELESS_ROUTER.get(), pos, state);
    }

    public static BaseBlock createBlock() {
        return new BaseBlock(new BlockBuilder()
                .topDriver(XNetTOPDriver.DRIVER)
                .tileEntitySupplier(TileEntityWirelessRouter::new)
                .manualEntry(ManualHelper.create("xnet:simple/wireless/wireless_router"))
                .info(TooltipBuilder.key("message.xnet.shiftmessage"))
                .infoShift(TooltipBuilder.header(), TooltipBuilder.gold())
        ) {
            @Override
            protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
                super.createBlockStateDefinition(builder);
                builder.add(ERROR);
            }
        };
    }

    public boolean isPublicAccess() {
        return publicAccess;
    }

    public void setPublicAccess(boolean publicAccess) {
        this.publicAccess = publicAccess;
        setChanged();
    }

    @Override
    public void tickServer() {
        counter--;
        if (counter > 0) {
            return;
        }
        counter = 10;

        int version = XNetWirelessChannels.get(level).getGlobalChannelVersion();
        if (globalChannelVersion != version) {
            globalChannelVersion = version;
            NetworkId networkId = findRoutingNetwork();
            if (networkId != null) {
                WorldBlob worldBlob = XNetBlobData.get(level).getWorldBlob(level);
                worldBlob.markNetworkDirty(networkId);
            }
        }

        boolean err = false;
        int range = getAntennaRange();
        if (range < 0) {
            err = true;
        }

        if (!err) {
            NetworkId networkId = findRoutingNetwork();
            if (networkId != null) {
                LogicTools.consumers(level, networkId)
                        .forEach(consumerPos -> LogicTools.forEachRouter(level, consumerPos, r -> publishChannels(r, networkId)));
            }
        }

        setError(err);
    }

    private int getAntennaTier() {
        if (level.getBlockState(worldPosition.above()).getBlock() != WirelessRouterModule.ANTENNA_BASE.get()) {
            return TIER_INVALID;
        }
        Block aboveAntenna = level.getBlockState(worldPosition.above(2)).getBlock();
        if (aboveAntenna == WirelessRouterModule.ANTENNA_DISH.get()) {
            return TIER_INF;
        }
        if (aboveAntenna != WirelessRouterModule.ANTENNA.get()) {
            return TIER_INVALID;
        }
        if (level.getBlockState(worldPosition.above(3)).getBlock() == WirelessRouterModule.ANTENNA.get()) {
            return TIER_2;
        } else {
            return TIER_1;
        }
    }

    private int getAntennaRange() {
        int tier = getAntennaTier();
        return switch (tier) {
            case TIER_INVALID -> -1;
            case TIER_1 -> Config.antennaTier1Range.get();
            case TIER_2 -> Config.antennaTier2Range.get();
            case TIER_INF -> Integer.MAX_VALUE;
            default -> -1;
        };
    }

    private boolean inRange(TileEntityWirelessRouter otherRouter) {
        // Both wireless routers have to support the range
        int thisRange = getAntennaRange();
        int otherRange = otherRouter.getAntennaRange();
        if (thisRange >= Integer.MAX_VALUE && otherRange >= Integer.MAX_VALUE) {
            return true;
        }
        if (thisRange <= 0 || otherRange <= 0) {
            return false;
        }

        // If the dimension is different at this point there is no connection
        if (!level.dimension().equals(otherRouter.level.dimension())) {
            return false;
        }

        double maxSqdist = Math.min(thisRange, otherRange);
        maxSqdist *= maxSqdist;
        double sqdist = worldPosition.distSqr(otherRouter.worldPosition);
        return sqdist <= maxSqdist;
    }

    private boolean inRange(XNetWirelessChannels.WirelessRouterInfo wirelessRouter) {
        Level otherWorld = LevelTools.getLevel(level, wirelessRouter.getCoordinate().dimension());
        if (otherWorld == null) {
            return false;
        }
        for (BlockPos consumerPos : LogicTools.consumers(otherWorld, wirelessRouter.getNetworkId())) {
            if (LevelTools.isLoaded(otherWorld, consumerPos)) {
                if (LogicTools.findWirelessRouter(otherWorld, consumerPos, this::inRange)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void findRemoteChannelInfo(List<ControllerChannelClientInfo> list) {
        NetworkId network = findRoutingNetwork();
        if (network == null) {
            return;
        }

        XNetWirelessChannels wirelessData = XNetWirelessChannels.get(level);
        wirelessData.forEachChannel(getOwnerUUID(), channel -> {
                    // Find all wireless routers on a given channel but remove the ones on our own channel
                    channel.getRouters().values().forEach(routerInfo -> {
                        // Make sure this is not the same router
                        if (isDifferentRouter(network, routerInfo) && inRange(routerInfo)) {
                            // Find all routers on this network
                            Level otherWorld = LevelTools.getLevel(level, routerInfo.getCoordinate().dimension());
                            for (BlockPos consumerPos : LogicTools.consumers(otherWorld, routerInfo.getNetworkId())) {
                                if (otherWorld.hasChunkAt(consumerPos)) {
                                    // Range check not needed here since the check is already done on the wireless router
                                    LogicTools.forEachRouter(otherWorld, consumerPos, router -> router.findLocalChannelInfo(list, true, true));
                                }
                            }
                        }
                    });
                });
    }

    // Test if the given router is not a router on this network
    private boolean isDifferentRouter(NetworkId thisNetwork, XNetWirelessChannels.WirelessRouterInfo routerInfo) {
        return !routerInfo.getCoordinate().dimension().equals(level.dimension()) || !thisNetwork.equals(routerInfo.getNetworkId());
    }

//    private long getStoredPower() {
//        return energyHandler.map(h -> h.getEnergy()).orElse(0L);
//    }

    private void publishChannels(TileEntityRouter router, NetworkId networkId) {
        int tier = getAntennaTier();
        UUID ownerUUID = publicAccess ? null : getOwnerUUID();
        XNetWirelessChannels wirelessData = XNetWirelessChannels.get(level);
        energyHandler.ifPresent(h -> {
            router.forEachPublishedChannel((name, channelType) -> {
                long energyStored = h.getEnergy();
                if (Config.wirelessRouterRfPerChannel[tier].get() <= energyStored) {
                    h.consumeEnergy(Config.wirelessRouterRfPerChannel[tier].get());
                    wirelessData.transmitChannel(name, channelType, ownerUUID, level.dimension(),
                            worldPosition, networkId);
                }
            });
        });
    }

    public void addWirelessConnectors(Map<SidedConsumer, IConnectorSettings> connectors, String channelName, IChannelType type,
                                      @Nullable UUID owner) {
        WirelessChannelKey key = new WirelessChannelKey(channelName, type, owner);
        XNetWirelessChannels.WirelessChannelInfo info = XNetWirelessChannels.get(level).findChannel(key);
        if (info != null) {
            info.getRouters().keySet().forEach(routerPos -> {
                // Don't do this for ourselves
                if (!routerPos.dimension().equals(level.dimension()) || !routerPos.pos().equals(worldPosition)) {
                    ServerLevel otherWorld = LevelTools.getLevel(level, routerPos.dimension());
                    if (LevelTools.isLoaded(otherWorld, routerPos.pos())) {
                        BlockEntity otherTE = otherWorld.getBlockEntity(routerPos.pos());
                        if (otherTE instanceof TileEntityWirelessRouter otherRouter) {
                            if (inRange(otherRouter) && !otherRouter.inError()) {
                                NetworkId routingNetwork = otherRouter.findRoutingNetwork();
                                if (routingNetwork != null) {
                                    LogicTools.consumers(level, routingNetwork)
                                            .forEach(consumerPos -> LogicTools.forEachRouter(otherWorld, consumerPos, router -> {
                                                router.addConnectorsFromConnectedNetworks(connectors, channelName, type);
                                            }));
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    private void setError(boolean err) {
        if (error != err) {
            error = err;
            BlockState state = level.getBlockState(worldPosition);
            if (error) {
                if (!state.getValue(ERROR)) {
                    level.setBlock(worldPosition, state.setValue(ERROR, true), Block.UPDATE_ALL);
                }
            } else {
                if (state.getValue(ERROR)) {
                    level.setBlock(worldPosition, state.setValue(ERROR, false), Block.UPDATE_ALL);
                }
            }
            markDirtyQuick();
        }
    }

    public boolean inError() {
        return error;
    }

    @Nullable
    public NetworkId findRoutingNetwork() {
        WorldBlob worldBlob = XNetBlobData.get(level).getWorldBlob(level);
        return LogicTools.findRoutingConnector(level, getBlockPos(), worldBlob::getNetworkAt);
    }


    @Override
    public void saveAdditional(@Nonnull CompoundTag tagCompound) {
        tagCompound.putBoolean("error", error);
        super.saveAdditional(tagCompound);
    }

    @Override
    public void load(CompoundTag tagCompound) {
        super.load(tagCompound);
        error = tagCompound.getBoolean("error");
    }

    @Override
    public void saveInfo(CompoundTag tagCompound) {
        super.saveInfo(tagCompound);
        CompoundTag info = getOrCreateInfo(tagCompound);
        info.putBoolean("publicAcc", publicAccess);
    }

    @Override
    public void loadInfo(CompoundTag tagCompound) {
        super.loadInfo(tagCompound);
        CompoundTag info = tagCompound.getCompound(TAG_INFO);
        publicAccess = info.getBoolean("publicAcc");
    }

    @Override
    public void onReplaced(Level world, BlockPos pos, BlockState state, BlockState newstate) {
        if (state.getBlock() == newstate.getBlock()) {
            return;
        }
        if (!this.level.isClientSide) {
            XNetBlobData blobData = XNetBlobData.get(this.level);
            WorldBlob worldBlob = blobData.getWorldBlob(this.level);
            worldBlob.removeCableSegment(pos);
            blobData.save();
        }
    }

    @Override
    public void onBlockPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        if (!world.isClientSide) {
            XNetBlobData blobData = XNetBlobData.get(world);
            WorldBlob worldBlob = blobData.getWorldBlob(world);
            NetworkId networkId = worldBlob.newNetwork();
            worldBlob.createNetworkProvider(pos, new ColorId(CableColor.ROUTING.ordinal() + 1), networkId);
            blobData.save();
        }
    }


    // @todo 1.14
    public BlockState getActualState(BlockState state) {
        return state.setValue(ERROR, inError());
    }
}
