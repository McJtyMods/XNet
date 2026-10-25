package mcjty.xnet.modules.controller.blocks;

import com.google.gson.*;
import mcjty.lib.api.container.DefaultContainerProvider;
import mcjty.lib.blockcommands.Command;
import mcjty.lib.blockcommands.ListCommand;
import mcjty.lib.blockcommands.ServerCommand;
import mcjty.lib.blocks.BaseBlock;
import mcjty.lib.builder.BlockBuilder;
import mcjty.lib.builder.TooltipBuilder;
import mcjty.lib.container.ContainerFactory;
import mcjty.lib.container.GenericContainer;
import mcjty.lib.container.GenericItemHandler;
import mcjty.lib.setup.Registration;
import mcjty.lib.tileentity.Cap;
import mcjty.lib.tileentity.CapType;
import mcjty.lib.tileentity.GenericEnergyStorage;
import mcjty.lib.tileentity.TickingTileEntity;
import mcjty.lib.typed.Key;
import mcjty.lib.typed.Type;
import mcjty.lib.typed.TypedMap;
import mcjty.lib.varia.BlockPosTools;
import mcjty.lib.varia.Cached;
import mcjty.lib.varia.OrientationTools;
import mcjty.lib.varia.Tools;
import mcjty.rftoolsbase.api.xnet.channels.*;
import mcjty.rftoolsbase.api.xnet.keys.ConsumerId;
import mcjty.rftoolsbase.api.xnet.keys.NetworkId;
import mcjty.rftoolsbase.api.xnet.keys.SidedConsumer;
import mcjty.rftoolsbase.api.xnet.keys.SidedPos;
import mcjty.rftoolsbase.modules.filter.items.FilterModuleItem;
import mcjty.rftoolsbase.tools.ManualHelper;
import mcjty.xnet.XNet;
import mcjty.xnet.client.ChannelClientInfo;
import mcjty.xnet.client.ConnectedBlockClientInfo;
import mcjty.xnet.client.ConnectorClientInfo;
import mcjty.xnet.client.ConnectorInfo;
import mcjty.xnet.compat.XNetTOPDriver;
import mcjty.xnet.logic.LogicTools;
import mcjty.xnet.modules.cables.CableModule;
import mcjty.xnet.modules.cables.blocks.ConnectorBlock;
import mcjty.xnet.modules.cables.blocks.ConnectorTileEntity;
import mcjty.xnet.modules.controller.ChannelInfo;
import mcjty.xnet.modules.controller.ConnectedBlockInfo;
import mcjty.xnet.modules.controller.ControllerModule;
import mcjty.xnet.modules.controller.KnownUnsidedBlocks;
import mcjty.xnet.modules.controller.client.GuiController;
import mcjty.xnet.modules.controller.data.ControllerData;
import mcjty.xnet.modules.controller.network.PacketControllerError;
import mcjty.xnet.modules.controller.network.PacketJsonToClipboard;
import mcjty.xnet.multiblock.*;
import mcjty.xnet.setup.Config;
import mcjty.xnet.setup.XNetMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import mcjty.lib.varia.Lazy;
import mcjty.lib.container.IItemHandler;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static mcjty.lib.api.container.DefaultContainerProvider.container;
import static mcjty.lib.container.SlotDefinition.specific;
import static mcjty.xnet.apiimpl.Constants.*;
import static mcjty.xnet.modules.controller.ChannelInfo.MAX_CHANNELS;
import static mcjty.xnet.modules.controller.ControllerModule.CONTROLLER;
import static mcjty.xnet.utils.I18nConstants.BLOCK_CONTROLLER;

public final class TileEntityController extends TickingTileEntity implements IControllerContext {

    public static final Key<Integer> PARAM_INDEX = new Key<>(TAG_INDEX, Type.INTEGER);

    public static final Key<String> PARAM_TYPE = new Key<>(TAG_TYPE, Type.STRING);
    public static final Key<String> PARAM_JSON = new Key<>("json", Type.STRING);
    public static final Key<Integer> PARAM_CHANNEL = new Key<>(TAG_CHANNEL, Type.INTEGER);
    public static final Key<Integer> PARAM_SIDE = new Key<>(TAG_SIDE, Type.INTEGER);
    public static final Key<BlockPos> PARAM_POS = new Key<>(TAG_POS, Type.BLOCKPOS);

    public static final BooleanProperty ERROR = BooleanProperty.create("error");

    public static final int SLOT_FILTER = 0;
    public static final int FILTER_SLOTS = 4;

    public static final String JSON_CONNECTOR = "cn";
    public static final String JSON_NAME = "n";
    public static final String JSON_ADVANCED = "ad";
    public static final String JSON_BLOCK = "b";
    public static final String JSON_CONNECTORS = "cns";

    // For client
    public List<ChannelClientInfo> clientChannels = null;
    public List<ConnectedBlockClientInfo> clientConnectedBlocks = null;

    private final Predicate<ItemStack> filterCaches[] = new Predicate[FILTER_SLOTS];

    public static final Lazy<ContainerFactory> CONTAINER_FACTORY = Lazy.of(() -> new ContainerFactory(FILTER_SLOTS)
            .box(specific(s -> s.getItem() instanceof FilterModuleItem), SLOT_FILTER, 17, 5, FILTER_SLOTS, 1)
            .playerSlots(91, 157));

    private NetworkId networkId;
    private WorldBlob worldBlob;
    private int wirelessVersion = -1;   // To invalidate wireless channels if needed

    // Cached/transient data
    private final Map<SidedConsumer, IConnectorSettings> cachedConnectors[] = new Map[MAX_CHANNELS];
    private final Map<SidedConsumer, IConnectorSettings> cachedRoutedConnectors[] = new Map[MAX_CHANNELS];

    private final GenericItemHandler items = GenericItemHandler.create(this, CONTAINER_FACTORY)
            .itemValid((slot, stack) -> stack.getItem() instanceof FilterModuleItem)
            .onUpdate((slot, stack) -> clearFilterCache())
            .build();
    @Cap(type = CapType.ITEMS_AUTOMATION)
    private static final Function<TileEntityController, IItemHandler> ITEM_CAP = tile -> tile.items;

