package mcjty.xnet.apiimpl.items;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mcjty.lib.varia.LevelTools;
import mcjty.rftoolsbase.api.xnet.channels.IChannelSettings;
import mcjty.rftoolsbase.api.xnet.channels.IChannelType;
import mcjty.rftoolsbase.api.xnet.channels.IConnectorSettings;
import mcjty.rftoolsbase.api.xnet.channels.IControllerContext;
import mcjty.rftoolsbase.api.xnet.gui.IEditorGui;
import mcjty.rftoolsbase.api.xnet.gui.IndicatorIcon;
import mcjty.rftoolsbase.api.xnet.helper.DefaultChannelSettings;
import mcjty.rftoolsbase.api.xnet.keys.ConsumerId;
import mcjty.rftoolsbase.api.xnet.keys.SidedConsumer;
import mcjty.xnet.XNet;
import mcjty.xnet.apiimpl.ConnectedEntity;
import mcjty.xnet.apiimpl.EnumStringTranslators;
import mcjty.xnet.apiimpl.enums.ChannelMode;
import mcjty.xnet.apiimpl.enums.InsExtMode;
import mcjty.xnet.apiimpl.items.enums.StackMode;
import mcjty.xnet.compat.RFToolsSupport;
import mcjty.xnet.modules.cables.blocks.ConnectorTileEntity;
import mcjty.xnet.setup.Config;
import mcjty.xnet.utils.CastTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import mcjty.lib.container.IItemHandler;
import mcjty.lib.container.ItemHandlerHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

import static mcjty.xnet.apiimpl.Constants.TAG_MODE;
import static mcjty.xnet.compat.RFToolsSupport.isStorageScanner;

public class ItemChannelSettings extends DefaultChannelSettings implements IChannelSettings {

    public static final Identifier iconGuiElements = Identifier.fromNamespaceAndPath(XNet.MODID, "textures/gui/guielements.png");

    // Cache data
    private List<ConnectedEntity<ItemConnectorSettings>> itemExtractors = null;
    private List<ConnectedEntity<ItemConnectorSettings>> itemConsumers = null;
    private boolean[] consumerFull; // Array of filled consumers in which you don't have to try to insert

    public List<Integer> getIndicesAsList() {
        List<Integer> list = new ArrayList<>();
        for (Map.Entry<ConsumerId, Integer> entry : extractIndices.entrySet()) {
            list.add(entry.getKey().id());
            list.add(entry.getValue());
        }
        return list;
    }

    public Map<Integer, Integer> getIndicesAsMap() {
        Map<Integer, Integer> map = new HashMap<>();
        for (Map.Entry<ConsumerId, Integer> entry : extractIndices.entrySet()) {
            map.put(entry.getKey().id(), entry.getValue());
        }
        return map;
    }

    public void setIndicesFromList(List<Integer> list) {
        extractIndices.clear();
        for (int i = 0; i < list.size(); i += 2) {
            extractIndices.put(new ConsumerId(list.get(i)), list.get(i + 1));
        }
    }

    private ChannelMode channelMode = ChannelMode.PRIORITY;
    private int delay = 0;
    private int roundRobinOffset = 0;
    public final Map<ConsumerId, Integer> extractIndices = new HashMap<>();

