package mcjty.xnet.apiimpl.items;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import mcjty.lib.varia.LevelTools;
import mcjty.rftoolsbase.api.xnet.channels.IChannelSettings;
import mcjty.rftoolsbase.api.xnet.channels.IConnectorSettings;
import mcjty.rftoolsbase.api.xnet.channels.IControllerContext;
import mcjty.rftoolsbase.api.xnet.gui.IEditorGui;
import mcjty.rftoolsbase.api.xnet.gui.IndicatorIcon;
import mcjty.rftoolsbase.api.xnet.helper.DefaultChannelSettings;
import mcjty.rftoolsbase.api.xnet.keys.ConsumerId;
import mcjty.rftoolsbase.api.xnet.keys.SidedConsumer;
import mcjty.xnet.XNet;
import mcjty.xnet.apiimpl.EnumStringTranslators;
import mcjty.xnet.apiimpl.enums.ChannelMode;
import mcjty.xnet.apiimpl.enums.InsExtMode;
import mcjty.xnet.apiimpl.items.enums.StackMode;
import mcjty.xnet.compat.RFToolsSupport;
import mcjty.xnet.modules.controller.client.AbstractEditorPanel;
import mcjty.xnet.setup.Config;
import mcjty.xnet.utils.CastTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

import static mcjty.xnet.apiimpl.Constants.TAG_DELAY;
import static mcjty.xnet.apiimpl.Constants.TAG_EXTIDX;
import static mcjty.xnet.apiimpl.Constants.TAG_MODE;
import static mcjty.xnet.apiimpl.Constants.TAG_OFFSET;

public class ItemChannelSettings extends DefaultChannelSettings implements IChannelSettings {

    public static final ResourceLocation iconGuiElements = new ResourceLocation(XNet.MODID, "textures/gui/guielements.png");


    // Cache data
    private Map<SidedConsumer, ItemConnectorSettings> itemExtractors = null;
    private List<Pair<SidedConsumer, ItemConnectorSettings>> itemConsumers = null;

    private ChannelMode channelMode = ChannelMode.PRIORITY;
    private int delay = 0;
    private int roundRobinOffset = 0;
    private final Map<ConsumerId, Integer> extractIndices = new HashMap<>();

    public ChannelMode getChannelMode() {
        return channelMode;
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
        channelMode = ChannelMode.values()[tag.getByte(TAG_MODE)];
        delay = tag.getInt(TAG_DELAY);
        roundRobinOffset = tag.getInt(TAG_OFFSET);
        int[] cons = tag.getIntArray(TAG_EXTIDX);
        for (int idx = 0; idx < cons.length; idx += 2) {
            extractIndices.put(new ConsumerId(cons[idx]), cons[idx + 1]);
        }
    }