    private final GenericEnergyStorage energyStorage = new GenericEnergyStorage(this, true, Config.controllerMaxRF.get(), Config.controllerRfPerTick.get());
    @Cap(type = CapType.ENERGY)
    private static final Function<TileEntityController, GenericEnergyStorage> ENERGY_CAP = tile -> tile.energyStorage;

    @Cap(type = CapType.CONTAINER)
    private static final Function<TileEntityController, MenuProvider> SCREEN_CAP = be -> new DefaultContainerProvider<GenericContainer>(BLOCK_CONTROLLER.i18n())
            .containerSupplier(container(ControllerModule.CONTAINER_CONTROLLER, CONTAINER_FACTORY, be))
            .itemHandler(() -> be.items)
            .energyHandler(() -> be.energyStorage)
            .setupSync(be);

    private final Cached<NetworkChecker> networkChecker = Cached.of(this::createNetworkChecker);

    public TileEntityController(BlockPos pos, BlockState state) {
        super(CONTROLLER.be().get(), pos, state);
//        Arrays.fill(channels, null);
    }

    private void clearFilterCache() {
        Arrays.fill(filterCaches, null);
    }

    @Nonnull
    @Override
    public Predicate<ItemStack> getIndexedFilter(int idx) {
        if (idx < 0 || idx >= FILTER_SLOTS) {
            return stack -> false;
        }
        if (filterCaches[idx] == null) {
            ItemStack stack = items.getStackInSlot(idx);
            if (stack.getItem() instanceof FilterModuleItem) {
                filterCaches[idx] = FilterModuleItem.getCache(stack);
            } else {
                filterCaches[idx] = s -> false;
            }
        }
        return filterCaches[idx];
    }

