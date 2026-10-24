package mcjty.xnet.apiimpl.none;

import com.google.gson.JsonObject;
import com.mojang.serialization.MapCodec;
import mcjty.rftoolsbase.api.xnet.channels.IChannelType;
import mcjty.rftoolsbase.api.xnet.gui.IEditorGui;
import mcjty.rftoolsbase.api.xnet.gui.IndicatorIcon;
import mcjty.rftoolsbase.api.xnet.helper.AbstractConnectorSettings;
import mcjty.xnet.XNet;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class NoneConnectorSettings extends AbstractConnectorSettings {

    public static final Identifier iconGuiElements = Identifier.fromNamespaceAndPath(XNet.MODID, "textures/gui/guielements.png");

    public static final NoneConnectorSettings EMPTY = new NoneConnectorSettings(Direction.NORTH);
    public static final MapCodec<NoneConnectorSettings> CODEC = MapCodec.unit(EMPTY);
    public static final StreamCodec<RegistryFriendlyByteBuf, NoneConnectorSettings> STREAM_CODEC = StreamCodec.unit(EMPTY);

    private NoneConnectorSettings(@Nonnull Direction side) {
        super(DEFAULT_SETTINGS, side);
    }

    @Override
    public IChannelType getType() {
        return XNet.setup.noneChannelType;
    }

    @Nullable
    @Override
    public IndicatorIcon getIndicatorIcon() {
        return new IndicatorIcon(iconGuiElements, 26, 70, 13, 10);
    }

    @Nullable
    @Override
    public String getIndicator() {
        return null;
    }

    @Override
    public boolean isEnabled(String tag) {
        return false;
    }

    @Override
    public void createGui(IEditorGui gui) {
    }

    @Override
    public JsonObject writeToJson() {
        return new JsonObject();
    }

}
