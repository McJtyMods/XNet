package mcjty.xnet.apiimpl.energy;

import com.google.gson.JsonObject;
import mcjty.lib.varia.EnergyTools;
import mcjty.lib.varia.LevelTools;
import mcjty.rftoolsbase.api.xnet.channels.IChannelSettings;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnergyChannelSettings extends DefaultChannelSettings implements IChannelSettings {

    public static final ResourceLocation iconGuiElements = new ResourceLocation(XNet.MODID, "textures/gui/guielements.png");

    // Cache data
    private List<Pair<SidedConsumer, EnergyConnectorSettings>> energyExtractors = null;
    private List<Pair<SidedConsumer, EnergyConnectorSettings>> energyConsumers = null;

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
        updateCache(channel, context);

        Level world = context.getControllerWorld();

        // First find out how much energy we have to distribute in total
        int totalToDistribute = 0;
        // Keep track of the connectors we already got energy from and how much energy we
        // got from it
        Map<BlockPos, Integer> alreadyHandled = new HashMap<>();

        List<Pair<ConnectorTileEntity, Integer>> energyProducers = new ArrayList<>();
        for (Pair<SidedConsumer, EnergyConnectorSettings> entry : energyExtractors) {
            BlockPos connectorPos = context.findConsumerPosition(entry.getKey().consumerId());
            if (connectorPos != null) {

                Direction side = entry.getKey().side();
                BlockPos energyPos = connectorPos.relative(side);
                if (!LevelTools.isLoaded(world, energyPos)) {
                    continue;
                }

                BlockEntity te = world.getBlockEntity(energyPos);
                // @todo report error somewhere?
                if (isEnergyTE(te, side.getOpposite())) {
                    EnergyConnectorSettings settings = entry.getValue();
                    ConnectorTileEntity connectorTE = (ConnectorTileEntity) world.getBlockEntity(connectorPos);

                    if (checkRedstone(world, settings, connectorPos)) {
                        continue;
                    }
                    if (!context.matchColor(settings.getColorsMask())) {
                        continue;
                    }

                    Integer count = settings.getMinmax();
                    if (count != null) {
                        int level = getEnergyLevel(te, side.getOpposite());
                        if (level < count) {
                            continue;
                        }
                    }

                    Integer rate = settings.getRate();
                    if (rate == null) {
                        boolean advanced = ConnectorBlock.isAdvancedConnector(world, connectorPos);
                        rate = advanced ? Config.maxRfRateAdvanced.get() : Config.maxRfRateNormal.get();
                    }
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
                    }
                }
            }
        }

        if (totalToDistribute <= 0) {
            // Nothing to do
            return;
        }

        if (!context.checkAndConsumeRF(Config.controllerOperationRFT.get())) {
            // Not enough energy for this operation
            return;
        }

        int actuallyConsumed = insertEnergy(context, totalToDistribute);
        if (actuallyConsumed <= 0) {
            // Nothing was done
            return;
        }

        // Now we need to actually fetch the energy from the producers
        for (Pair<ConnectorTileEntity, Integer> entry : energyProducers) {
            ConnectorTileEntity connectorTE = entry.getKey();
            int amount = entry.getValue();

            int actuallySpent = Math.min(amount, actuallyConsumed);
            connectorTE.setEnergy(connectorTE.getEnergy() - actuallySpent);
            actuallyConsumed -= actuallySpent;
            if (actuallyConsumed <= 0) {
                break;
            }
        }
    }

    private int insertEnergy(@Nonnull IControllerContext context, int energy) {
        int total = 0;
        Level world = context.getControllerWorld();
        for (Pair<SidedConsumer, EnergyConnectorSettings> entry : energyConsumers) {
            EnergyConnectorSettings settings = entry.getValue();
            BlockPos extractorPos = context.findConsumerPosition(entry.getKey().consumerId());
            if (extractorPos != null) {
                Direction side = entry.getKey().side();
                BlockPos pos = extractorPos.relative(side);
                if (!LevelTools.isLoaded(world, pos)) {
                    continue;
                }
                BlockEntity te = world.getBlockEntity(pos);
                // @todo report error somewhere?
                if (isEnergyTE(te, settings.getFacing())) {

                    if (checkRedstone(world, settings, extractorPos)) {
                        continue;
                    }
                    if (!context.matchColor(settings.getColorsMask())) {
                        continue;
                    }

                    Integer count = settings.getMinmax();
                    if (count != null) {
                        int level = getEnergyLevel(te, settings.getFacing());
                        if (level >= count) {
                            continue;
                        }
                    }

                    Integer rate = settings.getRate();
                    if (rate == null) {
                        boolean advanced = ConnectorBlock.isAdvancedConnector(world, extractorPos);
                        rate = advanced ? Config.maxRfRateAdvanced.get() : Config.maxRfRateNormal.get();
                    }
                    int totransfer = Math.min(rate, energy);
                    long e = EnergyTools.receiveEnergy(te, settings.getFacing(), totransfer);
                    energy -= e;
                    total += e;
                    if (energy <= 0) {
                        return total;
                    }
                }
            }
        }
        return total;
    }


    public static boolean isEnergyTE(@Nullable BlockEntity te, @Nonnull Direction side) {
        if (te == null) {
            return false;
        }
        return te.getCapability(ForgeCapabilities.ENERGY, side).isPresent();
    }

    public static int getEnergyLevel(BlockEntity tileEntity, @Nonnull Direction side) {
        if (tileEntity != null) {
            return tileEntity.getCapability(ForgeCapabilities.ENERGY, side).map(IEnergyStorage::getEnergyStored).orElse(0);
        } else {
            return 0;
        }
    }



    @Override
    public void cleanCache() {
        energyExtractors = null;
        energyConsumers = null;
    }

    private void updateCache(int channel, IControllerContext context) {
        if (energyExtractors == null) {
            energyExtractors = new ArrayList<>();
            energyConsumers = new ArrayList<>();
            Map<SidedConsumer, IConnectorSettings> connectors = context.getConnectors(channel);
            for (var entry : connectors.entrySet()) {
                EnergyConnectorSettings con = (EnergyConnectorSettings) entry.getValue();
                if (con.getEnergyMode() == InsExtMode.EXT) {
                    energyExtractors.add(Pair.of(entry.getKey(), con));
                } else {
                    energyConsumers.add(Pair.of(entry.getKey(), con));
                }
            }

            connectors = context.getRoutedConnectors(channel);
            for (var entry : connectors.entrySet()) {
                EnergyConnectorSettings con = (EnergyConnectorSettings) entry.getValue();
                if (con.getEnergyMode() == InsExtMode.INS) {
                    energyConsumers.add(Pair.of(entry.getKey(), con));
                }
            }

            energyExtractors.sort((o1, o2) -> o2.getRight().getPriority().compareTo(o1.getRight().getPriority()));
            energyConsumers.sort((o1, o2) -> o2.getRight().getPriority().compareTo(o1.getRight().getPriority()));
        }
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
