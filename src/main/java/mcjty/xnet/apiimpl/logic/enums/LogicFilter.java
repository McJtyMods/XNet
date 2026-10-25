package mcjty.xnet.apiimpl.logic.enums;

import com.mojang.serialization.Codec;
import mcjty.lib.gui.ITranslatableEnum;
import mcjty.rftoolsbase.api.xnet.gui.IEditorGui;
import mcjty.xnet.apiimpl.logic.RSOutput;
import mcjty.xnet.utils.I18nUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import mcjty.lib.network.StreamCodecs;
import org.apache.commons.lang3.StringUtils;

import static mcjty.rftoolsbase.api.xnet.channels.Color.COLORS;
import static mcjty.xnet.apiimpl.Constants.*;
import static mcjty.xnet.utils.I18nConstants.*;

public enum LogicFilter implements ITranslatableEnum<LogicFilter>, StringRepresentable {
    DIRECT("xnet.enum.logic.logicfilter.direct") {
        @Override
        public void createGui(RSOutput output, IEditorGui gui) {
            gui.colors(TAG_RS_CHANNEL_1, LOGIC_INPUT_CHANNEL_TOOLTIP.i18n(), output.getInputChannel1().getColor(), COLORS);
            gui.integer(TAG_REDSTONE_OUT, LOGIC_RS_TOOLTIP.i18n(), output.getRedstoneOut(), 30, 15, 0).nl();
            createPulseGui(output, gui);
        }
    },
    INVERTED("xnet.enum.logic.logicfilter.inverted") {
        @Override
        public void createGui(RSOutput output, IEditorGui gui) {
            gui.colors(TAG_RS_CHANNEL_1, LOGIC_INPUT_CHANNEL_TOOLTIP.i18n(), output.getInputChannel1().getColor(), COLORS);
            gui.integer(TAG_REDSTONE_OUT, LOGIC_RS_TOOLTIP.i18n(), output.getRedstoneOut(), 30, 15, 0).nl();
            createPulseGui(output, gui);
        }
    },
    OR("xnet.enum.logic.logicfilter.or") {
        @Override
        public void createGui(RSOutput output, IEditorGui gui) {
            gui.colors(TAG_RS_CHANNEL_1, LOGIC_INPUT_CHANNEL_TOOLTIP.i18n() + " 1", output.getInputChannel1().getColor(), COLORS);
            gui.colors(TAG_RS_CHANNEL_2, LOGIC_INPUT_CHANNEL_TOOLTIP.i18n() + " 2", output.getInputChannel2().getColor(), COLORS);
            gui.integer(TAG_REDSTONE_OUT, LOGIC_RS_TOOLTIP.i18n(), output.getRedstoneOut(), 30, 15, 0).nl();
            createPulseGui(output, gui);
        }
    },
    AND("xnet.enum.logic.logicfilter.and") {
        @Override
        public void createGui(RSOutput output, IEditorGui gui) {
            gui.colors(TAG_RS_CHANNEL_1, LOGIC_INPUT_CHANNEL_TOOLTIP.i18n() + " 1", output.getInputChannel1().getColor(), COLORS);
            gui.colors(TAG_RS_CHANNEL_2, LOGIC_INPUT_CHANNEL_TOOLTIP.i18n() + " 2", output.getInputChannel2().getColor(), COLORS);
            gui.integer(TAG_REDSTONE_OUT, LOGIC_RS_TOOLTIP.i18n(), output.getRedstoneOut(), 30, 15, 0).nl();
            createPulseGui(output, gui);
        }
    },
    NOR("xnet.enum.logic.logicfilter.nor") {
        @Override
        public void createGui(RSOutput output, IEditorGui gui) {
            gui.colors(TAG_RS_CHANNEL_1, LOGIC_INPUT_CHANNEL_TOOLTIP.i18n() + " 1", output.getInputChannel1().getColor(), COLORS);
            gui.colors(TAG_RS_CHANNEL_2, LOGIC_INPUT_CHANNEL_TOOLTIP.i18n() + " 2", output.getInputChannel2().getColor(), COLORS);
            gui.integer(TAG_REDSTONE_OUT, LOGIC_RS_TOOLTIP.i18n(), output.getRedstoneOut(), 30, 15, 0).nl();
            createPulseGui(output, gui);
        }
    },
    NAND("xnet.enum.logic.logicfilter.nand") {
        @Override
        public void createGui(RSOutput output, IEditorGui gui) {
            gui.colors(TAG_RS_CHANNEL_1, LOGIC_INPUT_CHANNEL_TOOLTIP.i18n() + " 1", output.getInputChannel1().getColor(), COLORS);
            gui.colors(TAG_RS_CHANNEL_2, LOGIC_INPUT_CHANNEL_TOOLTIP.i18n() + " 2", output.getInputChannel2().getColor(), COLORS);
            gui.integer(TAG_REDSTONE_OUT, LOGIC_RS_TOOLTIP.i18n(), output.getRedstoneOut(), 30, 15, 0).nl();
            createPulseGui(output, gui);
        }
    },
    XOR("xnet.enum.logic.logicfilter.xor") {
        @Override
        public void createGui(RSOutput output, IEditorGui gui) {
            gui.colors(TAG_RS_CHANNEL_1, LOGIC_INPUT_CHANNEL_TOOLTIP.i18n() + " 1", output.getInputChannel1().getColor(), COLORS);
            gui.colors(TAG_RS_CHANNEL_2, LOGIC_INPUT_CHANNEL_TOOLTIP.i18n() + " 2", output.getInputChannel2().getColor(), COLORS);
            gui.integer(TAG_REDSTONE_OUT, LOGIC_RS_TOOLTIP.i18n(), output.getRedstoneOut(), 30, 15, 0).nl();
            createPulseGui(output, gui);
        }
    },
    XNOR("xnet.enum.logic.logicfilter.xnor") {
        @Override
        public void createGui(RSOutput output, IEditorGui gui) {
            gui.colors(TAG_RS_CHANNEL_1, LOGIC_INPUT_CHANNEL_TOOLTIP.i18n() + " 1", output.getInputChannel1().getColor(), COLORS);
            gui.colors(TAG_RS_CHANNEL_2, LOGIC_INPUT_CHANNEL_TOOLTIP.i18n() + " 2", output.getInputChannel2().getColor(), COLORS);
            gui.integer(TAG_REDSTONE_OUT, LOGIC_RS_TOOLTIP.i18n(), output.getRedstoneOut(), 30, 15, 0).nl();
            createPulseGui(output, gui);
        }
    },
    LATCH("xnet.enum.logic.logicfilter.latch") {
        @Override
        public void createGui(RSOutput output, IEditorGui gui) {
            gui.colors(TAG_RS_CHANNEL_1, LOGIC_INPUT_CHANNEL_TOOLTIP.i18n(), output.getInputChannel1().getColor(), COLORS);
            gui.integer(TAG_REDSTONE_OUT, LOGIC_RS_TOOLTIP.i18n(), output.getRedstoneOut(), 30, 15, 0).nl();
        }
    },
    COUNTER("xnet.enum.logic.logicfilter.counter") {
        @Override
        public void createGui(RSOutput output, IEditorGui gui) {
            gui.colors(TAG_RS_CHANNEL_1, LOGIC_INPUT_CHANNEL_TOOLTIP.i18n(), output.getInputChannel1().getColor(), COLORS);
            gui.integer(TAG_RS_COUNTER, LOGIC_COUNTER_FILTER_TOOLTIP.i18n(), output.getCountingHolder(), 50, Integer.MAX_VALUE, 0);
            gui.integer(TAG_REDSTONE_OUT, LOGIC_RS_TOOLTIP.i18n(), output.getRedstoneOut(), 30, 15, 0).nl();
        }
    },
    TIMER("xnet.enum.logic.logicfilter.timer") {
        @Override
        public void createGui(RSOutput output, IEditorGui gui) {
            gui.integer(TAG_RS_TIMER, LOGIC_TIMER_FILTER_TOOLTIP.i18n(), output.getTicksHolder(), 50, Integer.MAX_VALUE, 5);
            gui.integer(TAG_REDSTONE_OUT, LOGIC_RS_TOOLTIP.i18n(), output.getRedstoneOut(), 30, 15, 0).nl();
        }
    },
    STATIC("xnet.enum.logic.logicfilter.static") {
        @Override
        public void createGui(RSOutput output, IEditorGui gui) {
            gui.integer(TAG_REDSTONE_OUT, LOGIC_RS_TOOLTIP.i18n(), output.getRedstoneOut(), 30, 15, 0).nl();
        }
    },;

