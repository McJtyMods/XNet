package mcjty.xnet.utils;

public interface ITranslatableEnum<E extends Enum<E>> {

    String name();
    int ordinal();
    String toString();

    String getI18n();
    String[] getI18nSplitedTooltip();
}
