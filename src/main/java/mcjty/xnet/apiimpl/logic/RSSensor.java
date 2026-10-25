package mcjty.xnet.apiimpl.logic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mcjty.lib.varia.FluidTools;
import mcjty.rftoolsbase.api.xnet.channels.Color;
import mcjty.rftoolsbase.api.xnet.gui.IEditorGui;
import mcjty.xnet.apiimpl.energy.EnergyChannelSettings;
import mcjty.xnet.apiimpl.fluids.FluidChannelSettings;
import mcjty.xnet.apiimpl.items.ItemChannelSettings;
import mcjty.xnet.apiimpl.logic.enums.Operator;
import mcjty.xnet.apiimpl.logic.enums.SensorMode;
import mcjty.xnet.compat.RFToolsSupport;
import mcjty.xnet.utils.CastTools;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import mcjty.lib.varia.FluidStack;
import mcjty.lib.varia.IFluidHandler;
import mcjty.lib.container.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

import static mcjty.rftoolsbase.api.xnet.channels.Color.COLORS;
import static mcjty.rftoolsbase.api.xnet.channels.Color.OFF;
import static mcjty.xnet.apiimpl.Constants.*;
import static mcjty.xnet.utils.I18nConstants.*;

public class RSSensor {

    private final int index;

    private SensorMode sensorMode = SensorMode.OFF;
    private Operator operator = Operator.EQUAL;
    private int amount = 0;
    private Color outputColor = OFF;
    private ItemStack filter = ItemStack.EMPTY;

