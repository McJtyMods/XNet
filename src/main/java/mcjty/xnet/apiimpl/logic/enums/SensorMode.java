package mcjty.xnet.apiimpl.logic.enums;

import com.mojang.serialization.Codec;
import mcjty.lib.gui.ITranslatableEnum;
import mcjty.lib.varia.ComponentFactory;
import mcjty.xnet.utils.I18nUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import mcjty.lib.network.StreamCodecs;

public enum SensorMode implements ITranslatableEnum<SensorMode>, StringRepresentable {
    OFF("xnet.enum.logic.sensormode.off"),
    ITEM("xnet.enum.logic.sensormode.item"),
    FLUID("xnet.enum.logic.sensormode.fluid"),
    ENERGY("xnet.enum.logic.sensormode.energy"),
    RS("xnet.enum.logic.sensormode.rs");

    private final String i18n;

    public static final Codec<SensorMode> CODEC = StringRepresentable.fromEnum(SensorMode::values);
    public static final StreamCodec<FriendlyByteBuf, SensorMode> STREAM_CODEC = StreamCodecs.enumCodec(SensorMode.class);

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

    @Override
    public String getSerializedName() {
        return name();
    }
}
