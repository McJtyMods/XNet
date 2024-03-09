package mcjty.xnet.apiimpl.energy;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import mcjty.rftoolsbase.api.xnet.gui.IEditorGui;
import mcjty.rftoolsbase.api.xnet.gui.IndicatorIcon;
import mcjty.rftoolsbase.api.xnet.helper.AbstractConnectorSettings;
import mcjty.xnet.XNet;
import mcjty.xnet.apiimpl.EnumStringTranslators;
import mcjty.xnet.apiimpl.enums.InsExtMode;
import mcjty.xnet.modules.controller.client.ConnectorEditorPanel;
import mcjty.xnet.setup.Config;
import mcjty.xnet.utils.CastTools;
import mcjty.xnet.utils.TagUtils;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

import static mcjty.xnet.apiimpl.Constants.TAG_ADVANCED_NEEDED;
import static mcjty.xnet.apiimpl.Constants.TAG_ENERGY_MODE;
import static mcjty.xnet.apiimpl.Constants.TAG_MINMAX;
import static mcjty.xnet.apiimpl.Constants.TAG_MODE;
import static mcjty.xnet.apiimpl.Constants.TAG_PRIORITY;
import static mcjty.xnet.apiimpl.Constants.TAG_RATE;
import static mcjty.xnet.utils.I18nConstants.ENERGY_MINMAX_TOOLTIP_FORMATTED;
import static mcjty.xnet.utils.I18nConstants.ENERGY_RATE_TOOLTIP_FORMATTED;
import static mcjty.xnet.utils.I18nConstants.EXT_ENDING;
import static mcjty.xnet.utils.I18nConstants.HIGH_FORMAT;
import static mcjty.xnet.utils.I18nConstants.INS_ENDING;
import static mcjty.xnet.utils.I18nConstants.LOW_FORMAT;
import static mcjty.xnet.utils.I18nConstants.MAX;
import static mcjty.xnet.utils.I18nConstants.MIN;
import static mcjty.xnet.utils.I18nConstants.PRIORITY_LABEL;
import static mcjty.xnet.utils.I18nConstants.PRIORITY_TOOLTIP;
import static mcjty.xnet.utils.I18nConstants.RATE_LABEL;

public class EnergyConnectorSettings extends AbstractConnectorSettings {

    public static final ResourceLocation iconGuiElements = new ResourceLocation(XNet.MODID, "textures/gui/guielements.png");

    private InsExtMode energyMode = InsExtMode.INS;

    @Nullable private Integer priority = 0;
    @Nullable private Integer rate = null;
    @Nullable private Integer minmax = null;

    public EnergyConnectorSettings(@Nonnull Direction side) {
        super(side);
    }

    @Nullable
    @Override
    public IndicatorIcon getIndicatorIcon() {
        return switch (energyMode) {
            case INS -> new IndicatorIcon(iconGuiElements, 0, 70, 13, 10);
            case EXT -> new IndicatorIcon(iconGuiElements, 13, 70, 13, 10);
        };
    }

    @Override
    @Nullable
    public String getIndicator() {
        return null;
    }

    public InsExtMode getEnergyMode() {
        return energyMode;
    }

    private String getRateTooltip() {
        return ENERGY_RATE_TOOLTIP_FORMATTED.i18n(
                (energyMode == InsExtMode.EXT ? EXT_ENDING : INS_ENDING).i18n(),
                Config.getMaxRfRate(advanced)
        );
    }

    private String getMinMaxTooltip() {
        return ENERGY_MINMAX_TOOLTIP_FORMATTED.i18n(
                (energyMode == InsExtMode.EXT ? EXT_ENDING : INS_ENDING).i18n(),
                (energyMode == InsExtMode.EXT ? LOW_FORMAT : HIGH_FORMAT).i18n()
        );
    }

