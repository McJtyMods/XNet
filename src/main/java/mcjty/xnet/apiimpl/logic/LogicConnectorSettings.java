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
import mcjty.xnet.apiimpl.logic.enums.LogicFilter;
import mcjty.xnet.apiimpl.logic.enums.LogicMode;
import mcjty.xnet.modules.controller.client.ConnectorEditorPanel;
import mcjty.xnet.utils.CastTools;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static mcjty.xnet.apiimpl.Constants.TAG_ADVANCED_NEEDED;
import static mcjty.xnet.apiimpl.Constants.TAG_AMOUNT;
import static mcjty.xnet.apiimpl.Constants.TAG_COLORS;
import static mcjty.xnet.apiimpl.Constants.TAG_FILTER;
import static mcjty.xnet.apiimpl.Constants.TAG_LOGIC_MODE;
import static mcjty.xnet.apiimpl.Constants.TAG_MODE;
import static mcjty.xnet.apiimpl.Constants.TAG_OPERATOR;
import static mcjty.xnet.apiimpl.Constants.TAG_OUTPUT;
import static mcjty.xnet.apiimpl.Constants.TAG_OUTPUT_COLOR;
import static mcjty.xnet.apiimpl.Constants.TAG_REDSTONE_OUT;
import static mcjty.xnet.apiimpl.Constants.TAG_RS_CHANNEL_1;
import static mcjty.xnet.apiimpl.Constants.TAG_RS_CHANNEL_2;
import static mcjty.xnet.apiimpl.Constants.TAG_RS_COUNTING_HOLDER;
import static mcjty.xnet.apiimpl.Constants.TAG_RS_FILTER;
import static mcjty.xnet.apiimpl.Constants.TAG_RS_TICKS_HOLDER;
import static mcjty.xnet.apiimpl.Constants.TAG_SENSORS;
import static mcjty.xnet.apiimpl.Constants.TAG_SENSOR_MODE;

public class LogicConnectorSettings extends AbstractConnectorSettings {

    public static final ResourceLocation iconGuiElements = new ResourceLocation(XNet.MODID, "textures/gui/guielements.png");

    public static final int SENSORS = 4;

    private LogicMode logicMode = LogicMode.SENSOR;
    private List<RSSensor> sensors = null;

    private RSOutput output = null;

    private int colorMask;         // Current colormask

    public LogicConnectorSettings(@Nonnull Direction side) {
        super(side);
        sensors = new ArrayList<>(SENSORS);
        for (int i = 0 ; i < SENSORS ; i++) {
            sensors.add(new RSSensor(i));
        }
        output = new RSOutput(this.advanced);
    }

    public List<RSSensor> getSensors() {
        return sensors;
    }

    public RSOutput getOutput() {
        return output;
    }

    public void setColorMask(int colorMask) {
        this.colorMask = colorMask;
    }

    public int getColorMask() {
        return colorMask;
    }

    public LogicMode getLogicMode() {
        return logicMode;
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
        for (RSSensor sensor : sensors) {
            if (sensor.isEnabled(tag)) {
                return true;
            }
        }

        if (output.isEnabled(tag)) {
            return true;
        }

        return TAGS.contains(tag);
    }

    @Override
    public void createGui(IEditorGui gui) {
        advanced = gui.isAdvanced();
        sideGui(gui);
        colorsGui(gui);
        redstoneGui(gui);
        gui.nl();
        ((ConnectorEditorPanel)gui).translatableChoices(TAG_MODE, logicMode, LogicMode.values());// TODO: 09.03.2024 remove ConnectorEditorPanel cast after rftoolbase update
        gui.nl();
        if (logicMode == LogicMode.SENSOR) {
            for (RSSensor sensor : sensors) {
                sensor.createGui(gui);
            }
        } else {
            output.createGui((ConnectorEditorPanel) gui); // TODO: 06.03.2024 remove ConnectorEditorPanel cast after rftoolbase update
        }
    }

    @Override
    public void update(Map<String, Object> data) {
        super.update(data);
        logicMode = CastTools.safeLogicMode(data.get(TAG_MODE));
        if (logicMode == LogicMode.SENSOR) {
            for (RSSensor sensor : sensors) {
                sensor.update(data);
            }
        } else {
            output.update(data);
        }
    }