    @Override
    public void writeToNBT(CompoundTag tag) {
        tag.putByte(TAG_MODE, (byte) channelMode.ordinal());
        tag.putInt(TAG_DELAY, delay);
        tag.putInt(TAG_OFFSET, roundRobinOffset);

        if (!extractIndices.isEmpty()) {
            int[] cons = new int[extractIndices.size() * 2];
            int idx = 0;
            for (Map.Entry<ConsumerId, Integer> entry : extractIndices.entrySet()) {
                cons[idx++] = entry.getKey().id();
                cons[idx++] = entry.getValue();
            }
            tag.putIntArray(TAG_EXTIDX, cons);
        }
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
        for (Map.Entry<SidedConsumer, ItemConnectorSettings> entry : itemExtractors.entrySet()) {
            ItemConnectorSettings settings = entry.getValue();
            if (d % settings.getSpeed() != 0) {
                continue;
            }

            ConsumerId consumerId = entry.getKey().consumerId();
            BlockPos extractorPos = context.findConsumerPosition(consumerId);
            if (extractorPos != null) {
                Direction side = entry.getKey().side();
                BlockPos pos = extractorPos.relative(side);
                if (!LevelTools.isLoaded(world, pos)) {
                    continue;
                }

                if (checkRedstone(world, settings, extractorPos)) {
                    continue;
                }
                if (!context.matchColor(settings.getColorsMask())) {
                    continue;
                }

                BlockEntity te = world.getBlockEntity(pos);

                if (RFToolsSupport.isStorageScanner(te)) {
                    RFToolsSupport.tickStorageScanner(context, settings, te, this);
                } else {
                    getItemHandlerAt(te, settings.getFacing()).ifPresent(handler -> {
                        int idx = getStartExtractIndex(settings, consumerId, handler);
                        idx = tickItemHandler(context, settings, handler, idx);
                        if (handler.getSlots() > 0) {
                            rememberExtractIndex(consumerId, (idx + 1) % handler.getSlots());
                        }
                    });
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


    private int tickItemHandler(IControllerContext context, ItemConnectorSettings settings, IItemHandler handler, int startIdx) {
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
            ItemStack stack = fetchItem(handler, true, extractMatcher, settings.getStackMode(), settings.getExtractAmount(), 64, index, startIdx);
            if (!stack.isEmpty()) {
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

                List<Pair<SidedConsumer, ItemConnectorSettings>> inserted = new ArrayList<>();
                int remaining = insertStackSimulate(inserted, context, stack);
                if (!inserted.isEmpty()) {
                    if (context.checkAndConsumeRF(Config.controllerOperationRFT.get())) {
                        insertStackReal(context, inserted, fetchItem(handler, false, extractMatcher, settings.getStackMode(), settings.getExtractAmount(), toextract - remaining, index, startIdx));
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

    // Returns what could not be inserted
    public int insertStackSimulate(@Nonnull List<Pair<SidedConsumer, ItemConnectorSettings>> inserted, @Nonnull IControllerContext context, @Nonnull ItemStack stack) {
        Level world = context.getControllerWorld();
        if (channelMode == ChannelMode.PRIORITY) {
            roundRobinOffset = 0;       // Always start at 0
        }
        int total = stack.getCount();
        for (int j = 0; j < itemConsumers.size(); j++) {
            int i = (j + roundRobinOffset) % itemConsumers.size();
            Pair<SidedConsumer, ItemConnectorSettings> entry = itemConsumers.get(i);
            ItemConnectorSettings settings = entry.getValue();

            if (settings.getMatcher(context).test(stack)) {
                BlockPos consumerPos = context.findConsumerPosition(entry.getKey().consumerId());
                if (consumerPos != null) {
                    if (!LevelTools.isLoaded(world, consumerPos)) {
                        continue;
                    }

                    if (checkRedstone(world, settings, consumerPos)) {
                        continue;
                    }
                    if (!context.matchColor(settings.getColorsMask())) {
                        continue;
                    }

                    Direction side = entry.getKey().side();
                    BlockPos pos = consumerPos.relative(side);
                    BlockEntity te = world.getBlockEntity(pos);
                    int actuallyinserted;
                    int toinsert = total;
                    ItemStack remaining;
                    Integer count = settings.getCount();

                    if (RFToolsSupport.isStorageScanner(te)) {
                        if (count != null) {
                            int amount = RFToolsSupport.countItems(te, settings.getMatcher(context), count);
                            int caninsert = count - amount;
                            if (caninsert <= 0) {
                                continue;
                            }
                            toinsert = Math.min(toinsert, caninsert);
                            stack = stack.copy();
                            stack.setCount(Math.max(toinsert, 0));
                        }
                        remaining = RFToolsSupport.insertItem(te, stack, true);
                    } else {
                        LazyOptional<IItemHandler> itemHandler = getItemHandlerAt(te, settings.getFacing());
                        if (itemHandler.isPresent()) {
                            if (count != null) {
                                int amount = countItems(itemHandler, settings.getMatcher(context));
                                int caninsert = count - amount;
                                if (caninsert <= 0) {
                                    continue;
                                }
                                toinsert = Math.min(toinsert, caninsert);
                                stack = stack.copy();
                                stack.setCount(Math.max(toinsert, 0));
                            }
                            ItemStack finalStack = stack;
                            remaining = itemHandler.map(h -> ItemHandlerHelper.insertItem(h, finalStack, true)).orElse(ItemStack.EMPTY);
                        } else {
                            continue;
                        }
                    }

                    actuallyinserted = toinsert - remaining.getCount();
                    if (count == null) {
                        // If we are not using a count then we restore 'stack' here as that is what
                        // we actually have to keep inserting until it is empty. If we are using a count
                        // then we don't do this as we don't want to risk stack getting null (on 1.10.2)
                        // from the insertItem() and then not being able to set stacksize a few lines
                        // above this
                        stack = remaining;
                    }
                    if (actuallyinserted > 0) {
                        inserted.add(entry);
                        total -= actuallyinserted;
                        if (total <= 0) {
                            return 0;
                        }
                    }
                }
            }
        }
        return total;
    }

    public void insertStackReal(@Nonnull IControllerContext context, @Nonnull List<Pair<SidedConsumer, ItemConnectorSettings>> inserted, @Nonnull ItemStack stack) {
        int total = stack.getCount();
        for (Pair<SidedConsumer, ItemConnectorSettings> entry : inserted) {
            BlockPos consumerPosition = context.findConsumerPosition(entry.getKey().consumerId());
            Direction side = entry.getKey().side();
            ItemConnectorSettings settings = entry.getValue();
            BlockPos pos = consumerPosition.relative(side);
            BlockEntity te = context.getControllerWorld().getBlockEntity(pos);
            if (RFToolsSupport.isStorageScanner(te)) {
                int toinsert = total;
                Integer count = settings.getCount();
                if (count != null) {
                    int amount = RFToolsSupport.countItems(te, settings.getMatcher(context), count);
                    int caninsert = count - amount;
                    if (caninsert <= 0) {
                        continue;
                    }
                    toinsert = Math.min(toinsert, caninsert);
                    stack = stack.copy();
                    stack.setCount(Math.max(toinsert, 0));
                }
                ItemStack remaining = RFToolsSupport.insertItem(te, stack, false);
                int actuallyinserted = toinsert - remaining.getCount();
                if (count == null) {
                    // If we are not using a count then we restore 'stack' here as that is what
                    // we actually have to keep inserting until it is empty. If we are using a count
                    // then we don't do this as we don't want to risk stack getting null (on 1.10.2)
                    // from the insertItem() and then not being able to set stacksize a few lines
                    // above this
                    stack = remaining;
                }

                if (actuallyinserted > 0) {
                    roundRobinOffset = (roundRobinOffset + 1) % itemConsumers.size();
                    total -= actuallyinserted;
                    if (total <= 0) {
                        return;
                    }
                }

            } else {
                LazyOptional<IItemHandler> handler = getItemHandlerAt(te, settings.getFacing());

                int toinsert = total;
                Integer count = settings.getCount();
                if (count != null) {
                    int amount = countItems(handler, settings.getMatcher(context));
                    int caninsert = count - amount;
                    if (caninsert <= 0) {
                        continue;
                    }
                    toinsert = Math.min(toinsert, caninsert);
                    stack = stack.copy();
                    stack.setCount(Math.max(toinsert, 0));
                }
                ItemStack finalStack = stack;
                ItemStack remaining = handler.map(h -> ItemHandlerHelper.insertItem(h, finalStack, false)).orElse(ItemStack.EMPTY);
                int actuallyinserted = toinsert - remaining.getCount();
                if (count == null) {
                    // If we are not using a count then we restore 'stack' here as that is what
                    // we actually have to keep inserting until it is empty. If we are using a count
                    // then we don't do this as we don't want to risk stack getting null (on 1.10.2)
                    // from the insertItem() and then not being able to set stacksize a few lines
                    // above this
                    stack = remaining;
                }

                if (actuallyinserted > 0) {
                    roundRobinOffset = (roundRobinOffset + 1) % itemConsumers.size();
                    total -= actuallyinserted;
                    if (total <= 0) {
                        return;
                    }
                }
            }
        }
    }

    private int countItems(LazyOptional<IItemHandler> handler, Predicate<ItemStack> matcher) {
        return handler.map(h -> countItems(h, matcher)).orElse(0);
    }

    private Integer countItems(IItemHandler h, Predicate<ItemStack> matcher) {
        int cnt = 0;
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack s = h.getStackInSlot(i);
            if (!s.isEmpty()) {
                if (matcher.test(s)) {
                    cnt += s.getCount();
                }
            }
        }
        return cnt;
    }


    private ItemStack fetchItem(IItemHandler handler, boolean simulate, Predicate<ItemStack> matcher, StackMode stackMode, int extractAmount, int maxamount, MInteger index, int startIdx) {
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
            itemExtractors = new HashMap<>();
            itemConsumers = new ArrayList<>();
            Map<SidedConsumer, IConnectorSettings> connectors = context.getConnectors(channel);
            for (Map.Entry<SidedConsumer, IConnectorSettings> entry : connectors.entrySet()) {
                ItemConnectorSettings con = (ItemConnectorSettings) entry.getValue();
                if (con.getItemMode() == InsExtMode.EXT) {
                    itemExtractors.put(entry.getKey(), con);
                } else {
                    itemConsumers.add(Pair.of(entry.getKey(), con));
                }
            }
            connectors = context.getRoutedConnectors(channel);
            for (Map.Entry<SidedConsumer, IConnectorSettings> entry : connectors.entrySet()) {
                ItemConnectorSettings con = (ItemConnectorSettings) entry.getValue();
                if (con.getItemMode() == InsExtMode.INS) {
                    itemConsumers.add(Pair.of(entry.getKey(), con));
                }
            }

            itemConsumers.sort((o1, o2) -> o2.getRight().getPriority().compareTo(o1.getRight().getPriority()));
        }
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
        ((AbstractEditorPanel)gui).translatableChoices(TAG_MODE, channelMode, ChannelMode.values());// TODO: 09.03.2024 remove AbstractEditorPanel cast after rftoolbase update
    }

    @Override
    public void update(Map<String, Object> data) {
        channelMode = CastTools.safeChannelMode(data.get(TAG_MODE));
        roundRobinOffset = 0;
    }

    @Nonnull
    public static LazyOptional<IItemHandler> getItemHandlerAt(@Nullable BlockEntity te, Direction intSide) {
        if (te != null) {
            return te.getCapability(ForgeCapabilities.ITEM_HANDLER, intSide);
        }
        return LazyOptional.empty();
    }


}
