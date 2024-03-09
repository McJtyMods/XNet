package mcjty.xnet.apiimpl.enums;

import mcjty.lib.varia.ComponentFactory;
import mcjty.xnet.utils.I18nUtils;
import mcjty.xnet.utils.ITranslatableEnum;

public enum ChannelMode implements ITranslatableEnum<ChannelMode> {
    PRIORITY("xnet.enum.channelmode.priority"),
    ROUNDROBIN("xnet.enum.channelmode.roundrobin");

    private final String i18n;

    ChannelMode(String i18n) {
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
