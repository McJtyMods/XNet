package mcjty.xnet.apiimpl.logic.enums;


import mcjty.lib.varia.ComponentFactory;
import mcjty.xnet.utils.I18nUtils;
import mcjty.xnet.utils.ITranslatableEnum;


public enum LogicMode implements ITranslatableEnum<LogicMode> {
    SENSOR( "xnet.enum.logic.logicmode.sensor"),
    OUTPUT("xnet.enum.logic.logicmode.output");

    private final String i18n;

    LogicMode(String i18n) {
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
