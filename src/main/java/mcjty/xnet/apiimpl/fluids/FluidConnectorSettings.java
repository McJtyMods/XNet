package mcjty.xnet.apiimpl.fluids;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mcjty.lib.varia.CompositeStreamCodec;
import mcjty.lib.varia.FluidTools;
import mcjty.lib.varia.JSonTools;
import mcjty.rftoolsbase.api.xnet.channels.IChannelType;
import mcjty.rftoolsbase.api.xnet.gui.IEditorGui;
import mcjty.rftoolsbase.api.xnet.gui.IndicatorIcon;
import mcjty.rftoolsbase.api.xnet.helper.AbstractConnectorSettings;
import mcjty.xnet.XNet;
import mcjty.xnet.apiimpl.Constants;
import mcjty.xnet.apiimpl.EnumStringTranslators;
import mcjty.xnet.apiimpl.enums.InsExtMode;
import mcjty.xnet.setup.Config;
import mcjty.xnet.utils.CastTools;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static mcjty.xnet.apiimpl.Constants.TAG_ADVANCED_NEEDED;
import static mcjty.xnet.apiimpl.Constants.TAG_FLT;
import static mcjty.xnet.apiimpl.Constants.TAG_FLUID_MODE;
import static mcjty.xnet.apiimpl.Constants.TAG_MINMAX;
import static mcjty.xnet.apiimpl.Constants.TAG_MODE;
import static mcjty.xnet.apiimpl.Constants.TAG_PRIORITY;
import static mcjty.xnet.apiimpl.Constants.TAG_RATE;
import static mcjty.xnet.apiimpl.Constants.TAG_SPEED;
import static mcjty.xnet.utils.I18nConstants.EXT_ENDING;
import static mcjty.xnet.utils.I18nConstants.FILTER_LABEL;
import static mcjty.xnet.utils.I18nConstants.FLUID_MINMAX_TOOLTIP_FORMATTED;
import static mcjty.xnet.utils.I18nConstants.FLUID_RATE_TOOLTIP_FORMATTED;
import static mcjty.xnet.utils.I18nConstants.HIGH_FORMAT;
import static mcjty.xnet.utils.I18nConstants.INS_ENDING;
import static mcjty.xnet.utils.I18nConstants.LOW_FORMAT;
import static mcjty.xnet.utils.I18nConstants.MAX;
import static mcjty.xnet.utils.I18nConstants.MIN;
import static mcjty.xnet.utils.I18nConstants.PRIORITY_LABEL;
import static mcjty.xnet.utils.I18nConstants.PRIORITY_TOOLTIP;
import static mcjty.xnet.utils.I18nConstants.RATE_LABEL;
import static mcjty.xnet.utils.I18nConstants.SPEED_TOOLTIP;

public class FluidConnectorSettings extends AbstractConnectorSettings {

    public static final Identifier iconGuiElements = Identifier.fromNamespaceAndPath(XNet.MODID, "textures/gui/guielements.png");

    private InsExtMode fluidMode = InsExtMode.INS;

    @Nullable private Integer priority = 0;
    @Nullable private Integer rate = null;
    @Nullable private Integer minmax = null;
    private int speed = 2;
    private ItemStack filter = ItemStack.EMPTY;

