package mcjty.xnet.apiimpl.items;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mcjty.lib.varia.CompositeStreamCodec;
import mcjty.lib.varia.ItemStackList;
import mcjty.lib.varia.JSonTools;
import mcjty.rftoolsbase.api.xnet.channels.IChannelType;
import mcjty.rftoolsbase.api.xnet.channels.IControllerContext;
import mcjty.rftoolsbase.api.xnet.gui.IEditorGui;
import mcjty.rftoolsbase.api.xnet.gui.IndicatorIcon;
import mcjty.rftoolsbase.api.xnet.helper.AbstractConnectorSettings;
import mcjty.xnet.XNet;
import mcjty.xnet.apiimpl.Constants;
import mcjty.xnet.apiimpl.EnumStringTranslators;
import mcjty.xnet.apiimpl.enums.InsExtMode;
import mcjty.xnet.apiimpl.items.enums.ExtractMode;
import mcjty.xnet.apiimpl.items.enums.StackMode;
import mcjty.xnet.utils.CastTools;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static mcjty.xnet.apiimpl.Constants.*;
import static mcjty.xnet.utils.I18nConstants.*;

public class ItemConnectorSettings extends AbstractConnectorSettings {

    public static final Identifier iconGuiElements = Identifier.fromNamespaceAndPath(XNet.MODID, "textures/gui/guielements.png");

    public static final int FILTER_SIZE = 18;

    private InsExtMode itemMode = InsExtMode.INS;
    private ExtractMode extractMode = ExtractMode.FIRST;
    private int speed = 2;
    private StackMode stackMode = StackMode.SINGLE;
    private boolean tagsMode = false;
    private boolean metaMode = false;
    private boolean componentMode = false;
    private boolean blacklist = false;
    @Nullable private Integer priority = 0;
    @Nullable private Integer count = null;
    @Nullable private Integer extractAmount = null;

    private final ItemStackList filters = ItemStackList.create(FILTER_SIZE);
    private int filterIndex = -1;

