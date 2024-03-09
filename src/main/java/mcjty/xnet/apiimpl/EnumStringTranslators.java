package mcjty.xnet.apiimpl;

import mcjty.xnet.apiimpl.enums.ChannelMode;
import mcjty.xnet.apiimpl.enums.InsExtMode;
import mcjty.xnet.apiimpl.items.enums.ExtractMode;
import mcjty.xnet.apiimpl.items.enums.StackMode;
import mcjty.xnet.apiimpl.logic.enums.LogicFilter;
import mcjty.xnet.apiimpl.logic.enums.LogicMode;
import mcjty.xnet.apiimpl.logic.enums.Operator;
import mcjty.xnet.apiimpl.logic.enums.SensorMode;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class EnumStringTranslators {

    private static Map<String, InsExtMode> itemModeMap;
    private static Map<String, ExtractMode> extractModeMap;
    private static Map<String, StackMode> stackModeMap;
    private static Map<String, ChannelMode> itemChannelModeMap;
    private static Map<String, ChannelMode> fluidChannelModeMap;
    private static Map<String, InsExtMode> fluidModeMap;
    private static Map<String, InsExtMode> energyModeMap;
    private static Map<String, LogicMode> logicModeMap;
    private static Map<String, SensorMode> sensorModeMap;
    private static Map<String, Operator> operatorMap;
    private static Map<String, LogicFilter> logicFilterMap;

    @Nullable
    public static Operator getOperator(String mode) {
        if (operatorMap == null) {
            operatorMap = new HashMap<>();
            for (Operator value : Operator.values()) {
                operatorMap.put(value.name(), value);
            }
        }
        return operatorMap.get(mode);
    }

    @Nullable
    public static LogicFilter getLogicFilter(String filter) {
        if (logicFilterMap == null) {
            logicFilterMap = new HashMap<>();
            for (LogicFilter value : LogicFilter.values()) {
                logicFilterMap.put(value.name(), value);
            }
        }
        return logicFilterMap.get(filter);
    }

    @Nullable
    public static SensorMode getSensorMode(String mode) {
        if (sensorModeMap == null) {
            sensorModeMap = new HashMap<>();
            for (SensorMode value : SensorMode.values()) {
                sensorModeMap.put(value.name(), value);
            }
        }
        return sensorModeMap.get(mode);
    }

    @Nullable
    public static LogicMode getLogicMode(String mode) {
        if (logicModeMap == null) {
            logicModeMap = new HashMap<>();
            for (LogicMode value : LogicMode.values()) {
                logicModeMap.put(value.name(), value);
            }
        }
        return logicModeMap.get(mode);
    }

    @Nullable
    public static InsExtMode getEnergyMode(String mode) {
        if (energyModeMap == null) {
            energyModeMap = new HashMap<>();
            for (InsExtMode value : InsExtMode.values()) {
                energyModeMap.put(value.name(), value);
            }
        }
        return energyModeMap.get(mode);
    }

    @Nullable
    public static InsExtMode getFluidMode(String mode) {
        if (fluidModeMap == null) {
            fluidModeMap = new HashMap<>();
            for (InsExtMode value : InsExtMode.values()) {
                fluidModeMap.put(value.name(), value);
            }
        }
        return fluidModeMap.get(mode);
    }

    @Nullable
    public static ChannelMode getFluidChannelMode(String mode) {
        if (fluidChannelModeMap == null) {
            fluidChannelModeMap = new HashMap<>();
            for (ChannelMode value : ChannelMode.values()) {
                fluidChannelModeMap.put(value.name(), value);
            }
        }
        return fluidChannelModeMap.get(mode);
    }

    @Nullable
    public static ChannelMode getItemChannelMode(String mode) {
        if (itemChannelModeMap == null) {
            itemChannelModeMap = new HashMap<>();
            for (ChannelMode value : ChannelMode.values()) {
                itemChannelModeMap.put(value.name(), value);
            }
        }
        return itemChannelModeMap.get(mode);
    }

    @Nullable
    public static InsExtMode getItemMode(String mode) {
        if (itemModeMap == null) {
            itemModeMap = new HashMap<>();
            for (InsExtMode value : mcjty.xnet.apiimpl.enums.InsExtMode.values()) {
                itemModeMap.put(value.name(), value);
            }
        }
        return itemModeMap.get(mode);
    }

    @Nullable
    public static ExtractMode getExtractMode(String mode) {
        if (extractModeMap == null) {
            extractModeMap = new HashMap<>();
            for (ExtractMode value : ExtractMode.values()) {
                extractModeMap.put(value.name(), value);
            }
        }
        return extractModeMap.get(mode);
    }

    @Nullable
    public static StackMode getStackMode(String mode) {
        if (stackModeMap == null) {
            stackModeMap = new HashMap<>();
            for (StackMode value : StackMode.values()) {
                stackModeMap.put(value.name(), value);
            }
        }
        return stackModeMap.get(mode);
    }

}
