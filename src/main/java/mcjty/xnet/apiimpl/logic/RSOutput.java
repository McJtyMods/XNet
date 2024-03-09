package mcjty.xnet.apiimpl.logic;

import mcjty.rftoolsbase.api.xnet.channels.Color;
import mcjty.xnet.apiimpl.logic.enums.LogicFilter;
import mcjty.xnet.modules.controller.client.ConnectorEditorPanel;
import mcjty.xnet.utils.CastTools;
import net.minecraft.nbt.CompoundTag;

import java.util.Map;

import static mcjty.rftoolsbase.api.xnet.channels.Color.COLORS;
import static mcjty.rftoolsbase.api.xnet.channels.Color.OFF;
import static mcjty.xnet.apiimpl.Constants.TAG_REDSTONE_OUT;
import static mcjty.xnet.apiimpl.Constants.TAG_RS_CHANNEL_1;
import static mcjty.xnet.apiimpl.Constants.TAG_RS_CHANNEL_2;
import static mcjty.xnet.apiimpl.Constants.TAG_RS_COUNTER;
import static mcjty.xnet.apiimpl.Constants.TAG_RS_COUNTING_HOLDER;
import static mcjty.xnet.apiimpl.Constants.TAG_RS_FILTER;
import static mcjty.xnet.apiimpl.Constants.TAG_RS_TICKS_HOLDER;
import static mcjty.xnet.apiimpl.Constants.TAG_RS_TIMER;
import static mcjty.xnet.utils.I18nConstants.LOGIC_COUNTER_FILTER_TOOLTIP;
import static mcjty.xnet.utils.I18nConstants.LOGIC_INPUT_CHANNEL_TOOLTIP;
import static mcjty.xnet.utils.I18nConstants.LOGIC_RS_LABEL;
import static mcjty.xnet.utils.I18nConstants.LOGIC_RS_TOOLTIP;
import static mcjty.xnet.utils.I18nConstants.LOGIC_TIMER_FILTER_TOOLTIP;

public class RSOutput {
    
    private boolean isAdvanced;
    private LogicFilter logicFilter = LogicFilter.OFF;
    private Color inputChannel1 = OFF;    // First input channel for logic filter
    private Color inputChannel2 = OFF;    // Second input channel for logic filter
    private int redstoneOut = 0;    // Redstone output value
    private boolean flipFlapState = false;  // If logicFilter == LATCH shows should we output redstone signal
    private boolean lastInputTrue = false;  // If logicFilter == LATCH shows should we toggle flipFlapState

    private int countingHolder = 0;  // Holds user value for counting filter
    private int countingCurrent = 0; // Current value for counting filter

    private int ticksHolder = 5;  // Holds user value for timer filter
    private int ticksCurrent = 5; // Current value for timer filter


    public RSOutput(boolean isAdvanced) {
        this.isAdvanced = isAdvanced;
    }

    public LogicFilter getLogicFilter() {
        return logicFilter;
    }

    public Color getInputChannel1() {
        return inputChannel1;
    }

    public Color getInputChannel2() {
        return inputChannel2;
    }

    public int getRedstoneOut() {
        return redstoneOut;
    }

    public int getCountingHolder() {
        return countingHolder;
    }

    public int getTicksHolder() {
        return ticksHolder;
    }

    public boolean isAdvanced() {
        return isAdvanced;
    }

    public boolean isFlipFlapState() {
        return flipFlapState;
    }

    public boolean isLastInputTrue() {
        return lastInputTrue;
    }

    public int getCountingCurrent() {
        return countingCurrent;
    }

    public int getTicksCurrent() {
        return ticksCurrent;
    }

    protected void setAdvanced(boolean advanced) {
        isAdvanced = advanced;
    }

    public void setFlipFlapState(boolean flipFlapState) {
        this.flipFlapState = flipFlapState;
    }

    public void setLastInputTrue(boolean lastInputTrue) {
        this.lastInputTrue = lastInputTrue;
    }

    public void setCountingCurrent(int countingCurrent) {
        this.countingCurrent = countingCurrent;
    }

    public void setTicksCurrent(int ticksCurrent) {
        this.ticksCurrent = ticksCurrent;
    }

    public void setCountingHolder(int countingHolder) {
        this.countingHolder = countingHolder;
        this.countingCurrent = countingHolder;
    }

    public void setTicksHolder(int ticksHolder) {
        this.ticksHolder = ticksHolder;
        this.ticksCurrent = ticksHolder;
    }

    public void setLogicFilter(LogicFilter logicFilter) {
        this.logicFilter = logicFilter;
    }