    @Override
    public JsonObject writeToJson() {
        JsonObject object = new JsonObject();
        super.writeToJsonInternal(object);
        setEnumSafe(object, TAG_LOGIC_MODE, logicMode);
        JsonArray sensorArray = new JsonArray();
        for (RSSensor sensor : sensors) {
            JsonObject o = new JsonObject();
            setEnumSafe(o, TAG_SENSOR_MODE, sensor.getSensorMode());
            setEnumSafe(o, TAG_OUTPUT_COLOR, sensor.getOutputColor());
            setEnumSafe(o, TAG_OPERATOR, sensor.getOperator());
            setIntegerSafe(o, TAG_AMOUNT, sensor.getAmount());
            if (!sensor.getFilter().isEmpty()) {
                o.add(TAG_FILTER, JSonTools.itemStackToJson(sensor.getFilter()));
            }
            sensorArray.add(o);
        }
        object.add(TAG_SENSORS, sensorArray);
        if (!output.getLogicFilter().equals(LogicFilter.OFF)) {
            object.add(TAG_ADVANCED_NEEDED, new JsonPrimitive(true));
        }
        JsonObject outputJSON = new JsonObject();
        setEnumSafe(outputJSON, TAG_RS_FILTER, output.getLogicFilter());
        setEnumSafe(outputJSON, TAG_RS_CHANNEL_1, output.getInputChannel1());
        setEnumSafe(outputJSON, TAG_RS_CHANNEL_2, output.getInputChannel2());
        outputJSON.addProperty(TAG_RS_COUNTING_HOLDER, output.getCountingHolder());
        outputJSON.addProperty(TAG_RS_TICKS_HOLDER, output.getTicksHolder());
        outputJSON.addProperty(TAG_REDSTONE_OUT, output.getRedstoneOut());
        object.add(TAG_OUTPUT, outputJSON);

        return object;
    }

    @Override
    public void readFromJson(JsonObject object) {
        super.readFromJsonInternal(object);
        logicMode = getEnumSafe(object, TAG_LOGIC_MODE, EnumStringTranslators::getLogicMode);
        JsonArray sensorArray = object.get(TAG_SENSORS).getAsJsonArray();
        sensors.clear();
        for (JsonElement oe : sensorArray) {
            JsonObject o = oe.getAsJsonObject();
            RSSensor sensor = new RSSensor(sensors.size());
            sensor.setAmount(getIntegerNotNull(o, TAG_AMOUNT));
            sensor.setOperator(getEnumSafe(o, TAG_OPERATOR, EnumStringTranslators::getOperator));
            sensor.setOutputColor(getEnumSafe(o, TAG_OUTPUT_COLOR, BaseStringTranslators::getColor));
            sensor.setSensorMode(getEnumSafe(o, TAG_SENSOR_MODE, EnumStringTranslators::getSensorMode));
            if (o.has(TAG_FILTER)) {
                sensor.setFilter(JSonTools.jsonToItemStack(o.get(TAG_FILTER).getAsJsonObject()));
            } else {
                sensor.setFilter(ItemStack.EMPTY);
            }
            sensors.add(sensor);
        }
        JsonObject outputJSON = object.getAsJsonObject(TAG_OUTPUT);
        output = new RSOutput(advanced);
        output.setLogicFilter(getEnumSafe(outputJSON, TAG_RS_FILTER, EnumStringTranslators::getLogicFilter));
        output.setInputChannel1(getEnumSafe(outputJSON, TAG_RS_CHANNEL_1, BaseStringTranslators::getColor));
        output.setInputChannel2(getEnumSafe(outputJSON, TAG_RS_CHANNEL_2, BaseStringTranslators::getColor));
        output.setCountingHolder(getIntegerNotNull(outputJSON, TAG_RS_COUNTING_HOLDER));
        output.setTicksHolder(getIntegerNotNull(outputJSON, TAG_RS_TICKS_HOLDER));
        output.setRedstoneOut(getIntegerNotNull(outputJSON, TAG_REDSTONE_OUT));
    }

    @Override
    public void readFromNBT(CompoundTag tag) {
        super.readFromNBT(tag);
        logicMode = LogicMode.values()[tag.getByte(TAG_LOGIC_MODE)];
        colorMask = tag.getInt(TAG_COLORS);
        for (RSSensor sensor : sensors) {
            sensor.readFromNBT(tag);
        }
        output.readFromNBT(tag);
    }

    @Override
    public void writeToNBT(CompoundTag tag) {
        super.writeToNBT(tag);
        tag.putByte(TAG_LOGIC_MODE, (byte) logicMode.ordinal());
        tag.putInt(TAG_COLORS, colorMask);
        for (RSSensor sensor : sensors) {
            sensor.writeToNBT(tag);
        }
        output.writeToNBT(tag);
    }

}
