package mcjty.xnet.apiimpl.logic;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mcjty.lib.varia.LevelTools;
import mcjty.rftoolsbase.api.xnet.channels.IChannelSettings;
import mcjty.rftoolsbase.api.xnet.channels.IChannelType;
import mcjty.rftoolsbase.api.xnet.channels.IConnectorSettings;
import mcjty.rftoolsbase.api.xnet.channels.IControllerContext;
import mcjty.rftoolsbase.api.xnet.gui.IEditorGui;
import mcjty.rftoolsbase.api.xnet.gui.IndicatorIcon;
import mcjty.rftoolsbase.api.xnet.helper.DefaultChannelSettings;
import mcjty.rftoolsbase.api.xnet.keys.SidedConsumer;
import mcjty.xnet.XNet;
import mcjty.xnet.apiimpl.ConnectedBlock;
import mcjty.xnet.apiimpl.ConnectedEntity;
import mcjty.xnet.apiimpl.logic.enums.LogicMode;
import mcjty.xnet.logic.LogicOperations;
import mcjty.xnet.logic.LogicTools;
import mcjty.xnet.modules.cables.blocks.ConnectorTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static mcjty.xnet.apiimpl.Constants.TAG_COLORS;

public class LogicChannelSettings extends DefaultChannelSettings implements IChannelSettings {

    public static final Identifier iconGuiElements = Identifier.fromNamespaceAndPath(XNet.MODID, "textures/gui/guielements.png");
    private int colors = 0;     // Colors for this channel
    private List<ConnectedOptionalEntity<LogicConnectorSettings>> sensors = null;
    private List<ConnectedBlock<LogicConnectorSettings>> outputs = null;

    public static final MapCodec<LogicChannelSettings> CODEC = MapCodec.unit(LogicChannelSettings::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, LogicChannelSettings> STREAM_CODEC = StreamCodec.of(
            (buf, settings) -> { },
            buf -> new LogicChannelSettings()
    );

    @Override
    public IChannelType getType() {
        return XNet.setup.logicChannelType;
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
        colors = tag.getIntOr(TAG_COLORS, 0);
    }

    @Override
    public void writeToNBT(CompoundTag tag) {
        tag.putInt(TAG_COLORS, colors);
    }

    @Override
    public int getColors() {
        return colors;
    }

    @Override
    public void tick(int channel, IControllerContext context) {
        updateCache(channel, context);
        Level world = context.getControllerWorld();

        colors = 0;
        for (ConnectedOptionalEntity<LogicConnectorSettings> connectedBlock : sensors) {
            LogicConnectorSettings settings = connectedBlock.settings();
            int sensorColors = 0;
            BlockPos pos = connectedBlock.getBlockPos();
            if (!LevelTools.isLoaded(world, pos)) {
                // If it is not chunkloaded we just use the color settings as we last remembered it
                colors |= settings.getColorMask();
                continue;
            }

            // If checkRedstone is false the sensor is disabled which means the colors from it will also be disabled
            if (checkRedstone(settings, connectedBlock.getConnectorEntity(), context)) {
                BlockEntity te = connectedBlock.getConnectedEntity();

                for (RSSensor sensor : settings.getSensors()) {
                    if (sensor.test(te, world, pos, settings)) {
                        sensorColors |= 1 << sensor.getOutputColor().ordinal();
                    }
                }
            }
            settings.setColorMask(sensorColors);
            colors |= sensorColors;
        }

        for (ConnectedBlock<LogicConnectorSettings> connector : outputs) {
            LogicConnectorSettings settings = connector.settings();

            BlockPos connectorPos = connector.connectorPos();
            if (!LevelTools.isLoaded(world, connectorPos)) {
                continue;
            }

            Direction side = connector.sidedConsumer().side();
            ConnectorTileEntity connectorTileEntity = connector.getConnectorEntity();
            int powerOut;
            if (checkRedstone(settings, connectorTileEntity, context)) {
                RSOutput output = settings.getOutput();
                boolean[] colorsArray = LogicTools.intToBinary(colors);
                boolean input1 = colorsArray[output.getInputChannel1().ordinal()];
                boolean input2 = colorsArray[output.getInputChannel2().ordinal()];
                powerOut = LogicOperations.applyFilter(output, input1, input2) ? output.getRedstoneOut() : 0;
            } else {
                powerOut = 0;
            }
            connectorTileEntity.setPowerOut(side, powerOut);
        }
    }

    private void updateCache(int channel, IControllerContext context) {
        if (sensors == null) {
            sensors = new ArrayList<>();
            outputs = new ArrayList<>();
            Level world = context.getControllerWorld();
            Map<SidedConsumer, IConnectorSettings> connectors = context.getConnectors(channel);
            for (Map.Entry<SidedConsumer, IConnectorSettings> entry : connectors.entrySet()) {
                LogicConnectorSettings con = (LogicConnectorSettings) entry.getValue();
                ConnectedBlock<LogicConnectorSettings> connectedBlock;
                connectedBlock = getConnectedBlockInfo(context, entry, world, con);
                if (connectedBlock == null) {
                    continue;
                }
                if (con.getLogicMode() == LogicMode.SENSOR) {
                    ConnectedOptionalEntity<LogicConnectorSettings> connectedEntity = getConnectedEntity(connectedBlock, world);
                    sensors.add(connectedEntity);
                } else {
                    outputs.add(connectedBlock);
                }
            }

            connectors = context.getRoutedConnectors(channel);
            for (Map.Entry<SidedConsumer, IConnectorSettings> entry : connectors.entrySet()) {
                LogicConnectorSettings con = (LogicConnectorSettings) entry.getValue();
                if (con.getLogicMode() == LogicMode.OUTPUT) {
                    ConnectedBlock<LogicConnectorSettings> connectedBlock;
                    connectedBlock = getConnectedBlockInfo(context, entry, world, con);
                    if (connectedBlock == null) {
                        continue;
                    }
                    outputs.add(connectedBlock);
                }
            }
        }
    }

    @Nonnull
    private ConnectedOptionalEntity<LogicConnectorSettings> getConnectedEntity(
            @Nonnull ConnectedBlock<LogicConnectorSettings> connectedBlock, @Nonnull Level world
    ) {
        BlockEntity connectedEntity = world.getBlockEntity(connectedBlock.getBlockPos());
        return new ConnectedOptionalEntity<>(connectedBlock, connectedEntity);
    }

    @Nullable
    private ConnectedBlock<LogicConnectorSettings> getConnectedBlockInfo(
            IControllerContext context, Map.Entry<SidedConsumer, IConnectorSettings> entry, @Nonnull Level world, @Nonnull LogicConnectorSettings con
    ) {
        BlockPos connectorPos = context.findConsumerPosition(entry.getKey().consumerId());
        if (connectorPos == null) {
            return null;
        }
        ConnectorTileEntity connectorEntity = (ConnectorTileEntity) world.getBlockEntity(connectorPos);
        if (connectorEntity == null) {
            return null;
        }
        BlockPos connectedBlockPos = connectorPos.relative(entry.getKey().side());
        BlockEntity connectedEntity = world.getBlockEntity(connectedBlockPos);
        if (connectedEntity == null) {
            return new ConnectedBlock<>(entry.getKey(), con, connectorPos, connectedBlockPos, connectorEntity);
        }
        return new ConnectedEntity<>(entry.getKey(), con, connectorPos, connectedBlockPos, connectedEntity, connectorEntity);
    }

    @Override
    public void cleanCache() {
        sensors = null;
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
    public void createGui(IEditorGui gui) {

    }

    @Override
    public void update(Map<String, Object> data) {

    }
}
