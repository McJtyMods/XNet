package mcjty.xnet.apiimpl.items.enums;

import mcjty.lib.varia.ComponentFactory;
import mcjty.xnet.utils.I18nUtils;
import mcjty.xnet.utils.ITranslatableEnum;

public enum ExtractMode implements ITranslatableEnum<ExtractMode> {
    FIRST("xnet.enum.items.extractmode.first"),
    RND("xnet.enum.items.extractmode.rnd"),
    ORDER("xnet.enum.items.extractmode.order");
    private final String i18n;

    ExtractMode(String i18n) {
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
