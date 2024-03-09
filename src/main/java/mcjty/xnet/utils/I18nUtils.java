package mcjty.xnet.utils;

import org.apache.commons.lang3.StringUtils;

import static mcjty.lib.varia.ComponentFactory.translatable;
import static mcjty.xnet.apiimpl.Constants.TAG_TOOLTIP;

public class I18nUtils {

    public static String[] getSplitedTooltip(String i18n) {
        return StringUtils.split(i18n, '|');
    }

    public static String[] getSplitedEnumTooltip(String i18n) {
        return StringUtils.split(translatable(i18n + "." + TAG_TOOLTIP).getString(), '|');
    }
}
