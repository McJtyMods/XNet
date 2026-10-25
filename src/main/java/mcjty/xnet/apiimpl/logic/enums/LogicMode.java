package mcjty.xnet.apiimpl.logic.enums;


import com.mojang.serialization.Codec;
import mcjty.lib.gui.ITranslatableEnum;
import mcjty.lib.varia.ComponentFactory;
import mcjty.xnet.utils.I18nUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import mcjty.lib.network.StreamCodecs;


public enum LogicMode implements ITranslatableEnum<LogicMode>, StringRepresentable {
    SENSOR( "xnet.enum.logic.logicmode.sensor"),
    OUTPUT("xnet.enum.logic.logicmode.output");

    public static final Codec<LogicMode> CODEC = StringRepresentable.fromEnum(LogicMode::values);
    public static final StreamCodec<FriendlyByteBuf, LogicMode> STREAM_CODEC = StreamCodecs.enumCodec(LogicMode.class);

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

    @Override
    public String getSerializedName() {
        return name();
    }
}