    public static final Codec<RSSensor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("index").forGetter(s -> s.index),
            SensorMode.CODEC.fieldOf("sensorMode").forGetter(RSSensor::getSensorMode),
            Operator.CODEC.fieldOf("operator").forGetter(RSSensor::getOperator),
            Codec.INT.fieldOf("amount").forGetter(RSSensor::getAmount),
            Color.CODEC.fieldOf("outputColor").forGetter(RSSensor::getOutputColor),
            ItemStack.OPTIONAL_CODEC.fieldOf("filter").forGetter(RSSensor::getFilter)
    ).apply(instance, RSSensor::new));

    public final static StreamCodec<RegistryFriendlyByteBuf, RSSensor> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, s -> s.index,
            SensorMode.STREAM_CODEC, RSSensor::getSensorMode,
            Operator.STREAM_CODEC, RSSensor::getOperator,
            ByteBufCodecs.INT, RSSensor::getAmount,
            Color.STREAM_CODEC, RSSensor::getOutputColor,
            ItemStack.OPTIONAL_STREAM_CODEC, RSSensor::getFilter,
            RSSensor::new
    );

    public RSSensor(int index, SensorMode sensorMode, Operator operator, int amount, Color outputColor, ItemStack filter) {
        this.index = index;
        this.sensorMode = sensorMode;
        this.operator = operator;
        this.amount = amount;
        this.outputColor = outputColor;
        this.filter = filter;
    }

    public RSSensor(int index) {
        this.index = index;
    }

    public SensorMode getSensorMode() {
        return sensorMode;
    }

    public void setSensorMode(SensorMode sensorMode) {
        this.sensorMode = sensorMode;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public ItemStack getFilter() {
        return filter;
    }

    public void setFilter(ItemStack filter) {
        this.filter = filter;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public Color getOutputColor() {
        return outputColor;
    }

    public void setOutputColor(Color outputColor) {
        this.outputColor = outputColor;
    }

    public boolean isEnabled(String tag) {
        if ((TAG_MODE + index).equals(tag)) {
            return true;
        }
        if ((TAG_OP + index).equals(tag)) {
            return true;
        }
        if ((TAG_AMOUNT + index).equals(tag)) {
            return true;
        }
        if ((TAG_COLOR + index).equals(tag)) {
            return true;
        }
        if ((TAG_STACK + index).equals(tag)) {
            return sensorMode == SensorMode.FLUID || sensorMode == SensorMode.ITEM;
        }
        return false;
    }

    public void createGui(IEditorGui gui) {
        gui.translatableChoices(TAG_MODE + index, sensorMode, SensorMode.values());
        gui
                .choices(TAG_OP + index, LOGIC_SENSOR_OPERATOR_TOOLTIP.i18n(), operator, Operator.values())
                .integer(TAG_AMOUNT + index, LOGIC_SENSOR_AMOUNT_TOOLTIP.i18n(), amount, 46)
                .colors(TAG_COLOR + index, LOGIC_SENSOR_OUT_COLOR_TOOLTIP.i18n(), outputColor.getColor(), COLORS)
                .ghostSlot(TAG_STACK + index, filter)
                .nl();
    }

    public boolean test(@Nullable BlockEntity te, @Nonnull Level world, @Nonnull BlockPos pos, LogicConnectorSettings settings) {
        switch (sensorMode) {
            case ITEM -> {
                if (RFToolsSupport.isStorageScanner(te)) {
                    int cnt = RFToolsSupport.countItems(te, filter, amount + 1);
                    return operator.match(cnt, amount);
                } else {
                    IItemHandler handler = ItemChannelSettings.getItemHandlerAt(te, settings.getFacing());
                    if (handler == null) {
                        return false;
                    }
                    int cnt = countItem(handler, filter, amount + 1);
                    return operator.match(cnt, amount);
                }
            }
            case FLUID -> {
                IFluidHandler fluidHandler = FluidChannelSettings.getFluidHandlerAt(te, settings.getFacing());
                if (fluidHandler == null) {
                    return false;
                } else {
                    int cnt = countFluid(fluidHandler, filter, amount + 1);
                    return operator.match(cnt, amount);
                }
            }
            case ENERGY -> {
                if (EnergyChannelSettings.isEnergyTE(te, settings.getFacing())) {
                    int cnt = EnergyChannelSettings.getEnergyLevel(te, settings.getFacing());
                    return operator.match(cnt, amount);
                }
            }
            case RS -> {
                int cnt = world.getSignal(pos, settings.getFacing());
                return operator.match(cnt, amount);
            }
            case OFF -> {
            }
        }

        return false;
    }

    public void update(Map<String, Object> data) {
        sensorMode = CastTools.safeSensorMode(data.get(TAG_MODE + index));
        operator = CastTools.safeOperator(data.get(TAG_OP + index));
        amount = CastTools.safeInt(data.get(TAG_AMOUNT + index));
        outputColor = CastTools.safeColor(data.get(TAG_COLOR + index));
        filter = CastTools.safeItemStack(data.get(TAG_STACK + index));
    }

    public void readFromNBT(CompoundTag tag) {
    }

    public void writeToNBT(CompoundTag tag) {
    }


    // Count items. We will stop early if we have enough to satisfy the sensor
    private int countItem(@Nonnull IItemHandler handler, ItemStack matcher, int maxNeeded) {
        int cnt = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                if (!matcher.isEmpty()) {
                    // @todo 1.14 oredict?
                    if (ItemStack.isSameItem(matcher, stack)) {
                        cnt += stack.getCount();
                        if (cnt >= maxNeeded) {
                            return cnt;
                        }
                    }
                } else {
                    cnt += stack.getCount();
                    if (cnt >= maxNeeded) {
                        return cnt;
                    }
                }
            }
        }
        return cnt;
    }

    private int countFluid(@Nonnull IFluidHandler handler, ItemStack matcher, int maxNeeded) {
        FluidStack fluidStack;
        if (!matcher.isEmpty()) {
            fluidStack = FluidTools.convertBucketToFluid(matcher);
        } else {
            fluidStack = null;
        }
        int cnt = 0;
        for (int i = 0 ; i < handler.getTanks() ; i++) {
            FluidStack contents = handler.getFluidInTank(i);
            if (!contents.isEmpty()) {
                if (fluidStack != null) {
                    if (FluidStack.isSameFluidSameComponents(fluidStack, contents)) {
                        cnt += contents.getAmount();
                        if (cnt >= maxNeeded) {
                            return cnt;
                        }
                    }
                } else {
                    cnt += contents.getAmount();
                    if (cnt >= maxNeeded) {
                        return cnt;
                    }
                }
            }
        }
        return cnt;
    }
}
