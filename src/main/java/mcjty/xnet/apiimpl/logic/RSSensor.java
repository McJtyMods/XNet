package mcjty.xnet.apiimpl.logic;

import mcjty.lib.varia.FluidTools;
import mcjty.rftoolsbase.api.xnet.channels.Color;
import mcjty.rftoolsbase.api.xnet.gui.IEditorGui;
import mcjty.xnet.apiimpl.energy.EnergyChannelSettings;
import mcjty.xnet.apiimpl.fluids.FluidChannelSettings;
import mcjty.xnet.apiimpl.items.ItemChannelSettings;
import mcjty.xnet.apiimpl.logic.enums.Operator;
import mcjty.xnet.apiimpl.logic.enums.SensorMode;
import mcjty.xnet.compat.RFToolsSupport;
import mcjty.xnet.modules.controller.client.AbstractEditorPanel;
import mcjty.xnet.utils.CastTools;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

import static mcjty.rftoolsbase.api.xnet.channels.Color.COLORS;
import static mcjty.rftoolsbase.api.xnet.channels.Color.OFF;
import static mcjty.xnet.apiimpl.Constants.TAG_AMOUNT;
import static mcjty.xnet.apiimpl.Constants.TAG_COLOR;
import static mcjty.xnet.apiimpl.Constants.TAG_FILTER;
import static mcjty.xnet.apiimpl.Constants.TAG_MODE;
import static mcjty.xnet.apiimpl.Constants.TAG_OP;
import static mcjty.xnet.apiimpl.Constants.TAG_OPERATOR;
import static mcjty.xnet.apiimpl.Constants.TAG_SENSOR_MODE;
import static mcjty.xnet.apiimpl.Constants.TAG_STACK;
import static mcjty.xnet.utils.I18nConstants.LOGIC_SENSOR_AMOUNT_TOOLTIP;
import static mcjty.xnet.utils.I18nConstants.LOGIC_SENSOR_OPERATOR_TOOLTIP;
import static mcjty.xnet.utils.I18nConstants.LOGIC_SENSOR_OUT_COLOR_TOOLTIP;

public class RSSensor {

    private final int index;

    private SensorMode sensorMode = SensorMode.OFF;
    private Operator operator = Operator.EQUAL;
    private int amount = 0;
    private Color outputColor = OFF;
    private ItemStack filter = ItemStack.EMPTY;

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
        ((AbstractEditorPanel)gui).translatableChoices(TAG_MODE + index, sensorMode, SensorMode.values());
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
                    return ItemChannelSettings.getItemHandlerAt(te, settings.getFacing()).map(h -> {
                        int cnt = countItem(h, filter, amount + 1);
                        return operator.match(cnt, amount);
                    }).orElse(false);
                }
            }
            case FLUID -> {
                return FluidChannelSettings.getFluidHandlerAt(te, settings.getFacing()).map(h -> {
                    int cnt = countFluid(h, filter, amount + 1);
                    return operator.match(cnt, amount);
                }).orElse(false);
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
        sensorMode = SensorMode.values()[tag.getByte(TAG_SENSOR_MODE + index)];
        operator = Operator.values()[tag.getByte(TAG_OPERATOR + index)];
        amount = tag.getInt(TAG_AMOUNT + index);
        outputColor = Color.values()[tag.getByte(TAG_COLOR + index)];
        if (tag.contains(TAG_FILTER + index)) {
            CompoundTag itemTag = tag.getCompound(TAG_FILTER + index);
            filter = ItemStack.of(itemTag);
        } else {
            filter = ItemStack.EMPTY;
        }
    }

    public void writeToNBT(CompoundTag tag) {
        tag.putByte(TAG_SENSOR_MODE + index, (byte) sensorMode.ordinal());
        tag.putByte(TAG_OPERATOR + index, (byte) operator.ordinal());
        tag.putInt(TAG_AMOUNT + index, amount);
        tag.putByte(TAG_COLOR + index, (byte) outputColor.ordinal());
        if (!filter.isEmpty()) {
            CompoundTag itemTag = new CompoundTag();
            filter.save(itemTag);
            tag.put(TAG_FILTER + index, itemTag);
        }
    }

    // Count items. We will stop early if we have enough to satisfy the sensor
    private int countItem(@Nonnull IItemHandler handler, ItemStack matcher, int maxNeeded) {
        int cnt = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                if (!matcher.isEmpty()) {
                    // @todo 1.14 oredict?
                    if (matcher.sameItem(stack)) {
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
                    if (fluidStack.isFluidEqual(contents)) {
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