    public static final MapCodec<ItemConnectorSettings> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BaseSettings.CODEC.fieldOf("base").forGetter(settings -> settings.settings),
            InsExtMode.CODEC.fieldOf("itemMode").forGetter(settings -> settings.itemMode),
            ExtractMode.CODEC.fieldOf("extractMode").forGetter(settings -> settings.extractMode),
            StackMode.CODEC.fieldOf("stackMode").forGetter(settings -> settings.stackMode),
            Codec.INT.fieldOf("speed").forGetter(settings -> settings.speed),
            Codec.INT.fieldOf("filterIndex").forGetter(settings -> settings.filterIndex),
            Codec.BOOL.fieldOf("tagsMode").forGetter(settings -> settings.tagsMode),
            Codec.BOOL.fieldOf("metaMode").forGetter(settings -> settings.metaMode),
            Codec.BOOL.fieldOf("componentMode").forGetter(settings -> settings.componentMode),
            Codec.BOOL.fieldOf("blacklist").forGetter(settings -> settings.blacklist),
            Codec.INT.optionalFieldOf("priority").forGetter(settings -> Optional.ofNullable(settings.priority)),
            Codec.INT.optionalFieldOf("extractAmount").forGetter(settings -> Optional.ofNullable(settings.extractAmount)),
            Codec.INT.optionalFieldOf("count").forGetter(settings -> Optional.ofNullable(settings.count)),
            ItemStack.OPTIONAL_CODEC.listOf().fieldOf("filters").forGetter(settings -> settings.filters)
    ).apply(instance, (base, itemMode, extractMode, stackMode, speed, filterIndex, tagsMode, metaMode, componentMode, blacklist, priority, extractAmount, count, filters) -> {
        ItemConnectorSettings settings = new ItemConnectorSettings(base, Direction.NORTH);
        settings.itemMode = itemMode;
        settings.extractMode = extractMode;
        settings.stackMode = stackMode;
        settings.speed = speed;
        settings.filterIndex = filterIndex;
        settings.tagsMode = tagsMode;
        settings.metaMode = metaMode;
        settings.componentMode = componentMode;
        settings.blacklist = blacklist;
        settings.priority = priority.orElse(null);
        settings.extractAmount = extractAmount.orElse(null);
        settings.count = count.orElse(null);
        for (int i = 0 ; i < FILTER_SIZE ; i++) {
            settings.filters.set(i, filters.get(i));
        }
        return settings;
    }));

    public static StreamCodec<RegistryFriendlyByteBuf, ItemConnectorSettings> STREAM_CODEC = CompositeStreamCodec.composite(
            BaseSettings.STREAM_CODEC, s -> s.settings,
            InsExtMode.STREAM_CODEC, s -> s.itemMode,
            ExtractMode.STREAM_CODEC, s -> s.extractMode,
            StackMode.STREAM_CODEC, s -> s.stackMode,
            ByteBufCodecs.INT, s -> s.speed,
            ByteBufCodecs.INT, s -> s.filterIndex,
            ByteBufCodecs.BOOL, s -> s.tagsMode,
            ByteBufCodecs.BOOL, s -> s.metaMode,
            ByteBufCodecs.BOOL, s -> s.componentMode,
            ByteBufCodecs.BOOL, s -> s.blacklist,
            ByteBufCodecs.optional(ByteBufCodecs.INT), s -> Optional.ofNullable(s.priority),
            ByteBufCodecs.optional(ByteBufCodecs.INT), s -> Optional.ofNullable(s.extractAmount),
            ByteBufCodecs.optional(ByteBufCodecs.INT), s -> Optional.ofNullable(s.count),
            ItemStack.OPTIONAL_LIST_STREAM_CODEC, s -> s.filters,
            (base, itemMode, extractMode, stackMode, speed, filterIndex, tagsMode, metaMode, componentMode, blacklist, priority, extractAmount, count, filters) -> {
                ItemConnectorSettings settings = new ItemConnectorSettings(base, Direction.NORTH);
                settings.itemMode = itemMode;
                settings.extractMode = extractMode;
                settings.stackMode = stackMode;
                settings.speed = speed;
                settings.filterIndex = filterIndex;
                settings.tagsMode = tagsMode;
                settings.metaMode = metaMode;
                settings.componentMode = componentMode;
                settings.blacklist = blacklist;
                settings.priority = priority.orElse(null);
                settings.extractAmount = extractAmount.orElse(null);
                settings.count = count.orElse(null);
                for (int i = 0 ; i < FILTER_SIZE ; i++) {
                    settings.filters.set(i, filters.get(i));
                }
                return settings;
            }
    );

    public ItemConnectorSettings(@Nonnull BaseSettings settings, @Nonnull Direction side) {
        super(settings, side);
    }

    // Cached matcher for items
    private Predicate<ItemStack> matcher = null;

    public InsExtMode getItemMode() {
        return itemMode;
    }

    @Override
    public IChannelType getType() {
        return XNet.setup.itemChannelType;
    }

    @Nullable
    @Override
    public IndicatorIcon getIndicatorIcon() {
        return switch (itemMode) {
            case INS -> new IndicatorIcon(iconGuiElements, 0, 70, 13, 10);
            case EXT -> new IndicatorIcon(iconGuiElements, 13, 70, 13, 10);
        };
    }

    @Override
    @Nullable
    public String getIndicator() {
        return null;
    }

    private String getMinMaxTooltip() {
        return ITEM_COUNT_TOOLTIP_FORMATTED.i18n(
                (itemMode == InsExtMode.EXT ? EXT_ENDING : INS_ENDING).i18n(),
                (itemMode == InsExtMode.EXT ? LOW_FORMAT : HIGH_FORMAT).i18n()
        );
    }

    @Override
    public void createGui(IEditorGui gui) {
        advanced = gui.isAdvanced();
        String[] speeds = advanced ? Constants.ADVANCED_SPEEDS : Constants.SPEEDS;

        sideGui(gui);
        colorsGui(gui);
        redstoneGui(gui);
        gui.nl();
        gui.translatableChoices(TAG_MODE, itemMode, InsExtMode.values()).shift(5);
        gui.translatableChoices(TAG_STACK, stackMode, StackMode.values());

        if (stackMode == StackMode.COUNT && itemMode == InsExtMode.EXT) {
            gui.integer(TAG_EXTRACT_AMOUNT, ITEM_EXT_COUNT_TOOLTIP.i18n(), extractAmount, 30, 64);
        }

        gui.shift(10).choices(TAG_SPEED, SPEED_TOOLTIP.i18n(), Integer.toString(speed * 5), speeds);
        gui.nl();
        gui
                .label(PRIORITY_LABEL.i18n()).integer(TAG_PRIORITY, PRIORITY_TOOLTIP.i18n(), priority, 36).shift(5)
                .label("#").integer(TAG_COUNT, getMinMaxTooltip(), count, 30);

        if (itemMode == InsExtMode.EXT) {
            gui.shift(5).translatableChoices(TAG_EXTRACT, extractMode, ExtractMode.values());
        }

        gui.nl();
        gui
                .toggleText(TAG_BLACKLIST, ITEM_BLACKLIST_TOOLTIP.i18n(), ITEM_BLACKLIST_LABEL.i18n(), blacklist).shift(0)
                .toggleText(TAG_TAGS, ITEM_TAGS_TOOLTIP.i18n(), ITEM_TAGS_LABEL.i18n(), tagsMode).shift(0)
                .toggleText(TAG_META, ITEM_META_TOOLTIP.i18n(), ITEM_META_LABEL.i18n(), metaMode).shift(0)
                .toggleText(TAG_NBT, ITEM_NBT_TOOLTIP.i18n(), ITEM_NBT_LABEL.i18n(), componentMode).shift(0)
                .choices(TAG_FILTER_IDX, ITEM_FILTER_INDEX_TOOLTIP.i18n(), getFilterIndexString(),
                        ITEM_FILTER_OFF.i18n(), "1", "2", "3", "4")
                .nl();
        for (int i = 0 ; i < FILTER_SIZE ; i++) {
            if (i > 0 && i % 9 == 0) {
                gui.nl();
            }
            gui.ghostSlot(TAG_FLT + i, filters.get(i));
        }
    }

    private String getFilterIndexString() {
        if (filterIndex == -1) {
            return ITEM_FILTER_OFF.i18n();
        } else {
            return Integer.toString(filterIndex);
        }
    }

    public Predicate<ItemStack> getMatcher(IControllerContext context) {
        if (matcher == null) {
            ItemStackList filterList = ItemStackList.create();
            for (ItemStack stack : filters) {
                if (!stack.isEmpty()) {
                    filterList.add(stack);
                }
            }
            Predicate<ItemStack> filterMatcher = getIndexFilterMatcher(context);
            if (filterList.isEmpty()) {
                if (filterMatcher != null) {
                    matcher = filterMatcher;
                } else {
                    matcher = itemStack -> true;
                }
            } else {
                ItemFilterCache filterCache = new ItemFilterCache(metaMode, tagsMode, blacklist, componentMode, filterList);
                if (filterMatcher != null) {
                    matcher = stack -> filterMatcher.test(stack) || filterCache.match(stack);
                } else {
                    matcher = filterCache::match;
                }
            }
        }
        return matcher;
    }

    @Nullable
    private Predicate<ItemStack> getIndexFilterMatcher(IControllerContext context) {
        if (filterIndex == -1) {
            return null;
        }
        return s -> context.getIndexedFilter(filterIndex-1).test(s);
    }

    public StackMode getStackMode() {
        return stackMode;
    }


    public ExtractMode getExtractMode() {
        return extractMode;
    }

    @Nonnull
    public Integer getPriority() {
        return priority == null ? 0 : priority;
    }

    @Nullable
    public Integer getCount() {
        return count;
    }

    public int getExtractAmount() {
        return extractAmount == null ? 1 : extractAmount;
    }

    public int getSpeed() {
        return speed;
    }

    public int getFilterIndex() {
        return filterIndex;
    }

    private static final Set<String> INSERT_TAGS = ImmutableSet.of(TAG_MODE, TAG_RS, TAG_COLOR+"0", TAG_COLOR+"1", TAG_COLOR+"2", TAG_COLOR+"3", TAG_COUNT, TAG_PRIORITY, TAG_TAGS, TAG_META, TAG_NBT, TAG_BLACKLIST);
    private static final Set<String> EXTRACT_TAGS = ImmutableSet.of(TAG_MODE, TAG_RS, TAG_COLOR+"0", TAG_COLOR+"1", TAG_COLOR+"2", TAG_COLOR+"3", TAG_COUNT, TAG_TAGS, TAG_META, TAG_NBT, TAG_BLACKLIST, TAG_STACK, TAG_SPEED, TAG_EXTRACT, TAG_EXTRACT_AMOUNT);

    @Override
    public boolean isEnabled(String tag) {
        if (tag.startsWith(TAG_FLT)) {
            return true;
        }
        if (tag.equals(TAG_FACING)) {
            return advanced;
        }
        if (itemMode == InsExtMode.INS) {
            return INSERT_TAGS.contains(tag);
        } else {
            return EXTRACT_TAGS.contains(tag);
        }
    }

    public void setSpeed(int speed) {
        this.speed = speed != 0 ? speed : 4;
    }

    @Override
    public void update(Map<String, Object> data) {
        super.update(data);
        itemMode = CastTools.safeInsExtMode(data.get(TAG_MODE));
        extractMode = CastTools.safeExtractMode(data.get(TAG_EXTRACT));
        stackMode = CastTools.safeStackMode(data.get(TAG_STACK));
        setSpeed(Integer.parseInt((String) data.get(TAG_SPEED)) / 5);

        String idx = (String) data.get(TAG_FILTER_IDX);
        filterIndex = CastTools.safeIntOrValue(idx, -1);
        tagsMode = Boolean.TRUE.equals(data.get(TAG_TAGS));
        metaMode = Boolean.TRUE.equals(data.get(TAG_META));
        componentMode = Boolean.TRUE.equals(data.get(TAG_NBT));

        blacklist = Boolean.TRUE.equals(data.get(TAG_BLACKLIST));
        priority = (Integer) data.get(TAG_PRIORITY);
        count = (Integer) data.get(TAG_COUNT);
        extractAmount = (Integer) data.get(TAG_EXTRACT_AMOUNT);
        for (int i = 0 ; i < FILTER_SIZE ; i++) {
            filters.set(i, (ItemStack) data.get(TAG_FLT+i));
        }
        matcher = null;
    }

    @Override
    public JsonObject writeToJson() {
        JsonObject object = new JsonObject();
        super.writeToJsonInternal(object);
        setEnumSafe(object, TAG_ITEM_MODE, itemMode);
        setEnumSafe(object, TAG_EXTRACT_MODE, extractMode);
        setEnumSafe(object, TAG_STACK_MODE, stackMode);
        object.add(TAG_TAGS_MODE, new JsonPrimitive(tagsMode));
        object.add(TAG_META_MODE, new JsonPrimitive(metaMode));
        object.add(TAG_NBT_MODE, new JsonPrimitive(componentMode));
        object.add(TAG_BLACKLIST, new JsonPrimitive(blacklist));
        setIntegerSafe(object, TAG_PRIORITY, priority);
        setIntegerSafe(object, TAG_EXTRACT_AMOUNT, extractAmount);
        setIntegerSafe(object, TAG_COUNT, count);
        setIntegerSafe(object, TAG_SPEED, speed);
        setIntegerSafe(object, TAG_FILTER_INDEX, filterIndex);
        for (int i = 0 ; i < FILTER_SIZE ; i++) {
            if (!filters.get(i).isEmpty()) {
                object.add(TAG_FLT + i, JSonTools.itemStackToJson(filters.get(i)));
            }
        }
        if (speed == 1) {
            object.add(TAG_ADVANCED_NEEDED, new JsonPrimitive(true));
        }

        return object;
    }

    @Override
    public void readFromJson(JsonObject object) {
        super.readFromJsonInternal(object);
        itemMode = getEnumSafe(object, TAG_ITEM_MODE, EnumStringTranslators::getItemMode);
        extractMode = getEnumSafe(object, TAG_EXTRACT_MODE, EnumStringTranslators::getExtractMode);
        stackMode = getEnumSafe(object, TAG_STACK_MODE, EnumStringTranslators::getStackMode);
        tagsMode = getBoolSafe(object, TAG_TAGS_MODE);
        metaMode = getBoolSafe(object, TAG_META_MODE);
        componentMode = getBoolSafe(object, TAG_NBT_MODE);
        blacklist = getBoolSafe(object, TAG_BLACKLIST);
        priority = getIntegerSafe(object, TAG_PRIORITY);
        extractAmount = getIntegerSafe(object, TAG_EXTRACT_AMOUNT);
        count = getIntegerSafe(object, TAG_COUNT);
        speed = getIntegerNotNull(object, TAG_SPEED);
        if (object.has(TAG_FILTER_INDEX)) {
            filterIndex = getIntegerNotNull(object, TAG_FILTER_INDEX);
        } else {
            filterIndex = -1;
        }
        for (int i = 0 ; i < FILTER_SIZE ; i++) {
            if (object.has(TAG_FLT + i)) {
                filters.set(i, JSonTools.jsonToItemStack(object.get(TAG_FLT + i).getAsJsonObject()));
            } else {
                filters.set(i, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public void readFromNBT(CompoundTag tag) {
        super.readFromNBT(tag);
    }

    @Override
    public void writeToNBT(CompoundTag tag) {
        super.writeToNBT(tag);
    }
}
