package mcjty.xnet.blocks.wireless;

import mcjty.lib.bindings.DefaultValue;
import mcjty.lib.bindings.IValue;
import mcjty.lib.tileentity.GenericEnergyReceiverTileEntity;
import mcjty.lib.typed.Key;
import mcjty.lib.typed.Type;
import mcjty.lib.varia.WorldTools;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ProbeMode;
import mcjty.theoneprobe.api.TextStyleClass;
import mcjty.xnet.api.channels.IChannelType;
import mcjty.xnet.api.channels.IConnectorSettings;
import mcjty.xnet.api.keys.NetworkId;
import mcjty.xnet.api.keys.SidedConsumer;
import mcjty.xnet.blocks.generic.CableColor;
import mcjty.xnet.blocks.router.TileEntityRouter;
import mcjty.xnet.clientinfo.ControllerChannelClientInfo;
import mcjty.xnet.config.GeneralConfiguration;
import mcjty.xnet.init.ModBlocks;
import mcjty.xnet.logic.LogicTools;
import mcjty.xnet.multiblock.*;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TileEntityWirelessRouter extends GenericEnergyReceiverTileEntity implements ITickable {

    public static final PropertyBool ERROR = PropertyBool.create("error");

    public static final int TIER_INVALID = -1;
    public static final int TIER_1 = 0;
    public static final int TIER_2 = 1;
    public static final int TIER_INF = 2;

    public static final Key<Boolean> VALUE_PUBLIC = new Key<>("public", Type.BOOLEAN);

    private boolean error = false;
    private int counter = 10;
    private boolean publicAccess = false;

    private int globalChannelVersion = -1;      // Used to detect if a wireless channel has been published and we might need to recheck

    public TileEntityWirelessRouter() {
        super(GeneralConfiguration.wirelessRouterMaxRF, GeneralConfiguration.wirelessRouterRfPerTick);
    }

    @Override
    public IValue<?>[] getValues() {
        return new IValue[]{
                new DefaultValue<>(VALUE_PUBLIC, this::isPublicAccess, this::setPublicAccess)
        };
    }

    public boolean isPublicAccess() {
        return publicAccess;
    }

    public void setPublicAccess(boolean publicAccess) {
        this.publicAccess = publicAccess;
        markDirtyClient();
    }

    @Override
    public void update() {
        if (!world.isRemote) {
            counter--;
            if (counter > 0) {
                return;
            }
            counter = 10;

            int version = XNetWirelessChannels.getWirelessChannels(world).getGlobalChannelVersion();
            if (globalChannelVersion != version) {
                globalChannelVersion = version;
                NetworkId networkId = findRoutingNetwork();
                if (networkId != null) {
                    WorldBlob worldBlob = XNetBlobData.getBlobData(world).getWorldBlob(world);
                    worldBlob.incNetworkVersion(networkId);
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
                    LogicTools.consumers(world, networkId)
                            .forEach(consumerPos -> LogicTools.routers(world, consumerPos)
                                    .forEach(r -> publishChannels(r, networkId)));
                }
            }

            setError(err);
        }
    }

    private int getAntennaTier() {
        if (world.getBlockState(pos.up()).getBlock() != ModBlocks.antennaBaseBlock) {
            return TIER_INVALID;
        }
        Block aboveAntenna = world.getBlockState(pos.up(2)).getBlock();
        if (aboveAntenna == ModBlocks.antennaDishBlock) {
            return TIER_INF;
        }
        if (aboveAntenna != ModBlocks.antennaBlock) {
            return TIER_INVALID;
        }
        if (world.getBlockState(pos.up(3)).getBlock() == ModBlocks.antennaBlock) {
            return TIER_2;
        } else {
            return TIER_1;
        }
    }

    private int getAntennaRange() {
        int tier = getAntennaTier();
        switch (tier) {
            case TIER_INVALID:
                return -1;
            case TIER_1:
                return GeneralConfiguration.antennaTier1Range;
            case TIER_2:
                return GeneralConfiguration.antennaTier2Range;
            case TIER_INF:
                return Integer.MAX_VALUE;
        }
        return -1;
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
        if (world.provider.getDimension() != otherRouter.world.provider.getDimension()) {
            int tier = getAntennaTier();
            int tierOther = otherRouter.getAntennaTier();
            
            if(tier == tierOther && tier == TIER_INF) {
                return true;
            } else {
                return false;
            }
        }

        double maxSqdist = Math.min(thisRange, otherRange);
        maxSqdist *= maxSqdist;
        double sqdist = pos.distanceSq(otherRouter.pos);
        return sqdist <= maxSqdist;
    }

    private boolean inRange(XNetWirelessChannels.WirelessRouterInfo wirelessRouter) {
        World otherWorld = DimensionManager.getWorld(wirelessRouter.getCoordinate().getDimension());
        if (otherWorld == null) {
            return false;
        }
        return LogicTools.consumers(otherWorld, wirelessRouter.getNetworkId())
                .filter(consumerPos -> WorldTools.chunkLoaded(otherWorld, consumerPos))
                .anyMatch(consumerPos -> LogicTools.wirelessRouters(otherWorld, consumerPos)
                        .anyMatch(this::inRange));
    }

    public void findRemoteChannelInfo(List<ControllerChannelClientInfo> list) {
        NetworkId network = findRoutingNetwork();
        if (network == null) {
            return;
        }

        XNetWirelessChannels wirelessData = XNetWirelessChannels.getWirelessChannels(world);
        wirelessData.findChannels(getOwnerUUID())
                .forEach(channel -> {
                    // Find all wireless routers on a given channel but remove the ones on our own channel
                    channel.getRouters().values().stream()
                            // Make sure this is not the same router
                            .filter(routerInfo -> isDifferentRouter(network, routerInfo))
                            .filter(this::inRange)
                            .forEach(routerInfo -> {
                                // Find all routers on this network
                                World otherWorld = DimensionManager.getWorld(routerInfo.getCoordinate().getDimension());
                                LogicTools.consumers(otherWorld, routerInfo.getNetworkId())
                                        .filter(otherWorld::isBlockLoaded)
                                        // Range check not needed here since the check is already done on the wireless router
                                        .forEach(consumerPos -> LogicTools.routers(otherWorld, consumerPos)
                                                .forEach(router -> {
                                                    router.findLocalChannelInfo(list, true, true);
                                                }));

                            });
                });
    }

    // Test if the given router is not a router on this network
    private boolean isDifferentRouter(NetworkId thisNetwork, XNetWirelessChannels.WirelessRouterInfo routerInfo) {
        return routerInfo.getCoordinate().getDimension() != world.provider.getDimension() || !thisNetwork.equals(routerInfo.getNetworkId());
    }

    private void publishChannels(TileEntityRouter router, NetworkId networkId) {
        int tier = getAntennaTier();
        UUID ownerUUID = publicAccess ? null : getOwnerUUID();
        XNetWirelessChannels wirelessData = XNetWirelessChannels.getWirelessChannels(world);
        router.publishedChannelStream()
                .forEach(pair -> {
                    String name = pair.getKey();
                    IChannelType channelType = pair.getValue();
                    long energyStored = getStoredPower();
                    if (GeneralConfiguration.wirelessRouterRfPerChannel[tier] <= energyStored) {
                        consumeEnergy(GeneralConfiguration.wirelessRouterRfPerChannel[tier]);
                        wirelessData.transmitChannel(name, channelType, ownerUUID, world.provider.getDimension(),
                                pos, networkId);
                    }
                });
    }

    public void addWirelessConnectors(Map<SidedConsumer, IConnectorSettings> connectors, String channelName, IChannelType type,
                                      @Nullable UUID owner, @Nonnull Map<WirelessChannelKey, Integer> wirelessVersions) {
        WirelessChannelKey key = new WirelessChannelKey(channelName, type, owner);
        XNetWirelessChannels.WirelessChannelInfo info = XNetWirelessChannels.getWirelessChannels(world).findChannel(key);
        if (info != null) {
            info.getRouters().keySet().stream()
                    // Don't do this for ourselves
                    .filter(routerPos -> routerPos.getDimension() != world.provider.getDimension() || !routerPos.getCoordinate().equals(pos))
                    .filter(routerPos -> WorldTools.chunkLoaded(DimensionManager.getWorld(routerPos.getDimension()), routerPos.getCoordinate()))
                    .forEach(routerPos -> {
                        WorldServer otherWorld = DimensionManager.getWorld(routerPos.getDimension());
                        TileEntity otherTE = otherWorld.getTileEntity(routerPos.getCoordinate());
                        if (otherTE instanceof TileEntityWirelessRouter) {
                            TileEntityWirelessRouter otherRouter = (TileEntityWirelessRouter) otherTE;
                            if (inRange(otherRouter) && !otherRouter.inError()) {
                                NetworkId routingNetwork = otherRouter.findRoutingNetwork();
                                if (routingNetwork != null) {
                                    LogicTools.consumers(world, routingNetwork)
                                            .forEach(consumerPos -> LogicTools.routers(otherWorld, consumerPos).
                                                    forEach(router -> {
                                                        if (router.addConnectorsFromConnectedNetworks(connectors, channelName, type)) {
                                                            wirelessVersions.put(key, info.getVersion());
                                                        }
                                                    }));
                                }
                            }
                        }

                    });
        }
    }


    private void setError(boolean err) {
        if (error != err) {
            error = err;
            markDirtyClient();
        }
    }

    public boolean inError() {
        return error;
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        boolean oldError = inError();

        super.onDataPacket(net, packet);

        if (world.isRemote) {
            // If needed send a render update.
            if (oldError != inError()) {
                world.markBlockRangeForRenderUpdate(getPos(), getPos());
            }
        }
    }

    @Nullable
    public NetworkId findRoutingNetwork() {
        WorldBlob worldBlob = XNetBlobData.getBlobData(world).getWorldBlob(world);
        return LogicTools.routingConnectors(world, getPos())
                .findFirst()
                .map(worldBlob::getNetworkAt)
                .orElse(null);
    }


    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
        tagCompound.setBoolean("error", error);
        return super.writeToNBT(tagCompound);
    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound) {
        super.readFromNBT(tagCompound);
        error = tagCompound.getBoolean("error");
    }

    @Override
    public void writeRestorableToNBT(NBTTagCompound tagCompound) {
        super.writeRestorableToNBT(tagCompound);
        tagCompound.setBoolean("publicAcc", publicAccess);
    }

    @Override
    public void readRestorableFromNBT(NBTTagCompound tagCompound) {
        super.readRestorableFromNBT(tagCompound);
        publicAccess = tagCompound.getBoolean("publicAcc");
    }

    @Override
    @Optional.Method(modid = "theoneprobe")
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world, IBlockState blockState, IProbeHitData data) {
        super.addProbeInfo(mode, probeInfo, player, world, blockState, data);
        XNetBlobData blobData = XNetBlobData.getBlobData(world);
        WorldBlob worldBlob = blobData.getWorldBlob(world);
        Set<NetworkId> networks = worldBlob.getNetworksAt(data.getPos());
        for (NetworkId networkId : networks) {
            probeInfo.text(TextStyleClass.LABEL + "Network: " + TextStyleClass.INFO + networkId.getId());
            if (mode != ProbeMode.EXTENDED) {
                break;
            }
        }
        if (inError()) {
            probeInfo.text(TextStyleClass.ERROR + "Missing antenna!");
        } else {
//            probeInfo.text(TextStyleClass.LABEL + "Channels: " + TextStyleClass.INFO + getChannelCount());
        }

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
    }


    @Override
    public void onBlockBreak(World workd, BlockPos pos, IBlockState state) {
        super.onBlockBreak(workd, pos, state);
        if (!world.isRemote) {
            XNetBlobData blobData = XNetBlobData.getBlobData(world);
            WorldBlob worldBlob = blobData.getWorldBlob(world);
            worldBlob.removeCableSegment(pos);
            blobData.save();
        }
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        if (!world.isRemote) {
            XNetBlobData blobData = XNetBlobData.getBlobData(world);
            WorldBlob worldBlob = blobData.getWorldBlob(world);
            NetworkId networkId = worldBlob.newNetwork();
            worldBlob.createNetworkProvider(pos, new ColorId(CableColor.ROUTING.ordinal() + 1), networkId);
            blobData.save();
        }
    }


    @Override
    public IBlockState getActualState(IBlockState state) {
        return state.withProperty(ERROR, inError());
    }

}
