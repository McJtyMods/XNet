package mcjty.xnet.apiimpl.items.enums;

import com.mojang.serialization.Codec;
import mcjty.lib.gui.ITranslatableEnum;
import mcjty.lib.varia.ComponentFactory;
import mcjty.xnet.utils.I18nUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import mcjty.lib.network.StreamCodecs;

public enum StackMode implements ITranslatableEnum<StackMode>, StringRepresentable {
    SINGLE("xnet.enum.items.stackmode.single"),
    STACK("xnet.enum.items.stackmode.stack"),
    COUNT("xnet.enum.items.stackmode.count");

    private final String i18n;

    public static final Codec<StackMode> CODEC = StringRepresentable.fromEnum(StackMode::values);
    public static final StreamCodec<FriendlyByteBuf, StackMode> STREAM_CODEC = StreamCodecs.enumCodec(StackMode.class);

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

    @Override
    public String getSerializedName() {
        return name();
    }
}
