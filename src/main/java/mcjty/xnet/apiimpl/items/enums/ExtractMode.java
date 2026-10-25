package mcjty.xnet.apiimpl.items.enums;

import com.mojang.serialization.Codec;
import mcjty.lib.gui.ITranslatableEnum;
import mcjty.lib.varia.ComponentFactory;
import mcjty.xnet.utils.I18nUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import mcjty.lib.network.StreamCodecs;

public enum ExtractMode implements ITranslatableEnum<ExtractMode>, StringRepresentable {
    FIRST("xnet.enum.items.extractmode.first"),
    RND("xnet.enum.items.extractmode.rnd"),
    ORDER("xnet.enum.items.extractmode.order");
    private final String i18n;

    public static final Codec<ExtractMode> CODEC = StringRepresentable.fromEnum(ExtractMode::values);
    public static final StreamCodec<FriendlyByteBuf, ExtractMode> STREAM_CODEC = StreamCodecs.enumCodec(ExtractMode.class);

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

    @Override
    public String getSerializedName() {
        return name();
    }
}