    @Override
    public void createGui(IEditorGui gui) {
        advanced = gui.isAdvanced();
        sideGui(gui);
        colorsGui(gui);
        redstoneGui(gui);
        gui.nl();
        ((ConnectorEditorPanel) gui).translatableChoices(TAG_MODE, energyMode, InsExtMode.values());// TODO: 09.03.2024 remove ConnectorEditorPanel cast after rftoolbase update
        gui.nl()
                .label(PRIORITY_LABEL.i18n()).integer(TAG_PRIORITY, PRIORITY_TOOLTIP.i18n(), priority, 30).nl()

                .label(RATE_LABEL.i18n()).integer(TAG_RATE, getRateTooltip(), rate, 40)
                .shift(10)
                .label((energyMode == InsExtMode.EXT ? MIN : MAX).i18n()).integer(TAG_MINMAX, getMinMaxTooltip(), minmax, 50);
    }

    private static final Set<String> INSERT_TAGS = ImmutableSet.of(TAG_MODE, TAG_RS, TAG_COLOR+"0", TAG_COLOR+"1", TAG_COLOR+"2", TAG_COLOR+"3", TAG_RATE, TAG_MINMAX, TAG_PRIORITY);
    private static final Set<String> EXTRACT_TAGS = ImmutableSet.of(TAG_MODE, TAG_RS, TAG_COLOR+"0", TAG_COLOR+"1", TAG_COLOR+"2", TAG_COLOR+"3", TAG_RATE, TAG_MINMAX, TAG_PRIORITY);

    @Override
    public boolean isEnabled(String tag) {
        if (energyMode == InsExtMode.INS) {
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

    @Nonnull
    public Integer getPriority() {
        return priority == null ? 0 : priority;
    }

    @Nullable
    public Integer getRate() {
        return rate;
    }

    @Nullable
    public Integer getMinmax() {
        return minmax;
    }

    @Override
    public void update(Map<String, Object> data) {
        super.update(data);
        energyMode = CastTools.safeInsExtMode(data.get(TAG_MODE));
        rate = (Integer) data.get(TAG_RATE);
        minmax = (Integer) data.get(TAG_MINMAX);
        priority = (Integer) data.get(TAG_PRIORITY);
    }

    @Override
    public JsonObject writeToJson() {
        JsonObject object = new JsonObject();
        super.writeToJsonInternal(object);
        setEnumSafe(object, TAG_ENERGY_MODE, energyMode);
        setIntegerSafe(object, TAG_PRIORITY, priority);
        setIntegerSafe(object, TAG_RATE, rate);
        setIntegerSafe(object, TAG_MINMAX, minmax);
        if (rate != null && rate > Config.maxRfRateNormal.get()) {
            object.add(TAG_ADVANCED_NEEDED, new JsonPrimitive(true));
        }
        return object;
    }

    @Override
    public void readFromJson(JsonObject object) {
        super.readFromJsonInternal(object);
        energyMode = getEnumSafe(object, TAG_ENERGY_MODE, EnumStringTranslators::getEnergyMode);
        priority = getIntegerSafe(object, TAG_PRIORITY);
        rate = getIntegerSafe(object, TAG_RATE);
        minmax = getIntegerSafe(object, TAG_MINMAX);
    }

    @Override
    public void readFromNBT(CompoundTag tag) {
        super.readFromNBT(tag);
        byte energyModeByte = tag.contains(TAG_ENERGY_MODE) 
                                      ? tag.getByte(TAG_ENERGY_MODE) 
                                      : tag.getByte("itemMode"); // TODO: 06.03.2024 backward compatibility. DELETE THIS after 1.20.4_neo branch
        energyMode = InsExtMode.values()[energyModeByte];
        priority = TagUtils.getIntOrNull(tag, TAG_PRIORITY);
        rate = TagUtils.getIntOrNull(tag, TAG_RATE);
        minmax = TagUtils.getIntOrNull(tag, TAG_MINMAX);
    }

    @Override
    public void writeToNBT(CompoundTag tag) {
        super.writeToNBT(tag);
        tag.putByte(TAG_ENERGY_MODE, (byte) energyMode.ordinal());
        TagUtils.putIntIfNotNull(tag, TAG_PRIORITY, priority);
        TagUtils.putIntIfNotNull(tag, TAG_RATE, rate);
        TagUtils.putIntIfNotNull(tag, TAG_MINMAX, minmax);
    }
}