    public static final MapCodec<ItemChannelSettings> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ChannelMode.CODEC.fieldOf("mode").forGetter(ItemChannelSettings::getChannelMode),
            Codec.INT.listOf().fieldOf("extidx").forGetter(ItemChannelSettings::getIndicesAsList)
    ).apply(instance, ItemChannelSettings::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemChannelSettings> STREAM_CODEC = StreamCodec.composite(
            ChannelMode.STREAM_CODEC, ItemChannelSettings::getChannelMode,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.INT, ByteBufCodecs.INT), ItemChannelSettings::getIndicesAsMap,
            ItemChannelSettings::new);

    public ItemChannelSettings() {
    }

    public ItemChannelSettings(ChannelMode channelMode, List<Integer> itemExtractors) {
        this.channelMode = channelMode;
        setIndicesFromList(itemExtractors);
    }

    public ItemChannelSettings(ChannelMode channelMode, Map<Integer, Integer> itemExtractors) {
        this.channelMode = channelMode;
        itemExtractors.forEach((k, v) -> extractIndices.put(new ConsumerId(k), v));
    }

    public ChannelMode getChannelMode() {
        return channelMode;
    }

    @Override
    public IChannelType getType() {
        return XNet.setup.itemChannelType;
    }

    @Override
    public int getColors() {
        return 0;
    }

    @Override
    public JsonObject writeToJson() {
        JsonObject object = new JsonObject();
        object.add(TAG_MODE, new JsonPrimitive(channelMode.name()));
        return object;
    }

    @Override
    public void readFromJson(JsonObject data) {
        channelMode = EnumStringTranslators.getItemChannelMode(data.get(TAG_MODE).getAsString());
    }


    @Override
    public void readFromNBT(CompoundTag tag) {
        delay = tag.getIntOr("delay", 0);
        roundRobinOffset = tag.getIntOr("offset", 0);
    }

    @Override
    public void writeToNBT(CompoundTag tag) {
        tag.putInt("delay", delay);
        tag.putInt("offset", roundRobinOffset);
    }

    private int getExtractIndex(ConsumerId consumer) {
        return extractIndices.getOrDefault(consumer, 0);
    }

    private void rememberExtractIndex(ConsumerId consumer, int index) {
        extractIndices.put(consumer, index);
    }

    private static class MInteger {
        private int i;

        public MInteger(int i) {
            this.i = i;
        }

        // This can return an index out of bounds!
        public int get() {
            return i;
        }

        // Safe get that is always in bounds
        public int getSafe(int bounds) {
            return bounds <= 0 ? i : (i % bounds);
        }

        public void set(int i) {
            this.i = i;
        }

        public void inc() {
            i++;
        }
    }

    private static final Random random = new Random();

    @Override
    public void tick(int channel, IControllerContext context) {
        delay--;
        if (delay <= 0) {
            delay = 200 * 6;      // Multiply of the different speeds we have
        }
        if (delay % 5 != 0) {
            return;
        }
        int d = delay / 5;

        updateCache(channel, context);
        Level world = context.getControllerWorld();
        consumerFull = new boolean[itemConsumers.size()];
        for (int i = 0; i < itemExtractors.size(); i++) {
            ConnectedEntity<ItemConnectorSettings> extractor = itemExtractors.get(i);
            ItemConnectorSettings settings = extractor.settings();
            if (d % settings.getSpeed() != 0) {
                continue;
            }

            ConsumerId consumerId = extractor.sidedConsumer().consumerId();

            if (!LevelTools.isLoaded(world, extractor.getBlockPos())) {
                continue;
            }

            if (!checkRedstone(settings, extractor.getConnectorEntity(), context)) {
                continue;
            }

            if (isStorageScanner(extractor.getConnectedEntity())) {
                RFToolsSupport.tickStorageScanner(context, settings, extractor.getConnectedEntity(), this, world);
            } else {
                IItemHandler handler = getItemHandlerAt(extractor.getConnectedEntity(), extractor.settings().getFacing());
                if (handler == null) {
                    continue;
                }
                int idx = getStartExtractIndex(settings, consumerId, handler);
                idx = tickItemHandler(context, settings, handler, world, idx, i);
                if (handler.getSlots() > 0) {
                    rememberExtractIndex(consumerId, (idx + 1) % handler.getSlots());
                }
            }
        }
    }

    private int getStartExtractIndex(ItemConnectorSettings settings, ConsumerId consumerId, IItemHandler handler) {
        switch (settings.getExtractMode()) {
            case FIRST:
                return 0;
            case RND: {
                if (handler.getSlots() <= 0) {
                    return 0;
                }
                // Try 5 times to find a non empty slot
                for (int i = 0; i < 5; i++) {
                    int idx = random.nextInt(handler.getSlots());
                    if (!handler.getStackInSlot(idx).isEmpty()) {
                        return idx;
                    }
                }
                // Otherwise use a more complicated algorithm
                List<Integer> slots = new ArrayList<>();
                for (int i = 0; i < handler.getSlots(); i++) {
                    if (!handler.getStackInSlot(i).isEmpty()) {
                        slots.add(i);
                    }
                }
                if (slots.isEmpty()) {
                    return 0;
                }
                return slots.get(random.nextInt(slots.size()));
            }
            case ORDER:
                return getExtractIndex(consumerId);
        }
        return 0;
    }


    private int tickItemHandler(@Nonnull IControllerContext context, @Nonnull ItemConnectorSettings settings,
                                @Nonnull IItemHandler handler, @Nonnull Level world, int startIdx, int extractorIdx) {
        return NativeItems.transfer(() -> tickItemHandlerTransactional(context, settings, handler, world, startIdx, extractorIdx));
    }

    private int tickItemHandlerTransactional(IControllerContext context, ItemConnectorSettings settings,
            IItemHandler handler, Level world, int startIdx, int extractorIdx) {
        Predicate<ItemStack> extractMatcher = settings.getMatcher(context);

        Integer count = settings.getCount();
        int amount = 0;
        if (count != null) {
            amount = countItems(handler, extractMatcher);
            if (amount < count) {
                return startIdx;
            }
        }

        MInteger index = new MInteger(startIdx);
        while (true) {
            ItemStack stack = fetchItem(handler, true, extractMatcher, settings.getStackMode(),
                    settings.getExtractAmount(), 64, index, startIdx);
            if (stack.isEmpty()) {
                break;
            }

            // Now that we have a stack we first reduce the amount of the stack if we want to keep a certain
            // number of items
            int toextract = stack.getCount();
            if (count != null) {
                int canextract = amount - count;
                if (canextract <= 0) {
                    index.inc();
                    continue;
                }
                if (canextract < toextract) {
                    toextract = canextract;
                    stack = stack.copy();
                    stack.setCount(toextract);
                }
            }

            if (context.checkAndConsumeRF(Config.controllerOperationRFT.get())) {
                int remaining = insertStack(context, stack, world, extractorIdx);
                if (remaining != toextract) {
                    var extracted = fetchItem(handler, false, extractMatcher, settings.getStackMode(),
                            settings.getExtractAmount(), toextract - remaining, index, startIdx);
                    if (extracted.getCount() != toextract - remaining) {
                        throw new IllegalStateException("Item source changed during an XNet transaction");
                    }
                    break;
                } else {
                    index.inc();
                }

            } else {
                break;
            }
        }
        return index.getSafe(handler.getSlots());
    }

    public int insertStack(@Nonnull IControllerContext context, @Nonnull ItemStack source, @Nonnull Level world, int extractorIdx) {
        if (channelMode == ChannelMode.PRIORITY) {
            roundRobinOffset = 0;       // Always start at 0
        }
        int total = source.getCount();
        int consumersSize = itemConsumers.size();
        int extractorsSize = itemExtractors.size();
        for (int j = 0; j < consumersSize; j++) {
            int i = (j + roundRobinOffset) % consumersSize;
            if (consumerFull[i]) {
                continue;
            }
            ConnectedEntity<ItemConnectorSettings> consumer = itemConsumers.get(i);
            ItemConnectorSettings settings = consumer.settings();

            if (!LevelTools.isLoaded(world, consumer.getBlockPos())) {
                continue;
            }

            ItemStack remaining;

            IItemHandler destination = getItemHandlerAt(consumer.getConnectedEntity(), consumer.settings().getFacing());
            if (destination == null) {
                continue;
            }

            Predicate<ItemStack> matcher = settings.getMatcher(context);
            if (!matcher.test(source) || !checkRedstone(settings, consumer.getConnectorEntity(), context)) {
                continue;
            }
            BlockEntity te = consumer.getConnectedEntity();
            int toinsert = total;
            Integer count = settings.getCount();
            if (count != null) {
                int amount = isStorageScanner(te)
                                     ? RFToolsSupport.countItems(te, matcher, count)
                                     : countItems(destination, matcher);
                int caninsert = count - amount;
                if (caninsert <= 0) {
                    continue;
                }
                toinsert = Math.min(toinsert, caninsert);
                source = source.copy();
                source.setCount(toinsert);
            }

            remaining = isStorageScanner(te)
                                ? RFToolsSupport.insertItem(te, source, false)
                                : ItemHandlerHelper.insertItem(destination, source, false);

            int actuallyinserted = toinsert - remaining.getCount();
            if (count == null) {
                // If we are not using a count then we restore 'stack' here as that is what
                // we actually have to keep inserting until it is empty. If we are using a count
                // then we don't do this as we don't want to risk stack getting null (on 1.10.2)
                // from the insertItem() and then not being able to set stacksize a few lines
                // above this
                source = remaining;
            }
            if (actuallyinserted > 0) {
                roundRobinOffset = (roundRobinOffset + 1) % consumersSize;
                total -= actuallyinserted;
                if (total <= 0) {
                    return 0;
                }
            } else if (extractorsSize > 2 && (extractorIdx == 0 || extractorIdx == extractorsSize / 2 - 1)) {
                // If we have more than 1 extractor it would be useful to cache full consumer inventories
                // to avoid useless processing.
                // It will be enough to do it twice: at the beginning and in the middle of processing
                if (isFull(destination)) {
                    consumerFull[i] = true;// We will ignore this consumer for next extractor (if any)
                }
            }
        }
        return total;
    }

    private boolean isFull(IItemHandler itemHandler) {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stackInSlot = itemHandler.getStackInSlot(i);
            if (stackInSlot.getCount() < itemHandler.getSlotLimit(i)
                        && stackInSlot.getCount() < stackInSlot.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }

    private Integer countItems(IItemHandler h, Predicate<ItemStack> matcher) {
        int cnt = 0;
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack s = h.getStackInSlot(i);
            if (!s.isEmpty() && matcher.test(s)) {
                cnt += s.getCount();
            }
        }
        return cnt;
    }


    private ItemStack fetchItem(IItemHandler handler, boolean simulate, Predicate<ItemStack> matcher,
                                StackMode stackMode, int extractAmount, int maxamount, MInteger index, int startIdx) {
        if (handler.getSlots() <= 0) {
            return ItemStack.EMPTY;
        }
        for (int i = index.get(); i < handler.getSlots() + startIdx; i++) {
            int j = i % handler.getSlots();
            ItemStack stack = handler.getStackInSlot(j);
            if (!stack.isEmpty()) {
                int s = switch (stackMode) {
                    case SINGLE -> 1;
                    case STACK -> stack.getMaxStackSize();
                    case COUNT -> extractAmount;
                };
                s = Math.min(s, maxamount);
                stack = handler.extractItem(j, s, simulate);
                if (!stack.isEmpty() && matcher.test(stack)) {
                    index.set(i);
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }


    private void updateCache(int channel, IControllerContext context) {
        if (itemExtractors == null) {
            itemExtractors = new ArrayList<>();
            itemConsumers = new ArrayList<>();
            Level world = context.getControllerWorld();
            Map<SidedConsumer, IConnectorSettings> connectors = context.getConnectors(channel);
            for (Map.Entry<SidedConsumer, IConnectorSettings> entry : connectors.entrySet()) {
                ItemConnectorSettings con = (ItemConnectorSettings) entry.getValue();
                ConnectedEntity<ItemConnectorSettings> connectedEntity;
                connectedEntity = getConnectedEntityInfo(context, entry, world, con);
                if (connectedEntity == null) {
                    continue;
                }
                if (con.getItemMode() == InsExtMode.EXT) {
                    itemExtractors.add(connectedEntity);
                } else {
                    itemConsumers.add(connectedEntity);
                }

            }
            connectors = context.getRoutedConnectors(channel);
            for (Map.Entry<SidedConsumer, IConnectorSettings> entry : connectors.entrySet()) {
                ItemConnectorSettings con = (ItemConnectorSettings) entry.getValue();
                if (con.getItemMode() == InsExtMode.INS) {
                    ConnectedEntity<ItemConnectorSettings> connectedEntity;
                    connectedEntity = getConnectedEntityInfo(context, entry, world, con);
                    if (connectedEntity == null) {
                        continue;
                    }
                    itemConsumers.add(connectedEntity);
                }
            }

            itemConsumers.sort((o1, o2) -> o2.settings().getPriority().compareTo(o1.settings().getPriority()));
        }
    }

    @Nullable
    private ConnectedEntity<ItemConnectorSettings> getConnectedEntityInfo(
            IControllerContext context, Map.Entry<SidedConsumer, IConnectorSettings> entry, Level world, ItemConnectorSettings con
    ) {
        BlockPos connectorPos = context.findConsumerPosition(entry.getKey().consumerId());
        if (connectorPos == null) {
            return null;
        }
        ConnectorTileEntity connectorEntity = (ConnectorTileEntity) world.getBlockEntity(connectorPos);
        if (connectorEntity == null) {
            return null;
        }
        BlockPos connectedBlockPos = connectorPos.relative(entry.getKey().side());
        BlockEntity connectedEntity = world.getBlockEntity(connectedBlockPos);
        if (connectedEntity == null) {
            return null;
        }

        return new ConnectedEntity<>(entry.getKey(), con, connectorPos, connectedBlockPos, connectedEntity, connectorEntity);
    }

    @Override
    public void cleanCache() {
        itemExtractors = null;
        itemConsumers = null;
    }

    @Override
    public boolean isEnabled(String tag) {
        return true;
    }

    @Nullable
    @Override
    public IndicatorIcon getIndicatorIcon() {
        return new IndicatorIcon(iconGuiElements, 0, 80, 11, 10);
    }

    @Nullable
    @Override
    public String getIndicator() {
        return null;
    }

    @Override
    public void createGui(IEditorGui gui) {
        gui.nl();
        gui.translatableChoices(TAG_MODE, channelMode, ChannelMode.values());
    }

    @Override
    public void update(Map<String, Object> data) {
        channelMode = CastTools.safeChannelMode(data.get(TAG_MODE));
        roundRobinOffset = 0;
    }

    @Nullable
    public static IItemHandler getItemHandlerAt(@Nullable BlockEntity te, Direction intSide) {
        if (te != null) {
            var storage = net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(te.getLevel(), te.getBlockPos(), intSide);
            return storage == null ? null : new NativeItems(storage);
        }
        return null;
    }


}
