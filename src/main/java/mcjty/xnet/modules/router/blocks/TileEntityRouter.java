package mcjty.xnet.modules.router.blocks;

import mcjty.lib.api.container.DefaultContainerProvider;
import mcjty.lib.blockcommands.Command;
import mcjty.lib.blockcommands.ListCommand;
import mcjty.lib.blockcommands.ServerCommand;
import mcjty.lib.blocks.BaseBlock;
import mcjty.lib.builder.BlockBuilder;
import mcjty.lib.container.GenericContainer;
import mcjty.lib.tileentity.Cap;
import mcjty.lib.tileentity.CapType;
import mcjty.lib.tileentity.GenericTileEntity;
import mcjty.lib.typed.Key;
import mcjty.lib.typed.Type;
import mcjty.lib.varia.BlockPosTools;
import mcjty.lib.varia.OrientationTools;
import mcjty.rftoolsbase.api.xnet.channels.IChannelType;
import mcjty.rftoolsbase.api.xnet.channels.IConnectorSettings;
import mcjty.rftoolsbase.api.xnet.keys.NetworkId;
import mcjty.rftoolsbase.api.xnet.keys.SidedConsumer;
import mcjty.rftoolsbase.tools.ManualHelper;
import mcjty.xnet.client.ControllerChannelClientInfo;
import mcjty.xnet.compat.XNetTOPDriver;
import mcjty.xnet.logic.LogicTools;
import mcjty.xnet.modules.cables.CableColor;
import mcjty.xnet.modules.controller.ChannelInfo;
import mcjty.xnet.modules.controller.blocks.TileEntityController;
import mcjty.xnet.modules.router.LocalChannelId;
import mcjty.xnet.modules.router.RouterModule;
import mcjty.xnet.multiblock.ColorId;
import mcjty.xnet.multiblock.WorldBlob;
import mcjty.xnet.multiblock.XNetBlobData;
import mcjty.xnet.multiblock.XNetWirelessChannels;
import mcjty.xnet.setup.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import static mcjty.lib.api.container.DefaultContainerProvider.empty;
import static mcjty.lib.builder.TooltipBuilder.header;
import static mcjty.lib.builder.TooltipBuilder.key;
import static mcjty.xnet.apiimpl.Constants.TAG_CHANCNT;
import static mcjty.xnet.apiimpl.Constants.TAG_CHANNEL;
import static mcjty.xnet.apiimpl.Constants.TAG_INDEX;
import static mcjty.xnet.apiimpl.Constants.TAG_INFO;
import static mcjty.xnet.apiimpl.Constants.TAG_NAME;
import static mcjty.xnet.apiimpl.Constants.TAG_POS;
import static mcjty.xnet.apiimpl.Constants.TAG_PUBLISHED;
import static mcjty.xnet.modules.controller.ChannelInfo.MAX_CHANNELS;
import static mcjty.xnet.modules.controller.blocks.TileEntityController.ERROR;
import static mcjty.xnet.modules.router.RouterModule.TYPE_ROUTER;

public final class TileEntityRouter extends GenericTileEntity {

    private final Map<LocalChannelId, String> publishedChannels = new HashMap<>();
    private int channelCount = 0;

    public List<ControllerChannelClientInfo> clientLocalChannels = null;
    public List<ControllerChannelClientInfo> clientRemoteChannels = null;

    @Cap(type = CapType.CONTAINER)
    private final LazyOptional<MenuProvider> screenHandler = LazyOptional.of(() -> new DefaultContainerProvider<GenericContainer>("Router")
            .containerSupplier(empty(RouterModule.CONTAINER_ROUTER, this)));

    public TileEntityRouter(BlockPos pos, BlockState state) {
        super(TYPE_ROUTER.get(), pos, state);
    }

