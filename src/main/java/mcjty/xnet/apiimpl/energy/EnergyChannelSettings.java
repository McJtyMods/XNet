package mcjty.xnet.apiimpl.energy;

import com.google.gson.JsonObject;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import mcjty.lib.varia.EnergyTools;
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
import mcjty.xnet.apiimpl.enums.InsExtMode;
import mcjty.xnet.modules.cables.blocks.ConnectorBlock;
import mcjty.xnet.modules.cables.blocks.ConnectorTileEntity;
import mcjty.xnet.setup.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import org.apache.commons.lang3.tuple.Pair;


public class EnergyChannelSettings extends DefaultChannelSettings implements IChannelSettings {

    public static final Identifier iconGuiElements = Identifier.fromNamespaceAndPath(XNet.MODID, "textures/gui/guielements.png");

    public static final MapCodec<EnergyChannelSettings> CODEC = MapCodec.unit(EnergyChannelSettings::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, EnergyChannelSettings> STREAM_CODEC = StreamCodec.of(
            (buf, settings) -> {},
            buf -> new EnergyChannelSettings()
    );

    // Cache data
    private List<EnergyConnectedEntity> energyExtractors = null;
    private List<EnergyConnectedEntity> energyConsumers = null;
    private long maxConsume = 0; // Maximum RF that all consumers can accept per tick

    @Override
    public IChannelType getType() {
        return XNet.setup.energyChannelType;
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
    public int getColors() {
        return 0;
    }

    @Override
    public void tick(int channel, IControllerContext context) {
        if (!context.checkAndConsumeRF(Config.controllerOperationRFT.get())) {
            return; // Not enough energy for this operation
        }
        updateCache(channel, context);

        Level world = context.getControllerWorld();

        // First find out how much energy we have to distribute in total
        long totalToDistribute = 0;
        // Keep track of the connectors we already got energy from and how much energy we got from it
        Map<BlockPos, Integer> alreadyHandled = new HashMap<>();

        List<Pair<ConnectorTileEntity, Integer>> energyProducers = new ArrayList<>();
        for (EnergyConnectedEntity extractor : energyExtractors) {
            BlockPos connectorPos = extractor.connectorPos();

            Direction side = extractor.sidedConsumer().side();
            if (!LevelTools.isLoaded(world, extractor.getBlockPos())) {
                continue;
            }

            BlockEntity te = extractor.getConnectedEntity();
            // @todo report error somewhere?
            if (!isEnergyTE(te, side.getOpposite())) {
                continue;
            }
            EnergyConnectorSettings settings = extractor.settings();
            ConnectorTileEntity connectorTE = extractor.getConnectorEntity();

            if (!checkRedstone(settings, connectorTE, context)) {
                continue;
            }

            Integer count = settings.getMinmax();
            if (count != null) {
                int level = getEnergyLevel(te, side.getOpposite());
                if (level < count) {
                    continue;
                }
            }

            int rate = extractor.rate();
            connectorTE.setEnergyInputFrom(side, rate);

            if (!alreadyHandled.containsKey(connectorPos)) {
                // We did not handle this connector yet. Remember the amount of energy in it
                alreadyHandled.put(connectorPos, connectorTE.getEnergy());
            }

            // Check how much energy we can still send from that connector
            int connectorEnergy = alreadyHandled.get(connectorPos);
            int tosend = Math.min(rate, connectorEnergy);
            if (tosend > 0) {
                // Decrease the energy from our temporary datastructure
                alreadyHandled.put(connectorPos, connectorEnergy - tosend);
                totalToDistribute += tosend;
                energyProducers.add(Pair.of(connectorTE, tosend));
                if (totalToDistribute >= maxConsume) {
                    break; // We have enough to fill all consumers
                }
            }
        }

        if (totalToDistribute <= 0) {
            return; // Nothing to do
        }


        long actuallyConsumed = insertEnergy(context, totalToDistribute);
        if (actuallyConsumed <= 0) {
            return; // Nothing was done
        }

        // Now we need to actually fetch the energy from the producers
        for (Pair<ConnectorTileEntity, Integer> entry : energyProducers) {
            ConnectorTileEntity connectorTE = entry.getKey();
            int amount = entry.getValue();

            long actuallySpent = Math.min(amount, actuallyConsumed);
            connectorTE.setEnergy((int) (connectorTE.getEnergy() - actuallySpent));
            actuallyConsumed -= actuallySpent;
            if (actuallyConsumed <= 0) {
                break;
            }
        }
    }

    private long insertEnergy(@Nonnull IControllerContext context, long energy) {
        long total = 0;
        Level world = context.getControllerWorld();
        for (EnergyConnectedEntity consumer : energyConsumers) {
            EnergyConnectorSettings settings = consumer.settings();
            if (!LevelTools.isLoaded(world, consumer.getBlockPos())) {
                continue;
            }
            BlockEntity te = consumer.getConnectedEntity();
            // @todo report error somewhere?
            if (!isEnergyTE(te, settings.getFacing()) || !checkRedstone(settings, consumer.getConnectorEntity(), context)) {
                continue;
            }

            Integer count = settings.getMinmax();
            if (count != null) {
                int level = getEnergyLevel(te, settings.getFacing());
                if (level >= count) {
                    continue;
                }
            }

            long totransfer = Math.min(consumer.rate(), energy);
            long e = EnergyTools.receiveEnergy(te, settings.getFacing(), totransfer);
            energy -= e;
            total += e;
            if (energy <= 0) {
                return total;
            }
        }
        return total;
    }


    public static boolean isEnergyTE(@Nullable BlockEntity be, @Nonnull Direction side) {
        if (be == null) {
            return false;
        }
        return mcjty.lib.varia.CapabilityTools.energyHandler(be.getLevel().getCapability(Capabilities.Energy.BLOCK, be.getBlockPos(), side)) != null;
    }

    public static int getEnergyLevel(BlockEntity be, @Nonnull Direction side) {
        if (be != null) {
            EnergyHandler capability = mcjty.lib.varia.CapabilityTools.energyHandler(be.getLevel().getCapability(Capabilities.Energy.BLOCK, be.getBlockPos(), side));
            if (capability != null) {
                return capability.getAmountAsInt();
            }
        }
        return 0;
    }

    @Override
    public void cleanCache() {
        energyExtractors = null;
        energyConsumers = null;
        maxConsume = 0;
    }

    private void updateCache(int channel, IControllerContext context) {
        if (energyExtractors == null) {
            energyExtractors = new ArrayList<>();
            energyConsumers = new ArrayList<>();
            Map<SidedConsumer, IConnectorSettings> connectors = context.getConnectors(channel);
            Level world = context.getControllerWorld();
            for (var entry : connectors.entrySet()) {
                EnergyConnectorSettings con = (EnergyConnectorSettings) entry.getValue();
                EnergyConnectedEntity connectedBlock = getConnectedBlockInfo(context, entry, world, con);
                if (connectedBlock == null) {
                    continue;
                }
                if (con.getEnergyMode() == InsExtMode.EXT) {
                    energyExtractors.add(connectedBlock);
                } else {
                    energyConsumers.add(connectedBlock);
                    maxConsume += connectedBlock.rate();
                }
            }

            connectors = context.getRoutedConnectors(channel);
            for (var entry : connectors.entrySet()) {
                EnergyConnectorSettings con = (EnergyConnectorSettings) entry.getValue();
                EnergyConnectedEntity connectedBlock = getConnectedBlockInfo(context, entry, world, con);
                if (connectedBlock == null) {
                    continue;
                }
                if (con.getEnergyMode() == InsExtMode.INS) {
                    energyConsumers.add(connectedBlock);
                    maxConsume += connectedBlock.rate();
                }
            }

            energyExtractors.sort((o1, o2) -> o2.settings().getPriority().compareTo(o1.settings().getPriority()));
            energyConsumers.sort((o1, o2) -> o2.settings().getPriority().compareTo(o1.settings().getPriority()));
        }
    }

    @Nullable
    private EnergyConnectedEntity getConnectedBlockInfo(
            IControllerContext context, Map.Entry<SidedConsumer, IConnectorSettings> entry, @Nonnull Level world, @Nonnull EnergyConnectorSettings con
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
        Integer rate = getRateOrMax(con, connectorPos, world);
        return new EnergyConnectedEntity(entry.getKey(), con, connectorPos, connectedBlockPos, connectedEntity, connectorTileEntity, rate);
    }

    private static Integer getRateOrMax(@Nonnull EnergyConnectorSettings con, @Nonnull BlockPos connectorPos,
                                        @Nonnull Level world) {
        Integer rate = con.getRate();
        if (rate == null) {
            boolean advanced = ConnectorBlock.isAdvancedConnector(world, connectorPos);
            rate = advanced ? Config.maxRfRateAdvanced.get() : Config.maxRfRateNormal.get();
        }
        return rate;
    }

    @Override
    public boolean isEnabled(String tag) {
        return true;
    }

    @Nullable
    @Override
    public IndicatorIcon getIndicatorIcon() {
        return new IndicatorIcon(iconGuiElements, 11, 80, 11, 10);
    }

    @Nullable
    @Override
    public String getIndicator() {
        return null;
    }

    @Override
    public void createGui(IEditorGui gui) {
    }

    @Override
    public void update(Map<String, Object> data) {
    }
}