    public static final MapCodec<FluidConnectorSettings> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BaseSettings.CODEC.fieldOf("base").forGetter(settings -> settings.settings),
            Direction.CODEC.fieldOf("side").forGetter(FluidConnectorSettings::getSide),
            InsExtMode.CODEC.fieldOf("mode").forGetter(FluidConnectorSettings::getFluidMode),
            Codec.INT.optionalFieldOf("priority").forGetter(o -> Optional.ofNullable(o.priority)),
            Codec.INT.optionalFieldOf("rate").forGetter(o -> Optional.ofNullable(o.rate)),
            Codec.INT.optionalFieldOf("minmax").forGetter(o -> Optional.ofNullable(o.minmax)),
            Codec.INT.fieldOf("speed").forGetter(FluidConnectorSettings::getSpeed),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("filter", ItemStack.EMPTY).forGetter(o -> o.filter)
    ).apply(instance, FluidConnectorSettings::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidConnectorSettings> STREAM_CODEC = CompositeStreamCodec.composite(
            BaseSettings.STREAM_CODEC, s -> s.settings,
            Direction.STREAM_CODEC, AbstractConnectorSettings::getSide,
            InsExtMode.STREAM_CODEC, FluidConnectorSettings::getFluidMode,
            ByteBufCodecs.optional(ByteBufCodecs.INT), s -> Optional.ofNullable(s.priority),
            ByteBufCodecs.optional(ByteBufCodecs.INT), s -> Optional.ofNullable(s.rate),
            ByteBufCodecs.optional(ByteBufCodecs.INT), s -> Optional.ofNullable(s.minmax),
            ByteBufCodecs.INT, s -> s.speed,
            ItemStack.OPTIONAL_STREAM_CODEC, s -> s.filter,
            FluidConnectorSettings::new
    );

    public FluidConnectorSettings(@Nonnull Direction side) {
        super(DEFAULT_SETTINGS, side);
    }

    public FluidConnectorSettings(@Nonnull BaseSettings base, @Nonnull Direction side, InsExtMode fluidMode, Optional<Integer> priority,
                                  Optional<Integer> rate, Optional<Integer> minmax, int speed, ItemStack filter) {
        super(base, side);
        this.fluidMode = fluidMode;
        this.priority = priority.orElse(null);
        this.rate = rate.orElse(null);
        this.minmax = minmax.orElse(null);
        this.speed = speed;
        this.filter = filter;
    }

    public InsExtMode getFluidMode() {
        return fluidMode;
    }

    public int getSpeed() {
        return speed;
    }

    @Override
    public IChannelType getType() {
        return XNet.setup.fluidChannelType;
    }

    @Nonnull
    public Integer getPriority() {
        return priority == null ? 0 : priority;
    }

    @Nonnull
    public Integer getRate() {
        return rate == null ? Config.maxFluidRateNormal.get() : rate;
    }

    @Nullable
    public Integer getMinmax() {
        return minmax;
    }

    @Nullable
    @Override
    public IndicatorIcon getIndicatorIcon() {
        return switch (fluidMode) {
            case INS -> new IndicatorIcon(iconGuiElements, 0, 70, 13, 10);
            case EXT -> new IndicatorIcon(iconGuiElements, 13, 70, 13, 10);
        };
    }

    @Override
    @Nullable
    public String getIndicator() {
        return null;
    }

    private String getRateTooltip() {
        return FLUID_RATE_TOOLTIP_FORMATTED.i18n(
                (fluidMode == InsExtMode.EXT ? EXT_ENDING : INS_ENDING).i18n(),
                Config.getMaxFluidRate(advanced)
        );
    }

    private String getMinMaxTooltip() {
        return FLUID_MINMAX_TOOLTIP_FORMATTED.i18n(
                (fluidMode == InsExtMode.EXT ? EXT_ENDING : INS_ENDING).i18n(),
                (fluidMode == InsExtMode.EXT ? LOW_FORMAT : HIGH_FORMAT).i18n());
    }

    @Override
    public void createGui(IEditorGui gui) {
        advanced = gui.isAdvanced();
        String[] speeds = Arrays.stream(advanced ? Constants.ADVANCED_SPEEDS : Constants.SPEEDS)
                                  .map(s -> String.valueOf(Integer.parseInt(s) * 2)).toArray(String[]::new);
        int maxrate = Config.getMaxFluidRate(advanced);

        sideGui(gui);
        colorsGui(gui);
        redstoneGui(gui);
        gui.nl();
        gui.translatableChoices(TAG_MODE, fluidMode, InsExtMode.values())
                .choices(TAG_SPEED, SPEED_TOOLTIP.i18n(), Integer.toString(speed * 10), speeds)
                .nl()

                .label(PRIORITY_LABEL.i18n()).integer(TAG_PRIORITY, PRIORITY_TOOLTIP.i18n(), priority, 36).nl()

                .label(RATE_LABEL.i18n()).integer(TAG_RATE, getRateTooltip(), rate, 36, maxrate)
                .shift(10)
                .label((fluidMode == InsExtMode.EXT ? MIN : MAX).i18n()).integer(TAG_MINMAX, getMinMaxTooltip(), minmax, 36)
                .nl()
                .label(FILTER_LABEL.i18n())
                .ghostSlot(TAG_FLT, filter);
    }

    private static final Set<String> INSERT_TAGS = ImmutableSet.of(TAG_MODE, TAG_RS, TAG_COLOR+"0", TAG_COLOR+"1", TAG_COLOR+"2", TAG_COLOR+"3", TAG_RATE, TAG_MINMAX, TAG_PRIORITY, TAG_FLT);
    private static final Set<String> EXTRACT_TAGS = ImmutableSet.of(TAG_MODE, TAG_RS, TAG_COLOR+"0", TAG_COLOR+"1", TAG_COLOR+"2", TAG_COLOR+"3", TAG_RATE, TAG_MINMAX, TAG_PRIORITY, TAG_FLT, TAG_SPEED);

    @Override
    public boolean isEnabled(String tag) {
        if (fluidMode == InsExtMode.INS) {
            if (tag.equals(TAG_FACING)) {
                return advanced;
            }
            return INSERT_TAGS.contains(tag);
        } else {
            if (tag.equals(TAG_FACING)) {
                return false;           // We cannot extract from different sides
            }
            return EXTRACT_TAGS.contains(tag);
        }
    }

    @Nullable
    public FluidStack getMatcher() {
        // @todo optimize/cache this?
        if (!filter.isEmpty()) {
            return FluidTools.convertBucketToFluid(filter);
        } else {
            return null;
        }
    }

    public void setSpeed(int speed) {
        this.speed = speed != 0 ? speed : 2;
    }


    @Override
    public void update(Map<String, Object> data) {
        super.update(data);
        fluidMode = CastTools.safeInsExtMode(data.get(TAG_MODE));
        rate = (Integer) data.get(TAG_RATE);
        minmax = (Integer) data.get(TAG_MINMAX);
        priority = (Integer) data.get(TAG_PRIORITY);
        setSpeed(Integer.parseInt((String) data.get(TAG_SPEED)) / 10);
        filter = CastTools.safeItemStack(data.get(TAG_FLT));
    }

    @Override
    public JsonObject writeToJson() {
        JsonObject object = new JsonObject();
        super.writeToJsonInternal(object);
        setEnumSafe(object, TAG_FLUID_MODE, fluidMode);
        setIntegerSafe(object, TAG_PRIORITY, priority);
        setIntegerSafe(object, TAG_RATE, rate);
        setIntegerSafe(object, TAG_MINMAX, minmax);
        setIntegerSafe(object, TAG_SPEED, speed);
        if (!filter.isEmpty()) {
            object.add(TAG_FLT, JSonTools.itemStackToJson(filter));
        }
        if ((rate != null && rate > Config.maxFluidRateNormal.get()) || speed == 1) {
            object.add(TAG_ADVANCED_NEEDED, new JsonPrimitive(true));
        }
        return object;
    }

    @Override
    public void readFromJson(JsonObject object) {
        super.readFromJsonInternal(object);
        fluidMode = getEnumSafe(object, TAG_FLUID_MODE, EnumStringTranslators::getFluidMode);
        priority = getIntegerSafe(object, TAG_PRIORITY);
        rate = getIntegerSafe(object, TAG_RATE);
        minmax = getIntegerSafe(object, TAG_MINMAX);
        speed = getIntegerNotNull(object, TAG_SPEED);
        if (object.has(TAG_FLT)) {
            filter = JSonTools.jsonToItemStack(object.get(TAG_FLT).getAsJsonObject());
        } else {
            filter = ItemStack.EMPTY;
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
