package mcjty.xnet.apiimpl.enums;

import com.mojang.serialization.Codec;
import mcjty.lib.gui.ITranslatableEnum;
import mcjty.lib.varia.ComponentFactory;
import mcjty.xnet.utils.I18nUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import mcjty.lib.network.StreamCodecs;

public enum ChannelMode implements ITranslatableEnum<ChannelMode>, StringRepresentable {
    PRIORITY("xnet.enum.channelmode.priority"),
    ROUNDROBIN("xnet.enum.channelmode.roundrobin");

    public static final Codec<ChannelMode> CODEC = StringRepresentable.fromEnum(ChannelMode::values);
    public static final StreamCodec<FriendlyByteBuf, ChannelMode> STREAM_CODEC = StreamCodecs.enumCodec(ChannelMode.class);

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


    @Override
    public String getSerializedName() {
        return name();
    }
}
