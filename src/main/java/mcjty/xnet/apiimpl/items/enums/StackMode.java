package mcjty.xnet.apiimpl.items.enums;

import mcjty.lib.varia.ComponentFactory;
import mcjty.xnet.utils.I18nUtils;
import mcjty.xnet.utils.ITranslatableEnum;

public enum StackMode implements ITranslatableEnum<StackMode> {
    SINGLE("xnet.enum.items.stackmode.single"),
    STACK("xnet.enum.items.stackmode.stack"),
    COUNT("xnet.enum.items.stackmode.count");

    private final String i18n;

    StackMode(String i18n) {
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
