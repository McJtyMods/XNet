package mcjty.xnet.apiimpl.logic;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import mcjty.lib.varia.JSonTools;
import mcjty.rftoolsbase.api.xnet.gui.IEditorGui;
import mcjty.rftoolsbase.api.xnet.gui.IndicatorIcon;
import mcjty.rftoolsbase.api.xnet.helper.AbstractConnectorSettings;
import mcjty.rftoolsbase.api.xnet.helper.BaseStringTranslators;
import mcjty.xnet.XNet;
import mcjty.xnet.apiimpl.EnumStringTranslators;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LogicConnectorSettings extends AbstractConnectorSettings {

    public static final ResourceLocation iconGuiElements = ResourceLocation.fromNamespaceAndPath(XNet.MODID, "textures/gui/guielements.png");

    public static final String TAG_MODE = "mode";
    public static final String TAG_SPEED = "speed";
    public static final String TAG_REDSTONE_OUT = "rsout";

    public enum LogicMode {
        SENSOR,
        OUTPUT
    }

    public static final int SENSORS = 4;

    private LogicMode logicMode = LogicMode.SENSOR;
    private List<Sensor> sensors = null;

    private int colors;         // Current colormask
    private int speed = 2;
    private Integer redstoneOut;    // Redstone output value

    public LogicConnectorSettings(@Nonnull Direction side) {
        super(side);
        sensors = new ArrayList<>(SENSORS);
        for (int i = 0 ; i < SENSORS ; i++) {
            sensors.add(new Sensor(i));
        }
    }

    public List<Sensor> getSensors() {
        return sensors;
    }

    public void setColorMask(int colors) {
        this.colors = colors;
    }

    public int getColorMask() {
        return colors;
    }

    public Integer getRedstoneOut() {
        return redstoneOut;
    }

    @Nullable
    @Override
    public IndicatorIcon getIndicatorIcon() {
        return switch (logicMode) {
            case SENSOR -> new IndicatorIcon(iconGuiElements, 26, 70, 13, 10);
            case OUTPUT -> new IndicatorIcon(iconGuiElements, 39, 70, 13, 10);
        };
    }



    @Nullable
    @Override
    public String getIndicator() {
        return null;
    }

    private static final Set<String> TAGS = ImmutableSet.of(TAG_REDSTONE_OUT, TAG_MODE, TAG_RS, TAG_COLOR+"0", TAG_COLOR+"1", TAG_COLOR+"2", TAG_COLOR+"3");

    @Override
    public boolean isEnabled(String tag) {
        if (tag.equals(TAG_FACING)) {
            return advanced && logicMode != LogicMode.OUTPUT;
        }
        if (tag.equals(TAG_SPEED)) {
            return true;
        }
        for (Sensor sensor : sensors) {
            if (sensor.isEnabled(tag)) {
                return true;
            }
        }

        return TAGS.contains(tag);
    }

    public int getSpeed() {
        return speed;
    }

    public LogicMode getLogicMode() {
        return logicMode;
    }


    @Override
    public void createGui(IEditorGui gui) {
        advanced = gui.isAdvanced();
        String[] speeds;
        if (advanced) {
            speeds = new String[] { "5", "10", "20", "60", "100", "200" };
        } else {
            speeds = new String[] { "10", "20", "60", "100", "200" };
        }
        sideGui(gui);
        colorsGui(gui);
        redstoneGui(gui);
        gui.nl()
                .choices(TAG_MODE, "Sensor or Output mode", logicMode, LogicMode.values())
                .choices(TAG_SPEED, (logicMode == LogicMode.SENSOR ? "Number of ticks for each check" : "Number of ticks for each operation"), Integer.toString(speed * 5), speeds)
                .nl();
        if (logicMode == LogicMode.SENSOR) {
            for (Sensor sensor : sensors) {
                sensor.createGui(gui);
            }
        } else {
            gui.label("Redstone:")
                    .integer(TAG_REDSTONE_OUT, "Redstone output value", redstoneOut, 40, 16)
                    .nl();
        }
    }

    @Override
    public void update(Map<String, Object> data) {
        super.update(data);
        logicMode = LogicMode.valueOf(((String)data.get(TAG_MODE)).toUpperCase());
        String facing = (String) data.get(TAG_FACING);
        // @todo suspicious

        speed = Integer.parseInt((String) data.get(TAG_SPEED)) / 5;
        if (speed == 0) {
            speed = 2;
        }
        if (logicMode == LogicMode.SENSOR) {
            for (Sensor sensor : sensors) {
                sensor.update(data);
            }
        } else {
            redstoneOut = (Integer) data.get(TAG_REDSTONE_OUT);
        }
    }

    @Override
    public JsonObject writeToJson() {
        JsonObject object = new JsonObject();
        super.writeToJsonInternal(object);
        setEnumSafe(object, "logicmode", logicMode);
        setIntegerSafe(object, "speed", speed);
        JsonArray sensorArray = new JsonArray();
        for (Sensor sensor : sensors) {
            JsonObject o = new JsonObject();
            setEnumSafe(o, "sensormode", sensor.getSensorMode());
            setEnumSafe(o, "outputcolor", sensor.getOutputColor());
            setEnumSafe(o, "operator", sensor.getOperator());
            setIntegerSafe(o, "amount", sensor.getAmount());
            if (!sensor.getFilter().isEmpty()) {
                o.add("filter", JSonTools.itemStackToJson(sensor.getFilter()));
            }
            sensorArray.add(o);
        }
        object.add("sensors", sensorArray);
        if (speed == 1) {
            object.add("advancedneeded", new JsonPrimitive(true));
        }
        return object;
    }

    @Override
    public void readFromJson(JsonObject object) {
        super.readFromJsonInternal(object);
        logicMode = getEnumSafe(object, "logicmode", EnumStringTranslators::getLogicMode);
        speed = getIntegerNotNull(object, "speed");
        JsonArray sensorArray = object.get("sensors").getAsJsonArray();
        sensors.clear();
        for (JsonElement oe : sensorArray) {
            JsonObject o = oe.getAsJsonObject();
            Sensor sensor = new Sensor(sensors.size());
            sensor.setAmount(getIntegerNotNull(o, "amount"));
            sensor.setOperator(getEnumSafe(o, "operator", EnumStringTranslators::getOperator));
            sensor.setOutputColor(getEnumSafe(o, "outputcolor", BaseStringTranslators::getColor));
            sensor.setSensorMode(getEnumSafe(o, "sensormode", EnumStringTranslators::getSensorMode));
            if (o.has("filter")) {
                sensor.setFilter(JSonTools.jsonToItemStack(o.get("filter").getAsJsonObject()));
            } else {
                sensor.setFilter(ItemStack.EMPTY);
            }
            sensors.add(sensor);
        }
    }

    @Override
    public void readFromNBT(CompoundTag tag) {
        super.readFromNBT(tag);
        logicMode = LogicMode.values()[tag.getByte("logicMode")];
        speed = tag.getInt("speed");
        if (speed == 0) {
            speed = 2;
        }
        colors = tag.getInt("colors");
        for (Sensor sensor : sensors) {
            sensor.readFromNBT(tag);
        }
        redstoneOut = tag.getInt("rsout");
    }

    @Override
    public void writeToNBT(CompoundTag tag) {
        super.writeToNBT(tag);
        tag.putByte("logicMode", (byte) logicMode.ordinal());
        tag.putInt("speed", speed);
        tag.putInt("colors", colors);
        for (Sensor sensor : sensors) {
            sensor.writeToNBT(tag);
        }
        if (redstoneOut != null) {
            tag.putInt("rsout", redstoneOut);
        }
    }

}