    public void setInputChannel1(Color inputChannel1) {
        this.inputChannel1 = inputChannel1;
    }

    public void setInputChannel2(Color inputChannel2) {
        this.inputChannel2 = inputChannel2;
    }

    public void setRedstoneOut(int redstoneOut) {
        this.redstoneOut = redstoneOut;
    }

    public void createGui(ConnectorEditorPanel gui) {// TODO: 09.03.2024 remove ConnectorEditorPanel cast after rftoolbase update 
        if (gui.isAdvanced()) {
            gui.translatableChoices(TAG_RS_FILTER, logicFilter, LogicFilter.values());
            switch (logicFilter) {
                case OFF -> {}
                case COUNTER -> {
                    gui.colors(TAG_RS_CHANNEL_1, LOGIC_INPUT_CHANNEL_TOOLTIP.i18n(), inputChannel1.getColor(), COLORS);
                    gui.integer(TAG_RS_COUNTER, LOGIC_COUNTER_FILTER_TOOLTIP.i18n(), countingHolder, 50, Integer.MAX_VALUE, 0);
                }
                case LATCH, NOT -> {
                    gui.colors(TAG_RS_CHANNEL_1, LOGIC_INPUT_CHANNEL_TOOLTIP.i18n(), inputChannel1.getColor(), COLORS);
                }
                case TIMER -> {
                    gui.integer(TAG_RS_TIMER, LOGIC_TIMER_FILTER_TOOLTIP.i18n(), ticksHolder, 50, Integer.MAX_VALUE, 5);
                }
                case OR, NOR, NAND, XOR, XNOR, AND -> {
                    gui.colors(TAG_RS_CHANNEL_1, LOGIC_INPUT_CHANNEL_TOOLTIP.i18n() + " 1", inputChannel1.getColor(), COLORS);
                    gui.colors(TAG_RS_CHANNEL_2, LOGIC_INPUT_CHANNEL_TOOLTIP.i18n() + " 2", inputChannel2.getColor(), COLORS);
                }
            }
        } else {
            gui.label(LOGIC_RS_LABEL.i18n());
        }

        gui.integer(TAG_REDSTONE_OUT, LOGIC_RS_TOOLTIP.i18n(), redstoneOut, 30, 15, 0).nl();
    }

    public void update(Map<String, Object> data) {
        logicFilter = CastTools.safeLogicFilter(data.get(TAG_RS_FILTER));
        inputChannel1 = CastTools.safeColor(data.get(TAG_RS_CHANNEL_1));
        inputChannel2 = CastTools.safeColor(data.get(TAG_RS_CHANNEL_2));
        countingHolder = CastTools.safeIntOrValue(data.get(TAG_RS_COUNTER), countingHolder);
        ticksHolder = CastTools.safeIntOrValue(data.get(TAG_RS_TIMER), ticksHolder);
        redstoneOut = CastTools.safeIntOrValue(data.get(TAG_REDSTONE_OUT), redstoneOut);
    }

    public boolean isEnabled(String tag) {
        switch (tag) {
            case TAG_RS_FILTER, TAG_REDSTONE_OUT, TAG_RS_CHANNEL_1, TAG_RS_CHANNEL_2, TAG_RS_COUNTER, TAG_RS_TIMER -> {
                return true;
            }
            default -> {return false;}
        }
    }

    public void readFromNBT(CompoundTag tag) {
        logicFilter = LogicFilter.values()[tag.getByte(TAG_RS_FILTER)];
        inputChannel1 = Color.values()[tag.getByte(TAG_RS_CHANNEL_1)];
        inputChannel2 = Color.values()[tag.getByte(TAG_RS_CHANNEL_2)];
        setCountingHolder(tag.getInt(TAG_RS_COUNTING_HOLDER));
        setTicksHolder(tag.getInt(TAG_RS_TICKS_HOLDER));
        redstoneOut = tag.getInt(TAG_REDSTONE_OUT);
    }

    public void writeToNBT(CompoundTag tag) {
        tag.putByte(TAG_RS_FILTER, (byte) logicFilter.ordinal());
        tag.putByte(TAG_RS_CHANNEL_1, (byte) inputChannel1.ordinal());
        tag.putByte(TAG_RS_CHANNEL_2, (byte) inputChannel2.ordinal());
        tag.putInt(TAG_RS_COUNTING_HOLDER, countingHolder);
        tag.putInt(TAG_RS_TICKS_HOLDER, ticksHolder);
        tag.putInt(TAG_REDSTONE_OUT, redstoneOut);
    }
}
