package mcjty.xnet.apiimpl.fluids;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import mcjty.lib.varia.LevelTools;
import mcjty.rftoolsbase.api.xnet.channels.IChannelSettings;
import mcjty.rftoolsbase.api.xnet.channels.IConnectorSettings;
import mcjty.rftoolsbase.api.xnet.channels.IControllerContext;
import mcjty.rftoolsbase.api.xnet.gui.IEditorGui;
import mcjty.rftoolsbase.api.xnet.gui.IndicatorIcon;
import mcjty.rftoolsbase.api.xnet.helper.DefaultChannelSettings;
import mcjty.rftoolsbase.api.xnet.keys.SidedConsumer;
import mcjty.xnet.XNet;
import mcjty.xnet.apiimpl.EnumStringTranslators;
import mcjty.xnet.setup.Config;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class FluidChannelSettings extends DefaultChannelSettings implements IChannelSettings {

    public static final ResourceLocation iconGuiElements = new ResourceLocation(XNet.MODID, "textures/gui/guielements.png");

    public static final String TAG_MODE = "mode";

    public enum ChannelMode {
        PRIORITY,
        DISTRIBUTE
    }

    private ChannelMode channelMode = ChannelMode.DISTRIBUTE;
    private int delay = 0;
    private int roundRobinOffset = 0;

    // Cache data
    private Map<SidedConsumer, FluidConnectorSettings> fluidExtractors = null;
    private List<Pair<SidedConsumer, FluidConnectorSettings>> fluidConsumers = null;

    public ChannelMode getChannelMode() {
        return channelMode;
    }

    @Override
    public JsonObject writeToJson() {
        JsonObject object = new JsonObject();
        object.add("mode", new JsonPrimitive(channelMode.name()));
        return object;
    }

    @Override
    public void readFromJson(JsonObject data) {
        channelMode = EnumStringTranslators.getFluidChannelMode(data.get("mode").getAsString());
    }


    @Override
    public void readFromNBT(CompoundNBT tag) {
        channelMode = ChannelMode.values()[tag.getByte("mode")];
        delay = tag.getInt("delay");
        roundRobinOffset = tag.getInt("offset");
    }

    @Override
    public void writeToNBT(CompoundNBT tag) {
        tag.putByte("mode", (byte) channelMode.ordinal());
        tag.putInt("delay", delay);
        tag.putInt("offset", roundRobinOffset);
    }

    @Override
    public void tick(int channel, IControllerContext context) {
        delay--;
        if (delay <= 0) {
            delay = 200*6;      // Multiply of the different speeds we have
        }
        if (delay % 10 != 0) {
            return;
        }
        int d = delay/10;

        updateCache(channel, context);
        // @todo optimize
        World world = context.getControllerWorld();
        for (Map.Entry<SidedConsumer, FluidConnectorSettings> entry : fluidExtractors.entrySet()) {
            FluidConnectorSettings settings = entry.getValue();
            if (d % settings.getSpeed() != 0) {
                continue;
            }

            BlockPos extractorPos = context.findConsumerPosition(entry.getKey().getConsumerId());
            if (extractorPos != null) {
                Direction side = entry.getKey().getSide();
                BlockPos pos = extractorPos.relative(side);
                if (!LevelTools.isLoaded(world, pos)) {
                    continue;
                }

                TileEntity te = world.getBlockEntity(pos);
                // @todo ugly code!
                IFluidHandler handler = getFluidHandlerAt(te, settings.getFacing()).map(h -> h).orElse(null);
                // @todo report error somewhere?
                if (handler != null) {
                    if (checkRedstone(world, settings, extractorPos)) {
                        continue;
                    }
                    if (!context.matchColor(settings.getColorsMask())) {
                        continue;
                    }

                    FluidStack extractMatcher = settings.getMatcher();
                    int bandwidth = settings.getRate();
                    Integer count = settings.getMinmax();
                    if (count != null) {
                        // count amount of dedicated fluid if extractMatcher is null, and all fluids otherwise
                        int amount = countFluid(handler, extractMatcher);
                        int canextract = amount-count;
                        bandwidth = Math.min(bandwidth, canextract);
                    }
                    if (bandwidth <= 0) {
                        continue;
                    }

                    Iterable<FluidStack> iter;
                    if (extractMatcher != null) {
                        iter = Collections.singleton(extractMatcher);
                    } else {
                        ArrayList<FluidStack> list = new ArrayList<>(handler.getTanks() + 1);
                        // try default behavior first, then move onto iteration
                        list.add(fetchFluid(handler, true, null, bandwidth));
                        for (int i = 0; i < handler.getTanks(); i++) {
                            list.add(handler.getFluidInTank(i));
                        }
                        iter = list;
                    }

                    fluidLoop:
                    for (FluidStack fluid : iter) {
                        if (fluid.isEmpty() || bandwidth <= 0) continue;
                        List<Pair<SidedConsumer, FluidConnectorSettings>> inserted = new ArrayList<>();
                        int toextract = bandwidth;
                        int remaining;
                        do {
                            // Imagine the pathological case where we're extracting from a container that works in 13mB
                            // increments and inserting into a container that works in 17mB increments. We should end up
                            // with toextract = 884 at the end of this loop, given that it started at 1000.
                            FluidStack stack = fetchFluid(handler, true, fluid, toextract);
                            if (stack.isEmpty()) {
                                continue fluidLoop;
                            }
                            toextract = stack.getAmount();
                            inserted.clear();
                            remaining = insertFluidSimulate(inserted, context, stack);
                            toextract -= remaining;
                            if (inserted.isEmpty() || toextract <= 0) {
                                continue fluidLoop;
                            }
                        } while(remaining > 0);

                        if (context.checkAndConsumeRF(Config.controllerOperationRFT.get())) {
                            FluidStack stack = fetchFluid(handler, false, fluid, toextract);
                            bandwidth -= stack.getAmount();
                            if (stack.isEmpty()) {
                                throw new NullPointerException(handler.getClass().getName() + " misbehaved! handler.drain(" + toextract + ", true) returned null, even though handler.drain(" + toextract + ", false) did not");
                            }
                            insertFluidReal(context, inserted, stack);
                        }
                    }
                }
            }
        }

    }


    @Override
    public void cleanCache() {
        fluidExtractors = null;
        fluidConsumers = null;
    }

    @Nonnull
    private FluidStack fetchFluid(IFluidHandler handler, boolean simulate, @Nullable FluidStack fluid, int rate) {
        IFluidHandler.FluidAction action = simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE;
        if (fluid == null) {
            return handler.drain(rate, action);
        } else {
            fluid = fluid.copy();
            fluid.setAmount(rate);
            return handler.drain(fluid, action);
        }
    }

    // Returns what could not be filled
    private int insertFluidSimulate(@Nonnull List<Pair<SidedConsumer, FluidConnectorSettings>> inserted, @Nonnull IControllerContext context, @Nonnull FluidStack stack) {
        World world = context.getControllerWorld();
        if (channelMode == ChannelMode.PRIORITY) {
            roundRobinOffset = 0;       // Always start at 0
        }
        int amount = stack.getAmount();
        for (int j = 0 ; j < fluidConsumers.size() ; j++) {
            int i = (j + roundRobinOffset)  % fluidConsumers.size();
            Pair<SidedConsumer, FluidConnectorSettings> entry = fluidConsumers.get(i);
            FluidConnectorSettings settings = entry.getValue();

            if (settings.getMatcher() == null || settings.getMatcher().equals(stack)) {
                BlockPos consumerPos = context.findConsumerPosition(entry.getKey().getConsumerId());
                if (consumerPos != null) {
                    if (!LevelTools.isLoaded(world, consumerPos)) {
                        continue;
                    }
                    if (checkRedstone(world, settings, consumerPos)) {
                        continue;
                    }
                    if (!context.matchColor(settings.getColorsMask())) {
                        continue;
                    }

                    Direction side = entry.getKey().getSide();
                    BlockPos pos = consumerPos.relative(side);
                    TileEntity te = world.getBlockEntity(pos);
                    // @todo ugly code!
                    IFluidHandler handler = getFluidHandlerAt(te, settings.getFacing()).map(h -> h).orElse(null);
                    // @todo report error somewhere?
                    if (handler != null) {
                        int toinsert = Math.min(settings.getRate(), amount);

                        Integer count = settings.getMinmax();
                        if (count != null) {
                            int a = countFluid(handler, settings.getMatcher());
                            int caninsert = count-a;
                            if (caninsert <= 0) {
                                continue;
                            }
                            toinsert = Math.min(toinsert, caninsert);
                        }

                        FluidStack copy = stack.copy();
                        copy.setAmount(toinsert);

                        int filled = handler.fill(copy, IFluidHandler.FluidAction.SIMULATE);
                        if (filled > 0) {
                            inserted.add(entry);
                            amount -= filled;
                            if (amount <= 0) {
                                return 0;
                            }
                        }
                    }
                }
            }
        }
        return amount;
    }

    private int countFluid(IFluidHandler handler, @Nullable FluidStack matcher) {
        int cnt = 0;
        for (int i = 0 ; i < handler.getTanks() ; i++) {
            if (!handler.getFluidInTank(i).isEmpty() && (matcher == null || matcher.equals(handler.getFluidInTank(i)))) {
                cnt += handler.getFluidInTank(i).getAmount();
            }
        }
        return cnt;
    }


    private void insertFluidReal(@Nonnull IControllerContext context, @Nonnull List<Pair<SidedConsumer, FluidConnectorSettings>> inserted, @Nonnull FluidStack stack) {
        int amount = stack.getAmount();
        for (Pair<SidedConsumer, FluidConnectorSettings> pair : inserted) {
            BlockPos consumerPosition = context.findConsumerPosition(pair.getKey().getConsumerId());
            Direction side = pair.getKey().getSide();
            FluidConnectorSettings settings = pair.getValue();
            BlockPos pos = consumerPosition.relative(side);
            TileEntity te = context.getControllerWorld().getBlockEntity(pos);
            // @todo ugly code!
            IFluidHandler handler = getFluidHandlerAt(te, settings.getFacing()).map(h -> h).orElse(null);

            int toinsert = Math.min(settings.getRate(), amount);

            Integer count = settings.getMinmax();
            if (count != null) {
                int a = countFluid(handler, settings.getMatcher());
                int caninsert = count-a;
                if (caninsert <= 0) {
                    continue;
                }
                toinsert = Math.min(toinsert, caninsert);
            }

            FluidStack copy = stack.copy();
            copy.setAmount(toinsert);

            int filled = handler.fill(copy, IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) {
                roundRobinOffset = (roundRobinOffset+1) % fluidConsumers.size();
                amount -= filled;
                if (amount <= 0) {
                    return;
                }
            }
        }
    }



    private void updateCache(int channel, IControllerContext context) {
        if (fluidExtractors == null) {
            fluidExtractors = new HashMap<>();
            fluidConsumers = new ArrayList<>();
            Map<SidedConsumer, IConnectorSettings> connectors = context.getConnectors(channel);
            for (Map.Entry<SidedConsumer, IConnectorSettings> entry : connectors.entrySet()) {
                FluidConnectorSettings con = (FluidConnectorSettings) entry.getValue();
                if (con.getFluidMode() == FluidConnectorSettings.FluidMode.EXT) {
                    fluidExtractors.put(entry.getKey(), con);
                } else {
                    fluidConsumers.add(Pair.of(entry.getKey(), con));
                }
            }

            connectors = context.getRoutedConnectors(channel);
            for (Map.Entry<SidedConsumer, IConnectorSettings> entry : connectors.entrySet()) {
                FluidConnectorSettings con = (FluidConnectorSettings) entry.getValue();
                if (con.getFluidMode() == FluidConnectorSettings.FluidMode.INS) {
                    fluidConsumers.add(Pair.of(entry.getKey(), con));
                }
            }

            fluidConsumers.sort((o1, o2) -> o2.getRight().getPriority().compareTo(o1.getRight().getPriority()));
        }
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
        gui.nl().choices(TAG_MODE, "Fluid distribution mode", channelMode, ChannelMode.values());
    }

    @Override
    public void update(Map<String, Object> data) {
        channelMode = ChannelMode.valueOf(((String)data.get(TAG_MODE)).toUpperCase());
    }

    @Override
    public int getColors() {
        return 0;
    }

    @Nonnull
    public static LazyOptional<IFluidHandler> getFluidHandlerAt(@Nullable TileEntity te, Direction intSide) {
        if (te != null) {
            return te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, intSide);
        } else {
            return LazyOptional.empty();
        }
    }
}


