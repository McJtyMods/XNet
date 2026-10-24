package mcjty.xnet.apiimpl.none;

import com.google.gson.JsonObject;
import com.mojang.serialization.MapCodec;
import mcjty.rftoolsbase.api.xnet.channels.IChannelSettings;
import mcjty.rftoolsbase.api.xnet.channels.IChannelType;
import mcjty.rftoolsbase.api.xnet.channels.IControllerContext;
import mcjty.rftoolsbase.api.xnet.gui.IEditorGui;
import mcjty.rftoolsbase.api.xnet.gui.IndicatorIcon;
import mcjty.rftoolsbase.api.xnet.helper.DefaultChannelSettings;
import mcjty.xnet.XNet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;
import java.util.Map;

public class NoneChannelSettings extends DefaultChannelSettings implements IChannelSettings {

    public static final Identifier iconGuiElements = Identifier.fromNamespaceAndPath(XNet.MODID, "textures/gui/guielements.png");

    public static final NoneChannelSettings EMPTY = new NoneChannelSettings();
    public static final StreamCodec<RegistryFriendlyByteBuf, NoneChannelSettings> STREAM_CODEC = StreamCodec.unit(EMPTY);
    public static final MapCodec<NoneChannelSettings> CODEC = MapCodec.unit(EMPTY);

    private NoneChannelSettings() {
    }

    @Override
    public IChannelType getType() {
        return XNet.setup.noneChannelType;
    }

    @Override
    public JsonObject writeToJson() {
        return new JsonObject();
    }

    @Override
    public void readFromJson(JsonObject data) {
    }


    @Override
    public void readFromNBT(CompoundTag tag) {
    }

    @Override
    public void writeToNBT(CompoundTag tag) {
    }

    @Override
    public void tick(int channel, IControllerContext context) {
    }

    @Override
    public void cleanCache() {
    }

    @Nullable
    @Override
    public IndicatorIcon getIndicatorIcon() {
        return new IndicatorIcon(iconGuiElements, 11, 90, 11, 10);
    }

    @Nullable
    @Override
    public String getIndicator() {
        return null;
    }

    @Override
    public boolean isEnabled(String tag) {
        return true;
    }

    @Override
    public int getColors() {
        return 0;
    }

    @Override
    public void createGui(IEditorGui gui) {
    }

    @Override
    public void update(Map<String, Object> data) {

    }
}