    public static BaseBlock createBlock() {
        return new BaseBlock(new BlockBuilder()
                .topDriver(XNetTOPDriver.DRIVER)
                .tileEntitySupplier(TileEntityRouter::new)
                .manualEntry(ManualHelper.create("xnet:network/router"))
                .info(key("message.xnet.shiftmessage"))
                .infoShift(header())
        ) {
            @Override
            protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
                super.createBlockStateDefinition(builder);
                builder.add(ERROR);
            }
        };
    }

    public void addPublishedChannels(Set<String> channels) {
        channels.addAll(publishedChannels.values());
    }

    public int countPublishedChannelsOnNet() {
        Set<String> channels = new HashSet<>();
        NetworkId networkId = findRoutingNetwork();
        if (networkId != null) {
            LogicTools.forEachRouter(level, networkId, router -> router.addPublishedChannels(channels));
        }
        return channels.size();
    }

    public boolean inError() {
        return channelCount > Config.maxPublishedChannels.get();
    }

    public int getChannelCount() {
        return channelCount;
    }

    public void setChannelCount(int cnt) {
        if (channelCount == cnt) {
            return;
        }
        channelCount = cnt;
        BlockState state = level.getBlockState(worldPosition);
        if (inError()) {
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

    @Override
    public void saveInfo(CompoundTag tagCompound) {
        super.saveInfo(tagCompound);
        CompoundTag info = getOrCreateInfo(tagCompound);
        info.putInt(TAG_CHANCNT, channelCount);
        ListTag published = new ListTag();
        for (Map.Entry<LocalChannelId, String> entry : publishedChannels.entrySet()) {
            CompoundTag tc = new CompoundTag();
            BlockPosTools.write(tc, TAG_POS, entry.getKey().controllerPos());
            tc.putInt(TAG_INDEX, entry.getKey().index());
            tc.putString(TAG_NAME, entry.getValue());
            published.add(tc);
        }
        info.put(TAG_PUBLISHED, published);
    }

    @Override
    public void loadInfo(CompoundTag tagCompound) {
        super.loadInfo(tagCompound);
        CompoundTag info = tagCompound.getCompound(TAG_INFO);
        channelCount = info.getInt(TAG_CHANCNT);
        ListTag published = info.getList(TAG_PUBLISHED, Tag.TAG_COMPOUND);
        for (int i = 0; i < published.size(); i++) {
            CompoundTag tc = published.getCompound(i);
            LocalChannelId id = new LocalChannelId(BlockPosTools.read(tc, TAG_POS), tc.getInt(TAG_INDEX));
            String name = tc.getString(TAG_NAME);
            publishedChannels.put(id, name);
        }
    }

    public void forEachPublishedChannel(BiConsumer<String, IChannelType> consumer) {
        LogicTools.forEachConnector(level, worldPosition, connectorPos -> {
            TileEntityController controller = LogicTools.getControllerForConnector(level, connectorPos);
            if (controller != null) {
                for (int i = 0 ; i < MAX_CHANNELS ; i++) {
                    ChannelInfo channelInfo = controller.getChannels()[i];
                    if (channelInfo != null && !channelInfo.getChannelName().isEmpty()) {
                        LocalChannelId id = new LocalChannelId(controller.getBlockPos(), i);
                        String publishedName = publishedChannels.get(id);
                        if (publishedName != null && !publishedName.isEmpty()) {
                            consumer.accept(publishedName, channelInfo.getType());
                        }
                    }
                }
            }
        });
    }

    public void findLocalChannelInfo(List<ControllerChannelClientInfo> list, boolean onlyPublished, boolean remote) {
        LogicTools.forEachConnector(level, getBlockPos(), connectorPos -> {
            TileEntityController controller = LogicTools.getControllerForConnector(level, connectorPos);
            if (controller != null) {
                for (int i = 0; i < MAX_CHANNELS; i++) {
                    ChannelInfo channelInfo = controller.getChannels()[i];
                    if (channelInfo != null && !channelInfo.getChannelName().isEmpty()) {
                        LocalChannelId id = new LocalChannelId(controller.getBlockPos(), i);
                        String publishedName = publishedChannels.get(id);
                        if (publishedName == null) {
                            publishedName = "";
                        }
                        if ((!onlyPublished) || !publishedName.isEmpty()) {
                            ControllerChannelClientInfo ci = new ControllerChannelClientInfo(channelInfo.getChannelName(), publishedName, controller.getBlockPos(), channelInfo.getType(), remote, i);
                            if (list.stream().noneMatch(ii -> Objects.equals(ii.getPublishedName(), ci.getPublishedName())
                                    && Objects.equals(ii.getChannelName(), ci.getChannelName())
                                    && Objects.equals(ii.getChannelType(), ci.getChannelType())
                                    && Objects.equals(ii.getPos(), ci.getPos()))) {
                                list.add(ci);
                            }
                        }
                    }
                }
            }
        });
    }

    private void findRemoteChannelInfo(List<ControllerChannelClientInfo> list) {
        NetworkId networkId = findRoutingNetwork();
        if (networkId != null) {
            // For each consumer on this network:
            LogicTools.consumers(level, networkId)
                    .forEach(consumerPos -> {
                        // Find all routers connected to this network and add their published local channels
                        LogicTools.forEachRouter(level, consumerPos, router -> {
                            if (router != this) {
                                router.findLocalChannelInfo(list, true, false);
                            }
                        });
                        // Find all wireless routers connected to this network and add the public or private
                        // channels that can be reached by them
                        LogicTools.forEachWirelessRouter(level, consumerPos, router -> {
                            if (!router.inError()) {
                                router.findRemoteChannelInfo(list);
                            }
                        });
                    });
        }
    }

    @Nullable
    public NetworkId findRoutingNetwork() {
        WorldBlob worldBlob = XNetBlobData.get(level).getWorldBlob(level);
        return LogicTools.findRoutingConnector(level, getBlockPos(), worldBlob::getNetworkAt);
    }

    public void addRoutedConnectors(Map<SidedConsumer, IConnectorSettings> connectors, @Nonnull BlockPos controllerPos, int channel, IChannelType type) {
        if (inError()) {
            // We are in error. Don't do anything
            return;
        }
        LocalChannelId id = new LocalChannelId(controllerPos, channel);
        String publishedName = publishedChannels.get(id);
        if (publishedName != null && !publishedName.isEmpty()) {
            NetworkId networkId = findRoutingNetwork();
            if (networkId != null) {
                LogicTools.consumers(level, networkId)
                        .forEach(consumerPos -> {
                            LogicTools.forEachRouter(level, consumerPos, router -> router.addConnectorsFromConnectedNetworks(connectors, publishedName, type));
                            LogicTools.forEachWirelessRouter(level, consumerPos, router -> {
                                if (!router.inError()) {
                                    // First public
                                    router.addWirelessConnectors(connectors, publishedName, type, null);
                                    // Now private
                                    router.addWirelessConnectors(connectors, publishedName, type, getOwnerUUID());
                                }
                            });
                        });
            } else {
                // If there is no routing network that means we have a local network only
                addConnectorsFromConnectedNetworks(connectors, publishedName, type);
            }
        }
    }

    public void addConnectorsFromConnectedNetworks(Map<SidedConsumer, IConnectorSettings> connectors, String channelName, IChannelType type) {
        LogicTools.forEachConnector(level, getBlockPos(), connectorPos -> {
            TileEntityController controller = LogicTools.getControllerForConnector(level, connectorPos);
            if (controller != null) {
                for (int i = 0; i < MAX_CHANNELS; i++) {
                    ChannelInfo info = controller.getChannels()[i];
                    if (info != null && info.isEnabled()) {
                        String publishedName = publishedChannels.get(new LocalChannelId(controller.getBlockPos(), i));
                        if (publishedName != null && !publishedName.isEmpty()) {
                            if (channelName.equals(publishedName) && type.equals(info.getType())) {
                                connectors.putAll(controller.getConnectors(i));
                            }
                        }
                    }
                }
            }
        });
    }

    private void updatePublishName(@Nonnull BlockPos controllerPos, int channel, String name) {
        LocalChannelId id = new LocalChannelId(controllerPos, channel);
        if (name == null || name.isEmpty()) {
            publishedChannels.remove(id);
        } else {
            publishedChannels.put(id, name);
        }
        int number = countPublishedChannelsOnNet();
        WorldBlob worldBlob = XNetBlobData.get(level).getWorldBlob(level);
        NetworkId networkId = findRoutingNetwork();
        if (networkId != null) {
            if (number != channelCount) {
                LogicTools.forEachRouter(level, networkId, router -> router.setChannelCount(number));
            }
            worldBlob.markNetworkDirty(networkId); // Force a recalc of affected networks
        }
        for (NetworkId net : worldBlob.getNetworksAt(worldPosition)) {
            worldBlob.markNetworkDirty(net);
        }
        for (Direction facing : OrientationTools.DIRECTION_VALUES) {
            for (NetworkId net : worldBlob.getNetworksAt(worldPosition.relative(facing))) {
                worldBlob.markNetworkDirty(net);
            }
        }
        XNetWirelessChannels.get(level).updateGlobalChannelVersion();

        markDirtyQuick();
    }

    public static final Key<BlockPos> PARAM_POS = new Key<>(TAG_POS, Type.BLOCKPOS);
    public static final Key<Integer> PARAM_CHANNEL = new Key<>(TAG_CHANNEL, Type.INTEGER);
    public static final Key<String> PARAM_NAME = new Key<>(TAG_NAME, Type.STRING);
    @ServerCommand
    public static final Command<?> CMD_UPDATENAME = Command.<TileEntityRouter>create("router.updateName",
        (te, player, params) -> te.updatePublishName(params.get(PARAM_POS), params.get(PARAM_CHANNEL), params.get(PARAM_NAME)));

    @ServerCommand(type = ControllerChannelClientInfo.class, serializer = ControllerChannelClientInfo.Serializer.class)
    public static final ListCommand<?, ?> CMD_GETCHANNELS = ListCommand.<TileEntityRouter, ControllerChannelClientInfo>create("xnet.router.getChannelInfo",
            (te, player, params) -> {
                List<ControllerChannelClientInfo> list = new ArrayList<>();
                te.findLocalChannelInfo(list, false, false);
                return list;
            },
            (te, player, params, list) -> te.clientLocalChannels = list);

    @ServerCommand(type = ControllerChannelClientInfo.class, serializer = ControllerChannelClientInfo.Serializer.class)
    public static final ListCommand<?, ?> CMD_GETREMOTECHANNELS = ListCommand.<TileEntityRouter, ControllerChannelClientInfo>create("xnet.router.getRemoteChannelInfo",
            (te, player, params) -> {
                List<ControllerChannelClientInfo> list = new ArrayList<>();
                te.findRemoteChannelInfo(list);
                return list;
            },
            (te, player, params, list) -> te.clientRemoteChannels = list);

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
}
