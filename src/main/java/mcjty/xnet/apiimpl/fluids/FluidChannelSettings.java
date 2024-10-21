package mcjty.xnet.apiimpl.fluids;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
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
import mcjty.xnet.apiimpl.ConnectedEntity;
import mcjty.xnet.apiimpl.EnumStringTranslators;
import mcjty.xnet.apiimpl.enums.ChannelMode;
import mcjty.xnet.apiimpl.enums.InsExtMode;
import mcjty.xnet.modules.cables.blocks.ConnectorTileEntity;
import mcjty.xnet.setup.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static mcjty.xnet.apiimpl.Constants.TAG_MODE;

public class FluidChannelSettings extends DefaultChannelSettings implements IChannelSettings {

    public static final ResourceLocation iconGuiElements = ResourceLocation.fromNamespaceAndPath(XNet.MODID, "textures/gui/guielements.png");
    private ChannelMode channelMode = ChannelMode.ROUNDROBIN;
    private int delay = 0;
    private int roundRobinOffset = 0;

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidChannelSettings> STREAM_CODEC = StreamCodec.composite(
            ChannelMode.STREAM_CODEC, FluidChannelSettings::getChannelMode,
            FluidChannelSettings::new
    );
    public static final MapCodec<FluidChannelSettings> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ChannelMode.CODEC.fieldOf("mode").forGetter(FluidChannelSettings::getChannelMode)
    ).apply(instance, FluidChannelSettings::new));

    public FluidChannelSettings() {
    }

    public FluidChannelSettings(ChannelMode channelMode) {
        this.channelMode = channelMode;
    }

    // Cache data
    private List<ConnectedEntity<FluidConnectorSettings>> fluidExtractors = null;
    private List<ConnectedEntity<FluidConnectorSettings>> fluidConsumers = null;

    public ChannelMode getChannelMode() {
        return channelMode;
    }

    @Override
    public IChannelType getType() {
        return XNet.setup.fluidChannelType;
    }

    @Override
    public JsonObject writeToJson() {
        JsonObject object = new JsonObject();
        object.add(TAG_MODE, new JsonPrimitive(channelMode.name()));
        return object;
    }

    @Override
    public void readFromJson(JsonObject data) {
        channelMode = EnumStringTranslators.getFluidChannelMode(data.get(TAG_MODE).getAsString());
    }


    @Override
    public void readFromNBT(CompoundTag tag) {
        delay = tag.getInt("delay");
        roundRobinOffset = tag.getInt("offset");
    }

    @Override
    public void writeToNBT(CompoundTag tag) {
        tag.putInt("delay", delay);
        tag.putInt("offset", roundRobinOffset);
    }

    @Override
    public void tick(int channel, IControllerContext context) {
        delay--;
        if (delay <= 0) {
            delay = 200 * 6;      // Multiply of the different speeds we have
        }
        if (delay % 10 != 0) {
            return;
        }
        int d = delay / 10;

        updateCache(channel, context);
        Level world = context.getControllerWorld();
        for (ConnectedEntity<FluidConnectorSettings> extractor : fluidExtractors) {
            FluidConnectorSettings settings = extractor.settings();
            if (d % settings.getSpeed() != 0) {
                continue;
            }

            if (!LevelTools.isLoaded(world, extractor.getBlockPos())) {
                continue;
            }
            if (!checkRedstone(settings, extractor.getConnectorEntity(), context)) {
                continue;
            }

            IFluidHandler handler = getFluidHandlerAt(extractor.getConnectedEntity(), extractor.settings().getFacing());
            if (handler == null) {
                continue;
            }
            tickFluidHandler(context, settings, handler);
        }

    }

    private void tickFluidHandler(IControllerContext context, FluidConnectorSettings settings, IFluidHandler handler) {
        if (!context.checkAndConsumeRF(Config.controllerOperationRFT.get())) {
            return;
        }
        FluidStack extractMatcher = settings.getMatcher();

        int toextract = settings.getRate();

        Integer count = settings.getMinmax();
        if (count != null) {
            int amount = countFluid(handler, extractMatcher);
            int canextract = amount-count;
            if (canextract <= 0) {
                return;
            }
            toextract = Math.min(toextract, canextract);
        }

        while (true) {
            FluidStack stack = fetchFluid(handler, true, extractMatcher, toextract);
            if (stack.isEmpty()) {
                return;
            }
            toextract = stack.getAmount();
            int remaining = insertFluid(context, stack);
            toextract -= remaining;
            if (remaining != toextract) {
                fetchFluid(handler, false, extractMatcher, toextract);
                return;
            }
        }
    }


    @Override
    public void cleanCache() {
        fluidExtractors = null;
        fluidConsumers = null;
    }

    @Nonnull
    private FluidStack fetchFluid(IFluidHandler handler, boolean simulate, @Nullable FluidStack matcher, int rate) {
        IFluidHandler.FluidAction action = simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE;
        if (matcher == null || matcher.isEmpty()) {
            return handler.drain(rate, action);
        } else {
            matcher = matcher.copy();
            matcher.setAmount(rate);
            return handler.drain(matcher, action);
        }
    }

    private int insertFluid(@Nonnull IControllerContext context, @Nonnull FluidStack stack) {
        Level world = context.getControllerWorld();
        if (channelMode == ChannelMode.PRIORITY) {
            roundRobinOffset = 0;       // Always start at 0
        }
        int amount = stack.getAmount();
        for (int j = 0 ; j < fluidConsumers.size() ; j++) {
            int i = (j + roundRobinOffset)  % fluidConsumers.size();
            ConnectedEntity<FluidConnectorSettings> consumer = fluidConsumers.get(i);
            FluidConnectorSettings settings = consumer.settings();
            if (!LevelTools.isLoaded(world, consumer.getBlockPos())) {
                continue;
            }
            IFluidHandler destination = getFluidHandlerAt(consumer.getConnectedEntity(), consumer.settings().getFacing());
            if (destination == null) {
                continue;
            }
            FluidStack matcher = settings.getMatcher();
            if (matcher != null && !matcher.equals(stack)) {
                continue;
            }
            if (!checkRedstone(settings, consumer.getConnectorEntity(), context)) {
                continue;
            }

            // @todo ugly code!
            int toinsert = Math.min(settings.getRate(), amount);

            Integer count = settings.getMinmax();
            if (count != null) {
                int a = countFluid(destination, settings.getMatcher());
                int caninsert = count - a;
                if (caninsert <= 0) {
                    continue;
                }
                toinsert = Math.min(toinsert, caninsert);
            }

            FluidStack copy = stack.copy();
            copy.setAmount(toinsert);

            int filled = destination.fill(copy, IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) {
                roundRobinOffset = (roundRobinOffset + 1) % fluidConsumers.size();
                amount -= filled;
                if (amount <= 0) {
                    return 0;
                }
            }
        }
        return amount;
    }

    private int countFluid(IFluidHandler handler, @Nullable FluidStack matcher) {
        int cnt = 0;
        for (int i = 0; i < handler.getTanks(); i++) {
            if (!handler.getFluidInTank(i).isEmpty() && (matcher == null || matcher.equals(handler.getFluidInTank(i)))) {
                cnt += handler.getFluidInTank(i).getAmount();
            }
        }
        return cnt;
    }

    private void updateCache(int channel, IControllerContext context) {
        if (fluidExtractors == null) {
            fluidExtractors = new ArrayList<>();
            fluidConsumers = new ArrayList<>();
            Level world = context.getControllerWorld();
            Map<SidedConsumer, IConnectorSettings> connectors = context.getConnectors(channel);
            for (var entry : connectors.entrySet()) {
                FluidConnectorSettings con = (FluidConnectorSettings) entry.getValue();
                ConnectedEntity<FluidConnectorSettings> connectedEntity;
                connectedEntity = getConnectedEntityInfo(context, entry, world, con);
                if (connectedEntity == null) {
                    continue;
                }
                if (con.getFluidMode() == InsExtMode.EXT) {
                    fluidExtractors.add(connectedEntity);
                } else {
                    fluidConsumers.add(connectedEntity);
                }
            }

            connectors = context.getRoutedConnectors(channel);
            for (var entry : connectors.entrySet()) {
                FluidConnectorSettings con = (FluidConnectorSettings) entry.getValue();
                if (con.getFluidMode() == InsExtMode.INS) {
                    ConnectedEntity<FluidConnectorSettings> connectedEntity;
                    connectedEntity = getConnectedEntityInfo(context, entry, world, con);
                    if (connectedEntity == null) {
                        continue;
                    }
                    fluidConsumers.add(connectedEntity);
                }
            }

            fluidConsumers.sort((o1, o2) -> o2.settings().getPriority().compareTo(o1.settings().getPriority()));
        }
    }

    @Nullable
    private ConnectedEntity<FluidConnectorSettings> getConnectedEntityInfo(
            IControllerContext context, Map.Entry<SidedConsumer, IConnectorSettings> entry, @Nonnull Level world, @Nonnull FluidConnectorSettings con
    ) {
        BlockPos connectorPos = context.findConsumerPosition(entry.getKey().consumerId());
        if (connectorPos == null) {
            return null;
        }
        ConnectorTileEntity connectorTileEntity = (ConnectorTileEntity) world.getBlockEntity(connectorPos);
        if (connectorTileEntity == null) {
            return null;
        }
        BlockPos connectedBlockPos = connectorPos.relative(entry.getKey().side());
        BlockEntity connectedEntity = world.getBlockEntity(connectedBlockPos);
        if (connectedEntity == null) {
            return null;
        }

        return  new ConnectedEntity<>(entry.getKey(), con, connectorPos, connectedBlockPos, connectedEntity, connectorTileEntity);
    }

    @Override
    public boolean isEnabled(String tag) {
        return true;
    }

    @Nullable
    @Override
    public IndicatorIcon getIndicatorIcon() {
        return new IndicatorIcon(iconGuiElements, 22, 80, 11, 10);
    }

    @Nullable
    @Override
    public String getIndicator() {
        return null;
    }

    @Override
    public void createGui(IEditorGui gui) {
        gui.nl();
        gui.translatableChoices(TAG_MODE, channelMode, ChannelMode.values());
    }

    @Override
    public void update(Map<String, Object> data) {
        channelMode = ChannelMode.valueOf(((String) data.get(TAG_MODE)).toUpperCase());
    }

    @Override
    public int getColors() {
        return 0;
    }

    @Nullable
    public static IFluidHandler getFluidHandlerAt(@Nullable BlockEntity te, Direction intSide) {
        if (te != null) {
            return te.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, te.getBlockPos(), intSide);
        } else {
            return null;
        }
    }
}
