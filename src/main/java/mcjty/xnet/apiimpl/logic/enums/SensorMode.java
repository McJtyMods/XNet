package mcjty.xnet.apiimpl.logic.enums;

import mcjty.lib.varia.ComponentFactory;
import mcjty.xnet.utils.I18nUtils;
import mcjty.xnet.utils.ITranslatableEnum;

public enum SensorMode implements ITranslatableEnum<SensorMode> {
    OFF("xnet.enum.logic.sensormode.off"),
    ITEM("xnet.enum.logic.sensormode.item"),
    FLUID("xnet.enum.logic.sensormode.fluid"),
    ENERGY("xnet.enum.logic.sensormode.energy"),
    RS("xnet.enum.logic.sensormode.rs");

    private final String i18n;

    SensorMode(String i18n) {
        this.i18n = i18n;
    }


    @Override
    public String getI18n() {
        return ComponentFactory.translatable(i18n).getString();
    }

    @Override
    public String[] getI18nSplitedTooltip() {
        return I18nUtils.getSplitedEnumTooltip(i18n);
    }
}
