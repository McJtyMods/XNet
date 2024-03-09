package mcjty.xnet.utils;

import mcjty.rftoolsbase.api.xnet.channels.Color;
import mcjty.xnet.apiimpl.enums.ChannelMode;
import mcjty.xnet.apiimpl.enums.InsExtMode;
import mcjty.xnet.apiimpl.items.enums.ExtractMode;
import mcjty.xnet.apiimpl.items.enums.StackMode;
import mcjty.xnet.apiimpl.logic.enums.LogicFilter;
import mcjty.xnet.apiimpl.logic.enums.LogicMode;
import mcjty.xnet.apiimpl.logic.enums.Operator;
import mcjty.xnet.apiimpl.logic.enums.SensorMode;
import net.minecraft.world.item.ItemStack;

import static mcjty.rftoolsbase.api.xnet.channels.Color.OFF;

public class CastTools {
    public static Color safeColor(Object o) {
        if (o != null) {
            return Color.colorByValue((Integer) o);
        } else {
            return OFF;
        }
    }

    public static LogicFilter safeLogicFilter(Object o) {
        if (o != null) {
            return LogicFilter.values()[(int) o];
        } else {
            return LogicFilter.OFF;
        }
    }

    public static int safeInt(Object o) {
        return safeIntOrValue(o, 0);
    }

    public static int safeIntOrValue(Object o, int value) {
        if (o instanceof Integer) {
            return (Integer) o;
        } else {
            return value;
        }
    }

    public static ChannelMode safeChannelMode(Object o) {
        if (o != null) {
            return ChannelMode.values()[(int) o];
        } else {
            return ChannelMode.PRIORITY;
        }
    }

    public static InsExtMode safeInsExtMode(Object o) {
        if (o != null) {
            return InsExtMode.values()[(int) o];
        } else {
            return InsExtMode.INS;
        }
    }

    public static ItemStack safeItemStack(Object o) {
        if (o != null) {
            return (ItemStack) o;
        } else {
            return ItemStack.EMPTY;
        }
    }

    public static ExtractMode safeExtractMode(Object o) {
        if (o != null) {
            return ExtractMode.values()[(int) o];
        } else {
            return ExtractMode.FIRST;
        }
    }

    public static StackMode safeStackMode(Object o) {
        if (o != null) {
            return StackMode.values()[(int) o];
        } else {
            return StackMode.SINGLE;
        }
    }

    public static LogicMode safeLogicMode(Object o) {
        if (o != null) {
            return LogicMode.values()[(int) o];
        } else {
            return LogicMode.SENSOR;
        }
    }

    public static SensorMode safeSensorMode(Object o) {
        if (o != null) {
            return SensorMode.values()[(int) o];
        } else {
            return SensorMode.OFF;
        }
    }

    public static Operator safeOperator(Object o) {
        if (o != null) {
            return Operator.valueOfCode(((String) o).toUpperCase());
        } else {
            return Operator.EQUAL;
        }
    }
}