    private final String i18n;

    public static final Codec<LogicFilter> CODEC = StringRepresentable.fromEnum(LogicFilter::values);
    public static final StreamCodec<FriendlyByteBuf, LogicFilter> STREAM_CODEC = StreamCodecs.enumCodec(LogicFilter.class);

    LogicFilter(String i18n) {
        this.i18n = i18n;
    }

    public abstract void createGui(RSOutput output, IEditorGui gui);

    @Override
    public String getI18n(){
        // This enum is not translatable with width limit. Only tooltips are used
        return StringUtils.capitalize(this.name().toLowerCase());
    }

    @Override
    public String[] getI18nSplitedTooltip() {
        return I18nUtils.getSplitedEnumTooltip(i18n);
    }

    private static void createPulseGui(RSOutput output, IEditorGui gui) {
        gui.label(LOGIC_IMPULSE_MODE_LABEL.i18n());
        gui.toggle(TAG_IMPULSE, LOGIC_IMPULSE_MODE_TOOLTIP.i18n(), output.isImpulse());
        gui.integer(TAG_IMPULSE_DUR, LOGIC_IMPULSE_DUR_TOOLTIP.i18n(), output.getImpulseDuration(), 36, 9999, 1).nl();
    }

    @Override
    public String getSerializedName() {
        return name();
    }
}
