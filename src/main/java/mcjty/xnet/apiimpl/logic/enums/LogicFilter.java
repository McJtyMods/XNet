package mcjty.xnet.apiimpl.logic.enums;

import mcjty.xnet.utils.I18nUtils;
import mcjty.xnet.utils.ITranslatableEnum;
import org.apache.commons.lang3.StringUtils;

public enum LogicFilter implements ITranslatableEnum<LogicFilter> {
    OFF("xnet.enum.logic.logicfilter.off"),
    NOT("xnet.enum.logic.logicfilter.not"),
    OR("xnet.enum.logic.logicfilter.or"),
    AND("xnet.enum.logic.logicfilter.and"),
    NOR("xnet.enum.logic.logicfilter.nor"),
    NAND("xnet.enum.logic.logicfilter.nand"),
    XOR("xnet.enum.logic.logicfilter.xor"),
    XNOR("xnet.enum.logic.logicfilter.xnor"),
    LATCH("xnet.enum.logic.logicfilter.latch"),
    COUNTER("xnet.enum.logic.logicfilter.counter"),
    TIMER("xnet.enum.logic.logicfilter.timer");

    private final String i18n;

    LogicFilter(String i18n) {
        this.i18n = i18n;
    }

    @Override
    public String getI18n(){
        // This enum is not translatable with width limit. Only tooltips are used
        return StringUtils.capitalize(this.name().toLowerCase());
    }

    @Override
    public String[] getI18nSplitedTooltip() {
        return I18nUtils.getSplitedEnumTooltip(i18n);
    }
}