    public static BaseBlock createBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        return new BaseBlock(new BlockBuilder().properties(mcjty.rftoolsbase.tools.RegistryProperties.block(properties))
                .topDriver(XNetTOPDriver.DRIVER)
                .tileEntitySupplier(TileEntityController::new)
                .manualEntry(ManualHelper.create("rftoolsbase:simple/controller"))
                .info(TooltipBuilder.key("message.xnet.shiftmessage"))
                .infoShift(TooltipBuilder.header())
        ) {
            @Override
            protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
                super.createBlockStateDefinition(builder);
                builder.add(ERROR);
            }
        };
    }

    private NetworkChecker createNetworkChecker() {
        NetworkChecker checker = new NetworkChecker();
        checker.add(networkId);
        worldBlob = XNetBlobData.get(level).getWorldBlob(level);
        LogicTools.forEachRouter(level, networkId, router -> {
                    checker.add(worldBlob.getNetworksAt(router.getBlockPos()));
                    // We're only interested in one network. The other router networks are all same topology
                    NetworkId routerNetwork = worldBlob.getNetworkAt(router.getBlockPos());
                    if (routerNetwork != null) {
                        LogicTools.forEachRouter(level, routerNetwork, r -> {
                            if (r != router) {
                                LogicTools.forEachConnector(level, r.getBlockPos(), connectorPos -> {
                                    checker.add(worldBlob.getNetworkAt(connectorPos));
                                });
                            }
                        });
                    }
                });
        return checker;
    }

    @Override
    public Level getControllerWorld() {
        return level;
    }

    @Override
    public NetworkId getNetworkId() {
        return networkId;
    }

    public void setNetworkId(NetworkId networkId) {
        if (networkId == null && this.networkId == null) {
            return;
        }
        if (networkId != null && networkId.equals(this.networkId)) {
            return;
        }
        networkChecker.clear();
        this.networkId = networkId;
        markDirtyQuick();
    }

    public Cached<NetworkChecker> getNetworkChecker() {
        return networkChecker;
    }

    private void checkNetwork(WorldBlob worldBlob) {
        if (networkId != null && networkChecker.get().isDirtyAndMarkClean(worldBlob)) {
            cleanCaches();
            return;
        }

        // Check wireless
        XNetWirelessChannels channels = XNetWirelessChannels.get(level);
        if (wirelessVersion != channels.getGlobalChannelVersion()) {
            wirelessVersion = channels.getGlobalChannelVersion();
            setChanged();
            cleanCaches();
        }
    }

    private void cleanCaches() {
        ControllerData data = getData(ControllerModule.CONTROLLER_DATA);
        for (int i = 0; i < data.channels().size(); i++) {
            if (!data.channels().get(i).isEmpty()) {
                cleanCache(i);
            }
        }
    }

    @Override
    public boolean matchColor(int colorMask) {
        int colors = getData(ControllerModule.CONTROLLER_DATA).colors();
        return (colors & colorMask) == colorMask;
    }

    public int getColors() {
        return getData(ControllerModule.CONTROLLER_DATA).colors();
    }

    @Override
    public void tickServer() {
        if (worldBlob == null) {
            worldBlob = XNetBlobData.get(level).getWorldBlob(level);
        }

        BlockState state = level.getBlockState(worldPosition);
        if (worldBlob.getNetworksAt(getBlockPos()).size() > 1) {
            if (!state.getValue(ERROR)) {
                level.setBlock(worldPosition, state.setValue(ERROR, true), Block.UPDATE_ALL);
            }
            return;
        } else {
            if (state.getValue(ERROR)) {
                level.setBlock(worldPosition, state.setValue(ERROR, false), Block.UPDATE_ALL);
            }
        }

        checkNetwork(worldBlob);

        if (!checkAndConsumeRF(Config.controllerRFT.get())) {
            return;
        }

        boolean dirty = false;
        int newcolors = 0;
        ControllerData data = getData(ControllerModule.CONTROLLER_DATA);
        for (int i = 0; i < MAX_CHANNELS; i++) {
            if (data.channels().get(i).isEnabled()) {
                if (checkAndConsumeRF(Config.controllerChannelRFT.get())) {
                    data.channels().get(i).getChannelSettings().tick(i, this);
                }
                newcolors |= data.channels().get(i).getChannelSettings().getColors();
                dirty = true;
            }
        }
        if (newcolors != data.colors()) {
            data = data.withColors(newcolors);
            setData(ControllerModule.CONTROLLER_DATA, data);
        }
        if (dirty) {
            markDirtyQuick();
        }
    }

    @Override
    public boolean checkAndConsumeRF(int rft) {
        if (rft > 0) {
            if (energyStorage.getEnergy() < rft) {
                // Not enough energy
                return false;
            }
            energyStorage.consumeEnergy(rft);
            markDirtyQuick();
        }
        return true;
    }

    private void networkDirty() {
        if (networkId != null) {
            XNetBlobData.get(level).getWorldBlob(level).markNetworkDirty(networkId);
        }
    }

    private void cleanCache(int channel) {
        cachedConnectors[channel] = null;
        cachedRoutedConnectors[channel] = null;
        ControllerData data = getData(ControllerModule.CONTROLLER_DATA);
        data.channels().get(channel).getChannelSettings().cleanCache();
    }

    @Override
    @Nonnull
    public Map<SidedConsumer, IConnectorSettings> getConnectors(int channel) {
        if (cachedConnectors[channel] == null) {
            WorldBlob worldBlob = XNetBlobData.get(level).getWorldBlob(level);
            cachedConnectors[channel] = new HashMap<>();
            ControllerData data = getData(ControllerModule.CONTROLLER_DATA);
            for (Map.Entry<SidedConsumer, ConnectorInfo> entry : data.channels().get(channel).getConnectors().entrySet()) {
                SidedConsumer sidedConsumer = entry.getKey();
                BlockPos pos = findConsumerPosition(sidedConsumer.consumerId());
                if (pos != null && worldBlob.getNetworksAt(pos).contains(getNetworkId())) {
                    cachedConnectors[channel].put(sidedConsumer, entry.getValue().getConnectorSettings());
                }
            }
        }
        return cachedConnectors[channel];
    }

    @Override
    @Nonnull
    public Map<SidedConsumer, IConnectorSettings> getRoutedConnectors(int channel) {
        if (cachedRoutedConnectors[channel] == null) {
            cachedRoutedConnectors[channel] = new HashMap<>();

            ControllerData data = getData(ControllerModule.CONTROLLER_DATA);
            if (!data.channels().get(channel).getChannelName().isEmpty()) {
                LogicTools.forEachRouter(level, networkId, router -> {
                            router.addRoutedConnectors(cachedRoutedConnectors[channel], getBlockPos(),
                                    channel, data.channels().get(channel).getType());
                        });
            }
        }
        return cachedRoutedConnectors[channel];
    }

    @Override
    protected void applyImplicitComponents(net.minecraft.core.component.DataComponentGetter input) {
        super.applyImplicitComponents(input);
        energyStorage.applyImplicitComponents(input.get(mcjty.lib.setup.Registration.ITEM_ENERGY.get()));
        items.applyImplicitComponents(input.get(mcjty.lib.setup.Registration.ITEM_INVENTORY.get()));
        var data = input.get(ControllerModule.ITEM_CONTROLLER_DATA.get());
        if (data != null) {
            setData(ControllerModule.CONTROLLER_DATA, data);
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        energyStorage.collectImplicitComponents(builder);
        items.collectImplicitComponents(builder);
        builder.set(ControllerModule.ITEM_CONTROLLER_DATA.get(), getData(ControllerModule.CONTROLLER_DATA));
    }

    @Override
    public void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tagCompound = new CompoundTag();
        HolderLookup.Provider provider = (level != null ? level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY);
        ControllerData data = getData(ControllerModule.CONTROLLER_DATA);
        for (int i = 0 ; i < data.channels().size() ; i++) {
            CompoundTag channelTag = new CompoundTag();
            channelTag.putInt("index", i);
            ChannelInfo channel = data.channels().get(i);
            channel.getChannelSettings().writeToNBT(channelTag);
            CompoundTag connectorsTag = new CompoundTag();
            for (Map.Entry<SidedConsumer, ConnectorInfo> entry : channel.getConnectors().entrySet()) {
                CompoundTag connectorTag = new CompoundTag();
                SidedConsumer sidedConsumer = entry.getKey();
                ConnectorInfo connectorInfo = entry.getValue();
                connectorInfo.getConnectorSettings().writeToNBT(connectorTag);
                connectorsTag.put(sidedConsumer.consumerId().toString() + ":" + sidedConsumer.side().getName(), connectorTag);
            }
            channelTag.put("connectors", connectorsTag);
        }
        if (networkId != null) {
            tagCompound.putInt("networkId", networkId.id());
        }
        energyStorage.save(tagCompound, "energy", provider);
        items.save(tagCompound, "items", provider);

        output.store(com.mojang.serialization.MapCodec.assumeMapUnsafe(CompoundTag.CODEC), tagCompound);
    }

    @Override
    public void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        CompoundTag tagCompound = input.read(com.mojang.serialization.MapCodec.assumeMapUnsafe(CompoundTag.CODEC)).orElseGet(CompoundTag::new);
        HolderLookup.Provider provider = input.lookup();
        ControllerData data = getData(ControllerModule.CONTROLLER_DATA);
        for (int i = 0 ; i < data.channels().size() ; i++) {
            ChannelInfo channel = data.channels().get(i);
            CompoundTag channelTag = tagCompound.getCompoundOrEmpty("channel" + i);
            channel.getChannelSettings().readFromNBT(channelTag);
            CompoundTag connectorsTag = channelTag.getCompoundOrEmpty("connectors");
            for (Map.Entry<SidedConsumer, ConnectorInfo> entry : channel.getConnectors().entrySet()) {
                SidedConsumer sidedConsumer = entry.getKey();
                CompoundTag connectorTag = connectorsTag.getCompoundOrEmpty(sidedConsumer.consumerId().toString() + ":" + sidedConsumer.side().getName());
                if (!connectorTag.isEmpty()) {
                    ConnectorInfo connectorInfo = entry.getValue();
                    connectorInfo.getConnectorSettings().readFromNBT(connectorTag);
                }
            }
        }
        if (tagCompound.contains("networkId")) {
            networkId = new NetworkId(tagCompound.getIntOr("networkId", 0));
        } else {
            networkId = null;
        }
        energyStorage.load(tagCompound, "energy", provider);
        items.load(tagCompound, "items", provider);
    }

    @Nullable
    @Override
    public BlockPos findConsumerPosition(@Nonnull ConsumerId consumerId) {
        WorldBlob worldBlob = XNetBlobData.get(level).getWorldBlob(level);
        return findConsumerPosition(worldBlob, consumerId);
    }

    @Nullable
    private BlockPos findConsumerPosition(@Nonnull WorldBlob worldBlob, @Nonnull ConsumerId consumerId) {
        return worldBlob.getConsumerPosition(consumerId);
    }

    @Override
    public List<SidedPos> getConnectedBlockPositions() {
        WorldBlob worldBlob = XNetBlobData.get(level).getWorldBlob(level);

        List<SidedPos> result = new ArrayList<>();
        forEachConsumer(worldBlob, consumerPos -> {
            BlockEntity te = level.getBlockEntity(consumerPos);
            if (!(te instanceof ConnectorTileEntity)) {
                XNet.setup.getLogger().warn("What? The connector at " + BlockPosTools.toString(consumerPos) + " is not a connector?");
            }
            for (Direction facing : OrientationTools.DIRECTION_VALUES) {
                if (ConnectorBlock.isConnectable(level, consumerPos, facing)) {
                    BlockPos pos = consumerPos.relative(facing);
                    SidedPos sidedPos = new SidedPos(pos, facing.getOpposite());
                    result.add(sidedPos);
                }
            }
        });

        return result;
    }

    @Nonnull
    private List<ConnectedBlockClientInfo> findConnectedBlocksForClient() {
        WorldBlob worldBlob = XNetBlobData.get(level).getWorldBlob(level);

        Set<ConnectedBlockClientInfo> set = new HashSet<>();
        forEachConsumer(worldBlob, consumerPos -> {
            String name = "";
            BlockEntity te = level.getBlockEntity(consumerPos);
            if (te instanceof ConnectorTileEntity) {
                // Should always be the case. @todo error?
                name = ((ConnectorTileEntity) te).getConnectorName();
            } else {
                XNet.setup.getLogger().warn("What? The connector at " + BlockPosTools.toString(consumerPos) + " is not a connector?");
            }
            for (Direction facing : OrientationTools.DIRECTION_VALUES) {
                if (ConnectorBlock.isConnectable(level, consumerPos, facing)) {
                    BlockPos pos = consumerPos.relative(facing);
                    SidedPos sidedPos = new SidedPos(pos, facing.getOpposite());
                    BlockState state = level.getBlockState(pos);
                    ItemStack item = state.getCloneItemStack(level, pos, false);
                    ConnectedBlockClientInfo info = new ConnectedBlockClientInfo(sidedPos, item, name);
                    set.add(info);
                }
            }
        });
        List<ConnectedBlockClientInfo> list = new ArrayList<>(set);
        list.sort(Comparator.comparing(ConnectedBlockClientInfo::getBlockUnlocName)
                .thenComparing(ConnectedBlockClientInfo::getPos));
        return list;
    }

    private void forEachConsumer(WorldBlob worldBlob, Consumer<BlockPos> consumer) {
        for (IConsumerProvider provider : XNet.xNetApi.getConsumerProviders()) {
            for (BlockPos pos : provider.getConsumers(level, worldBlob, getNetworkId())) {
                consumer.accept(pos);
            }
        }
    }

    @Nonnull
    private List<ChannelClientInfo> findChannelInfo() {
        WorldBlob worldBlob = XNetBlobData.get(level).getWorldBlob(level);

        ControllerData data = getData(ControllerModule.CONTROLLER_DATA);
        List<ChannelClientInfo> chanList = new ArrayList<>();
        for (ChannelInfo channel : data.channels()) {
            if (!channel.isEmpty()) {
                ChannelClientInfo clientInfo = new ChannelClientInfo(channel.getChannelName(), channel.getType(),
                        channel.getChannelSettings(), channel.isEnabled());

                for (Map.Entry<SidedConsumer, ConnectorInfo> entry : channel.getConnectors().entrySet()) {
                    SidedConsumer sidedConsumer = entry.getKey();
                    ConnectorInfo info = entry.getValue();
                    if (info.getConnectorSettings() != null) {
                        BlockPos consumerPos = findConsumerPosition(worldBlob, sidedConsumer.consumerId());
                        if (consumerPos != null) {
                            SidedPos pos = new SidedPos(consumerPos.relative(sidedConsumer.side()), sidedConsumer.side().getOpposite());
                            boolean advanced = level.getBlockState(consumerPos).getBlock() == CableModule.ADVANCED_CONNECTOR.get();
                            ConnectorClientInfo ci = new ConnectorClientInfo(pos, sidedConsumer.consumerId(), channel.getType(), info.getConnectorSettings());
                            clientInfo.getConnectors().put(sidedConsumer, ci);
                        } else {
                            // Consumer was possibly removed. We might want to remove the entry from our list here?
                            // @todo
                        }
                    }
                }

                chanList.add(clientInfo);
            } else {
                chanList.add(null);
            }
        }
        return chanList;
    }

    private void updateChannel(int channel, TypedMap params) {
        Map<String, Object> map = new HashMap<>();
        for (Key<?> key : params.getKeys()) {
            map.put(key.name(), params.get(key));
        }
        ControllerData data = getData(ControllerModule.CONTROLLER_DATA);
        data.channels().get(channel).getChannelSettings().update(map);

        Boolean enabled = (Boolean) map.get(TAG_ENABLED);
        data.channels().get(channel).setEnabled(Boolean.TRUE.equals(enabled));

        String name = (String) map.get(TAG_NAME);
        data.channels().get(channel).setChannelName(name);

        XNetWirelessChannels channels = XNetWirelessChannels.get(level);
        channels.updateGlobalChannelVersion();
        markAsDirty();
    }

    public void markAsDirty() {
        networkDirty();
        markDirtyQuick();
    }

    private void removeChannel(int channel) {
        ControllerData data = getData(ControllerModule.CONTROLLER_DATA);
        data = data.setChannel(channel, ChannelInfo.EMPTY);
        setData(ControllerModule.CONTROLLER_DATA, data);
        cachedConnectors[channel] = null;
        cachedRoutedConnectors[channel] = null;
    }

    private void createChannel(int channel, String typeId) {
        IChannelType type = XNet.xNetApi.findType(typeId);
        ControllerData data = getData(ControllerModule.CONTROLLER_DATA);
        data = data.setChannel(channel, new ChannelInfo(type));
        setData(ControllerModule.CONTROLLER_DATA, data);
    }

    private void updateConnector(int channel, SidedPos pos, TypedMap params) {
        WorldBlob worldBlob = XNetBlobData.get(level).getWorldBlob(level);
        ConsumerId consumerId = worldBlob.getConsumerAt(pos.pos().relative(pos.side()));
        ControllerData data = getData(ControllerModule.CONTROLLER_DATA);
        for (Map.Entry<SidedConsumer, ConnectorInfo> entry : data.channels().get(channel).getConnectors().entrySet()) {
            SidedConsumer key = entry.getKey();
            if (key.consumerId().equals(consumerId) && key.side().getOpposite().equals(pos.side())) {
                Map<String, Object> map = new HashMap<>();
                for (Key<?> k : params.getKeys()) {
                    map.put(k.name(), params.get(k));
                }
                data.channels().get(channel).getConnectors().get(key).getConnectorSettings().update(map);
                markAsDirty();
                return;
            }
        }
    }

    private void removeConnector(int channel, SidedPos pos) {
        WorldBlob worldBlob = XNetBlobData.get(level).getWorldBlob(level);
        ConsumerId consumerId = worldBlob.getConsumerAt(pos.pos().relative(pos.side()));
        SidedConsumer toremove = null;
        ControllerData data = getData(ControllerModule.CONTROLLER_DATA);
        for (Map.Entry<SidedConsumer, ConnectorInfo> entry : data.channels().get(channel).getConnectors().entrySet()) {
            SidedConsumer key = entry.getKey();
            if (key.side().getOpposite().equals(pos.side())) {
                if (key.consumerId().equals(consumerId)) {
                    toremove = key;
                    break;
                }
            }
        }
        if (toremove != null) {
            data.channels().get(channel).getConnectors().remove(toremove);
            markAsDirty();
        }
    }

    private ConnectorInfo createConnector(int channel, SidedPos pos) {
        WorldBlob worldBlob = XNetBlobData.get(level).getWorldBlob(level);
        BlockPos consumerPos = pos.pos().relative(pos.side());
        ConsumerId consumerId = worldBlob.getConsumerAt(consumerPos);
        if (consumerId == null) {
            throw new RuntimeException("What?");
        }
        SidedConsumer id = new SidedConsumer(consumerId, pos.side().getOpposite());
        boolean advanced = level.getBlockState(consumerPos).getBlock() == CableModule.ADVANCED_CONNECTOR.get();
        ControllerData data = getData(ControllerModule.CONTROLLER_DATA);
        ConnectorInfo info = data.channels().get(channel).createConnector(id, advanced);
        markAsDirty();
        return info;
    }

    private IConnectorSettings findConnectorSettings(ChannelInfo channel, SidedPos p) {
        WorldBlob worldBlob = XNetBlobData.get(level).getWorldBlob(level);

        for (Map.Entry<SidedConsumer, ConnectorInfo> entry : channel.getConnectors().entrySet()) {
            SidedConsumer sidedConsumer = entry.getKey();
            ConnectorInfo info = entry.getValue();
            if (info.getConnectorSettings() != null) {
                BlockPos consumerPos = findConsumerPosition(worldBlob, sidedConsumer.consumerId());
                if (consumerPos != null) {
                    SidedPos pos = new SidedPos(consumerPos.relative(sidedConsumer.side()), sidedConsumer.side().getOpposite());
                    if (pos.equals(p)) {
                        return info.getConnectorSettings();
                    }
                }
            }
        }
        return null;
    }

    private void forEachConnectedBlock(Consumer<ConnectedBlockInfo> consumer) {
        WorldBlob worldBlob = XNetBlobData.get(level).getWorldBlob(level);

        forEachConsumer(worldBlob, consumerPos -> {
            String name = "";
            BlockEntity te = level.getBlockEntity(consumerPos);
            if (te instanceof ConnectorTileEntity) {
                // Should always be the case. @todo error?
                name = ((ConnectorTileEntity) te).getConnectorName();
            } else {
                XNet.setup.getLogger().warn("What? The connector at " + BlockPosTools.toString(consumerPos) + " is not a connector?");
            }
            for (Direction facing : OrientationTools.DIRECTION_VALUES) {
                if (ConnectorBlock.isConnectable(level, consumerPos, facing)) {
                    BlockPos pos = consumerPos.relative(facing);
                    SidedPos sidedPos = new SidedPos(pos, facing.getOpposite());
                    BlockState state = level.getBlockState(pos);
                    state = state.isAir() ? null : state;
                    ConnectedBlockInfo info = new ConnectedBlockInfo(sidedPos, state, name);
                    consumer.accept(info);
                }
            }
        });
    }

    private void copyConnector(Player player, int index, SidedPos sidedPos) {
        ControllerData data = getData(ControllerModule.CONTROLLER_DATA);
        ChannelInfo channel = data.channels().get(index);
        JsonObject parent = new JsonObject();
        IConnectorSettings connectorSettings = findConnectorSettings(channel, sidedPos);
        if (connectorSettings != null) {
            JsonObject object = connectorSettings.writeToJson();
            if (object != null) {
                parent.add(TAG_TYPE, new JsonPrimitive(channel.getType().getID()));
                parent.add(JSON_CONNECTOR, object);
                boolean advanced = ConnectorBlock.isAdvancedConnector(level, sidedPos.pos().relative(sidedPos.side()));
                parent.add(JSON_ADVANCED, new JsonPrimitive(advanced));

                Gson gson = new GsonBuilder().create();
                String json = gson.toJson(parent);

                XNetMessages.sendToPlayer(PacketJsonToClipboard.create(json), player);
                return;
            }
        }
        XNetMessages.sendToPlayer(PacketControllerError.create("Error copying connector!"), player);
    }

    private void copyChannel(Player player, int index) {
        ControllerData data = getData(ControllerModule.CONTROLLER_DATA);
        ChannelInfo channel = data.channels().get(index);
        IChannelSettings settings = channel.getChannelSettings();
        JsonObject parent = new JsonObject();
        JsonObject channelObject = settings.writeToJson();

        if (channelObject != null) {
            parent.add(TAG_TYPE, new JsonPrimitive(channel.getType().getID()));
            parent.add(JSON_NAME, new JsonPrimitive(channel.getChannelName()));
            parent.add(TAG_CHANNEL, channelObject);

            JsonArray connectors = new JsonArray();

            forEachConnectedBlock(connectedBlock -> {
                SidedPos sidedPos = connectedBlock.getPos();
                IConnectorSettings connectorSettings = findConnectorSettings(channel, sidedPos);
                if (connectorSettings != null) {
                    JsonObject object = connectorSettings.writeToJson();
                    if (object != null) {
                        JsonObject connectorObject = new JsonObject();
                        connectorObject.add(JSON_CONNECTOR, object);
                        connectorObject.add(JSON_NAME, new JsonPrimitive(connectedBlock.getName()));
                        boolean advanced = ConnectorBlock.isAdvancedConnector(level, sidedPos.pos().relative(sidedPos.side()));
                        connectorObject.add(JSON_ADVANCED, new JsonPrimitive(advanced));
                        if (!connectedBlock.isAir()) {
                            BlockState state = connectedBlock.getConnectedState();
                            connectorObject.add(JSON_BLOCK, new JsonPrimitive(Tools.getId(state).toString()));
                        }

                        connectors.add(connectorObject);
                    }
                }
            });

            parent.add(JSON_CONNECTORS, connectors);

            Gson gson = new GsonBuilder().create();
            String json = gson.toJson(parent);

            XNetMessages.sendToPlayer(PacketJsonToClipboard.create(json), player);
        } else {
            XNetMessages.sendToPlayer(PacketControllerError.create("Channel does not support this!"), player);
        }
    }

    private int calculateMatchingScore(IChannelType type, ConnectedBlockInfo info, String name, Identifier block,
                                       @Nonnull Direction side, @Nonnull Direction facingOverride, boolean advanced,
                                       boolean advancedNeeded) {
        int score = 0;

        String infoName = info.getName();
        if (!name.isEmpty() && Objects.equals(name, infoName)) {
            score += 100;
        }

        BlockPos blockPos = info.getPos().pos();
        Direction facing = info.getPos().side();

        // This block doesn't support this type. So bad score
        if (!type.supportsBlock(level, blockPos, facing)) {
            score -= 1000;
        }

        Identifier infoBlock = Tools.getId(info.getConnectedState());

        // If the side doesn't match we give a bad penalty
        if (!KnownUnsidedBlocks.isUnsided(infoBlock) && !facingOverride.equals(facing)) {
            score -= 1000;
        }

        boolean infoAdvanced = ConnectorBlock.isAdvancedConnector(level, blockPos.relative(facing));
        if (advanced) {
            if (infoAdvanced) {
                score += 50;
            } else {
                // If advanced is desired but our actual connector is not advanced then we give a penalty. The penalty is big
                // if we can't match with the actual side or if we actually need advanced
                if (advancedNeeded) {
                    score -= 1000;
                } else {
                    score -= 40;
                }
            }
        } else {
            // If we don't need advanced then we add a small penalty if it is advanced
            if (infoAdvanced) {
                score--;
            }
        }

        if (!info.isAir()) {
            if (Objects.equals(infoBlock, block)) {
                score += 200;
            }
        }

        if (facing.equals(side)) {
            score += 2;
        }

        return score;
    }

    private void pasteConnector(Player player, int channel, SidedPos sidedPos, String json) {
        try {
            JsonParser parser = new JsonParser();
            JsonObject root = parser.parse(json).getAsJsonObject();

            if (!root.has(JSON_CONNECTOR) || !root.has(TAG_TYPE)) {
                XNetMessages.sendToPlayer(PacketControllerError.create("Invalid connector json!"), player);
                return;
            }

            String typeId = root.get(TAG_TYPE).getAsString();
            IChannelType type = XNet.xNetApi.findType(typeId);
            ControllerData data = getData(ControllerModule.CONTROLLER_DATA);
            if (type != data.channels().get(channel).getType()) {
                XNetMessages.sendToPlayer(PacketControllerError.create("Wrong channel type!"), player);
                return;
            }
            boolean advanced = root.get(JSON_ADVANCED).getAsBoolean();
            JsonObject connectorObject = root.get(JSON_CONNECTOR).getAsJsonObject();
            boolean advancedNeeded = connectorObject.get(TAG_ADVANCED_NEEDED).getAsBoolean();

            BlockPos blockPos = sidedPos.pos();
            Direction facing = sidedPos.side();

            Direction side = Direction.byName(connectorObject.get(TAG_SIDE).getAsString());
            Direction facingOverride = connectorObject.has("facingoverride") ? Direction.byName(connectorObject.get("facingoverride").getAsString().toLowerCase()) : side;
            boolean infoAdvanced = ConnectorBlock.isAdvancedConnector(level, blockPos.relative(facing));
            if (advanced) {
                if (!infoAdvanced) {
                    // If advanced is desired but our actual connector is not advanced then we give a penalty. The penalty is big
                    // if we can't match with the actual side or if we actually need advanced
                    if (advancedNeeded || !facingOverride.equals(facing)) {
                        XNetMessages.sendToPlayer(PacketControllerError.create("Advanced connector is needed!"), player);
                        return;
                    }
                }
            }
            if (!infoAdvanced) {
                // Remove the facingoverride
                connectorObject.remove("facingoverride");
            }

            ConnectorInfo info = createConnector(channel, sidedPos);
            info.getConnectorSettings().readFromJson(connectorObject);
        } catch (JsonSyntaxException e) {
            XNetMessages.sendToPlayer(PacketControllerError.create("Error pasting clipboard data!"), player);
        }

        markAsDirty();

    }

    public List<ChannelInfo> getChannels() {
        return getData(ControllerModule.CONTROLLER_DATA).channels();
    }

    private static class PossibleConnection {
        private final JsonObject connector;
        private List<Pair<ConnectedBlockInfo, Integer>> sortedMatches;

        public PossibleConnection(JsonObject connector, List<Pair<ConnectedBlockInfo, Integer>> sortedMatches) {
            this.connector = connector;
            this.sortedMatches = sortedMatches;
        }
    }

    private void pasteChannel(Player player, int channel, String json) {
        try {
            JsonParser parser = new JsonParser();
            JsonObject root = parser.parse(json).getAsJsonObject();
            if (!root.has(TAG_CHANNEL) || !root.has(TAG_TYPE) || !root.has(JSON_NAME)) {
                XNetMessages.sendToPlayer(PacketControllerError.create("Invalid channel json!"), player);
                return;
            }
            String typeId = root.get(TAG_TYPE).getAsString();
            IChannelType type = XNet.xNetApi.findType(typeId);
            ControllerData data = getData(ControllerModule.CONTROLLER_DATA);
            ChannelInfo chan = new ChannelInfo(type);
            chan.setChannelName(root.get(JSON_NAME).getAsString());
            chan.getChannelSettings().readFromJson(root.get(JSON_NAME).getAsJsonObject());
            chan.setEnabled(false);
            data = data.setChannel(channel, chan);
            setData(ControllerModule.CONTROLLER_DATA, data);

            // We try to paste the best matches first. If there are any connectors in the clip that can't
            // be pasted we'll give a warning to the user
            boolean notEnoughConnectors = false;

            // First scan all connectors from the json clip and from that make a list of possible connections
            // on the in-world blocks
            JsonArray connectors = root.get(JSON_CONNECTORS).getAsJsonArray();
            List<PossibleConnection> connections = new ArrayList<>();
            for (JsonElement con : connectors) {
                JsonObject connector = con.getAsJsonObject();

                // Fetch some useful information from the connector as it is defined in the json. Basically we
                // need the connector name, wether or not the original connector (we copied from) was advanced
                // and also the name of the block we were connecting too
                String name = connector.get(JSON_NAME).getAsString();
                boolean advanced = connector.get(JSON_ADVANCED).getAsBoolean();
                Identifier block = connector.has(JSON_BLOCK) ? Identifier.parse(connector.get(JSON_BLOCK).getAsString()) : null;

                // Also get some useful settings from the connector data itself. Using these we can estimate a
                // matching score to see how well the destination connector matches with this one
                JsonObject connectorSettings = connector.get(JSON_CONNECTOR).getAsJsonObject();
                Direction side = Direction.byName(connectorSettings.get(TAG_SIDE).getAsString());
                Direction facingOverride = connectorSettings.has("facingoverride") ? Direction.byName(connectorSettings.get("facingoverride").getAsString()) : side;

                // 'advancedNeeded' is true if the connector settings are such that they only work in an advanced connector. This
                // is unrelated to the actual 'side' differing from the side that is set in the connector (only advanced connectors
                // can change that) as it is still possible that in the pasted setup the side happens to be at the right side and
                // then we don't need an advanced connector
                boolean advancedNeeded = connectorSettings.get(TAG_ADVANCED_NEEDED).getAsBoolean();

                // Find all blocks connected to this controller. We'll try to match and paste the json data
                // to these blocks in the best way possible

                // Given these desired settings from the json connector we calculate a sorted list of connection
                // candidates. If there are good candidates in this list we remember this sorted list and
                // add it to our list of possible connections
                List<Pair<ConnectedBlockInfo, Integer>> sortedMatches = new ArrayList<>();
                forEachConnectedBlock(info -> {
                    int score = calculateMatchingScore(type, info, name, block, side, facingOverride, advanced, advancedNeeded);
                    sortedMatches.add(Pair.of(info, score));
                });
                sortedMatches.sort((p1, p2) -> Integer.compare(p2.getRight(), p1.getRight()));
                if (!sortedMatches.isEmpty() && sortedMatches.get(0).getRight() > -50) {
                    connections.add(new PossibleConnection(connector, sortedMatches));
                } else {
                    notEnoughConnectors = true;
                }
            }

            // Now we go over all possible connections by always selecting the best one from the list. The best
            // possible connection is one that has a connection candidate with the highest score on its first spot
            // (remember that the list of connection candidates is itself sorted with the highest scored candidates
            // in front of the list)
            while (!connections.isEmpty()) {
                connections.sort((p1, p2) -> Integer.compare(p2.sortedMatches.get(0).getRight(), p1.sortedMatches.get(0).getRight()));

                // Get rid of the highest priority one at the front of the list
                // This is the best match we have at this moment
                PossibleConnection pair = connections.remove(0);
                JsonObject connector = pair.connector;
                if (pair.sortedMatches.isEmpty()) {
                    // This connector has no more valid candidates so that means all remaining connectors
                    // are bad as well. We just stop here
                    notEnoughConnectors = true;
                    break;
                }

                // 'info' refers to the real block on this local network
                ConnectedBlockInfo info = pair.sortedMatches.get(0).getKey();
                boolean infoAdvanced = ConnectorBlock.isAdvancedConnector(level, info.getPos().pos());

                JsonObject connectorSettings = connector.get(JSON_CONNECTOR).getAsJsonObject();
                if (!infoAdvanced) {
                    // Remove the facingoverride because this is not an advanced connector so it doesn't
                    // support different facings
                    connectorSettings.remove("facingoverride");
                }

                // Actually create the connector and paste the connector settings
                Identifier block = connector.has(JSON_BLOCK) ? Identifier.parse(connector.get(JSON_BLOCK).getAsString()) : null;
                System.out.println("Pasting " + info.getName() + " (" + block.toString() + " into " + Tools.getId(info.getConnectedState()) + ") with score = " + pair.sortedMatches.get(0).getRight());
                ConnectorInfo connectorInfo = createConnector(channel, info.getPos());
                connectorInfo.getConnectorSettings().readFromJson(connectorSettings);

                // Remove the connected block info we just used from all remaining connection proposals
                for (PossibleConnection connection : connections) {
                    List<Pair<ConnectedBlockInfo, Integer>> newMatches = new ArrayList<>();
                    for (Pair<ConnectedBlockInfo, Integer> match : connection.sortedMatches) {
                        if (match.getLeft() != info) {
                            newMatches.add(match);
                        }
                    }
                    connection.sortedMatches = newMatches;
                }
            }

            if (notEnoughConnectors) {
                XNetMessages.sendToPlayer(PacketControllerError.create("Not everything could be pasted!"), player);
            }
        } catch (JsonSyntaxException e) {
            XNetMessages.sendToPlayer(PacketControllerError.create("Error pasting clipboard data!"), player);
        }

        markAsDirty();
    }

    @ServerCommand
    public static final Command<?> CMD_CREATECONNECTOR = Command.<TileEntityController>create("controller.createConnector",
        (te, player, params) -> te.createConnector(params.get(PARAM_CHANNEL), new SidedPos(params.get(PARAM_POS), OrientationTools.DIRECTION_VALUES[params.get(PARAM_SIDE)])));
    @ServerCommand
    public static final Command<?> CMD_REMOVECONNECTOR = Command.<TileEntityController>create("controller.removeConnector",
        (te, player, params) -> te.removeConnector(params.get(PARAM_CHANNEL), new SidedPos(params.get(PARAM_POS), OrientationTools.DIRECTION_VALUES[params.get(PARAM_SIDE)])));
    @ServerCommand
    public static final Command<?> CMD_UPDATECONNECTOR = Command.<TileEntityController>create("controller.updateConnector",
        (te, player, params) -> te.updateConnector(params.get(PARAM_CHANNEL), new SidedPos(params.get(PARAM_POS), OrientationTools.DIRECTION_VALUES[params.get(PARAM_SIDE)]), params));
    @ServerCommand
    public static final Command<?> CMD_CREATECHANNEL = Command.<TileEntityController>create("controller.createChannel",
        (te, player, params) -> te.createChannel(params.get(PARAM_INDEX), params.get(PARAM_TYPE)));
    @ServerCommand
    public static final Command<?> CMD_PASTECHANNEL = Command.<TileEntityController>create("controller.pasteChannel",
        (te, player, params) -> te.pasteChannel(player, params.get(PARAM_INDEX), params.get(PARAM_JSON)));
    @ServerCommand
    public static final Command<?> CMD_COPYCHANNEL = Command.<TileEntityController>create("controller.copyChannel",
        (te, player, params) -> te.copyChannel(player, params.get(PARAM_INDEX)));
    @ServerCommand
    public static final Command<?> CMD_PASTECONNECTOR = Command.<TileEntityController>create("controller.pasteConnector",
        (te, player, params) -> te.pasteConnector(player, params.get(PARAM_INDEX), new SidedPos(params.get(PARAM_POS), OrientationTools.DIRECTION_VALUES[params.get(PARAM_SIDE)]), params.get(PARAM_JSON)));
    @ServerCommand
    public static final Command<?> CMD_COPYCONNECTOR = Command.<TileEntityController>create("controller.copyConnector",
        (te, player, params) -> te.copyConnector(player, params.get(PARAM_INDEX), new SidedPos(params.get(PARAM_POS), OrientationTools.DIRECTION_VALUES[params.get(PARAM_SIDE)])));
    @ServerCommand
    public static final Command<?> CMD_REMOVECHANNEL = Command.<TileEntityController>create("controller.removeChannel",
        (te, player, params) -> te.removeChannel(params.get(PARAM_INDEX)));
    @ServerCommand
    public static final Command<?> CMD_UPDATECHANNEL = Command.<TileEntityController>create("controller.updateChannel",
        (te, player, params) -> te.updateChannel(params.get(PARAM_CHANNEL), params));

    @ServerCommand(type = ChannelClientInfo.class, serializer = ChannelClientInfo.Serializer.class)
    public static final ListCommand<?, ?> CMD_GETCHANNELS = ListCommand.<TileEntityController, ChannelClientInfo>create("xnet.controller.getChannelInfo",
            (te, player, params) -> te.findChannelInfo(),
            (te, player, params, list) -> te.clientChannels = list);

    @ServerCommand(type = ConnectedBlockClientInfo.class, serializer = ConnectedBlockClientInfo.Serializer.class)
    public static final ListCommand<?, ?> CMD_GETCONNECTEDBLOCKS = ListCommand.<TileEntityController, ConnectedBlockClientInfo>create("xnet.controller.getConnectedBlocks",
            (te, player, params) -> te.findConnectedBlocksForClient(),
            (te, player, params, list) -> te.clientConnectedBlocks = list);

    @Override
    public void onBlockPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        findNeighbourConnector(world, pos);
    }

    @Override
    public void onReplaced(Level world, BlockPos pos, BlockState state, BlockState newstate) {
        if (state.getBlock() == newstate.getBlock()) {
            return;
        }
        XNetBlobData blobData = XNetBlobData.get(this.level);
        WorldBlob worldBlob = blobData.getWorldBlob(this.level);
        worldBlob.removeCableSegment(pos);
        blobData.save();
    }

    @Override
    public void checkRedstone(Level world, BlockPos pos) {
        // We abuse the redstone check for something else
        if (!world.isClientSide()) {
            findNeighbourConnector(world, pos);
        }
    }

    // Check neighbour blocks for a connector and inherit the color from that
    private void findNeighbourConnector(Level world, BlockPos pos) {
        if (world.isClientSide()) {
            return;
        }
        XNetBlobData blobData = XNetBlobData.get(world);
        WorldBlob worldBlob = blobData.getWorldBlob(world);
        ColorId oldColor = worldBlob.getColorAt(pos);
        ColorId newColor = null;
        for (Direction facing : OrientationTools.DIRECTION_VALUES) {
            if (world.getBlockState(pos.relative(facing)).getBlock() instanceof ConnectorBlock) {
                ColorId color = worldBlob.getColorAt(pos.relative(facing));
                if (color != null) {
                    if (color == oldColor) {
                        return; // Nothing to do
                    }
                    newColor = color;
                }
            }
        }
        if (newColor != null) {
            if (worldBlob.getBlobAt(pos) != null) {
                worldBlob.removeCableSegment(pos);
            }
            NetworkId networkId = worldBlob.newNetwork();
            worldBlob.createNetworkProvider(pos, newColor, networkId);
            blobData.save();

            BlockEntity te = world.getBlockEntity(pos);
            if (te instanceof TileEntityController) {
                ((TileEntityController) te).setNetworkId(networkId);
            }
        }
    }

}
