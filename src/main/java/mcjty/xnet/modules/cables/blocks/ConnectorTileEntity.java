package mcjty.xnet.modules.cables.blocks;

import mcjty.lib.api.container.DefaultContainerProvider;
import mcjty.lib.bindings.GuiValue;
import mcjty.lib.bindings.Value;
import mcjty.lib.blockcommands.Command;
import mcjty.lib.blockcommands.ResultCommand;
import mcjty.lib.blockcommands.ServerCommand;
import mcjty.lib.container.GenericContainer;
import mcjty.lib.tileentity.GenericTileEntity;
import mcjty.lib.typed.Key;
import mcjty.lib.typed.Type;
import mcjty.lib.typed.TypedMap;
import mcjty.lib.varia.OrientationTools;
import mcjty.rftoolsbase.api.xnet.tiles.IConnectorTile;
import mcjty.xnet.modules.cables.CableModule;
import mcjty.xnet.modules.cables.data.ConnectorData;
import mcjty.xnet.modules.facade.FacadeModule;
import mcjty.xnet.modules.facade.IFacadeSupport;
import mcjty.xnet.modules.facade.MimicBlockSupport;
import mcjty.xnet.modules.facade.data.MimicData;
import mcjty.xnet.multiblock.WorldBlob;
import mcjty.xnet.multiblock.XNetBlobData;
import mcjty.xnet.setup.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import java.util.function.Function;

import static mcjty.lib.api.container.DefaultContainerProvider.empty;
import static mcjty.xnet.apiimpl.Constants.TAG_ENABLED;
import static mcjty.xnet.apiimpl.Constants.TAG_FACING;
import static mcjty.xnet.apiimpl.Constants.TAG_INFO;
import static mcjty.xnet.apiimpl.Constants.TAG_NAME;
import static mcjty.xnet.modules.cables.CableModule.TYPE_CONNECTOR;
import static mcjty.xnet.utils.I18nConstants.BLOCK_CONNECTOR;

public class ConnectorTileEntity extends GenericTileEntity implements IFacadeSupport, IConnectorTile {

    private int energy = 0;
    private int[] inputFromSide = new int[] { 0, 0, 0, 0, 0, 0 };

    // Count the number of redstone pulses we got
    private int pulseCounter;
    private final int[] powerOut = new int[] { 0, 0, 0, 0, 0, 0 };

    private final Lazy<SidedHandler>[] sidedStorages;

    private final Block[] cachedNeighbours = new Block[OrientationTools.DIRECTION_VALUES.length];

    @GuiValue
    public static final Value<?, String> VALUE_NAME = Value.create("name", Type.STRING, ConnectorTileEntity::getConnectorName, ConnectorTileEntity::setConnectorName);

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level.isClientSide() && getMimicBlock() != null) {
            level.setBlockAndUpdate(worldPosition, getBlockState());
        }
    }

    public static final Function<ConnectorTileEntity, MenuProvider> SCREEN_CAP = be -> new DefaultContainerProvider<GenericContainer>(BLOCK_CONNECTOR.i18n())
            .containerSupplier(empty(CableModule.CONTAINER_CONNECTOR, be));

    public ConnectorTileEntity(BlockPos pos, BlockState state) {
        this(TYPE_CONNECTOR.get(), pos, state);
    }

    protected ConnectorTileEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        sidedStorages = new Lazy[OrientationTools.DIRECTION_VALUES.length];
        for (Direction direction : OrientationTools.DIRECTION_VALUES) {
            sidedStorages[direction.ordinal()] = Lazy.of(() -> createSidedHandler(direction));
        }
    }

    @Override
    public void onDataPacket(Connection net, net.minecraft.world.level.storage.ValueInput input) {
        super.onDataPacket(net, input);

        if (level.isClientSide()) {
            // Dynamic models read the synchronized block entity when rebuilding the section.
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    public int getPowerOut(Direction side) {
        return powerOut[side.ordinal()];
    }

    public void setPowerOut(Direction side, int powerOut) {
        if (powerOut > 15) {
            powerOut = 15;
        }
        if (this.powerOut[side.ordinal()] == powerOut) {
            return;
        }
        this.powerOut[side.ordinal()] = powerOut;
        setChanged();
        level.neighborChanged(worldPosition.relative(side), this.getBlockState().getBlock(), null);
    }

    public void setEnabled(Direction direction, boolean e) {
        ConnectorData data = getData(CableModule.CONNECTOR_DATA);
        byte enabled = data.enabled();
        if (e) {
            enabled |= 1 << direction.ordinal();
        } else {
            enabled &= ~(1 << direction.ordinal());
        }
        setData(CableModule.CONNECTOR_DATA, data.withEnabled(enabled));
        Block block = getBlockState().getBlock();
        if (block instanceof GenericCableBlock) {
            level.setBlock(worldPosition, ((GenericCableBlock) block).calculateState(level, worldPosition, getBlockState()), Block.UPDATE_ALL);
        }
    }

    public boolean isEnabled(Direction direction) {
        ConnectorData data = getData(CableModule.CONNECTOR_DATA);
        byte enabled = data.enabled();
        return (enabled & (1 << direction.ordinal())) != 0;
    }

    @Override
    public BlockState getMimicBlock() {
        // Packet serialization must not create attachments and queue another chunk update.
        MimicData data = getExistingDataOrNull(FacadeModule.MIMIC_DATA.get());
        BlockState state = data == null ? null : data.state();
        if (state == null || state.isAir()) {
            return null;
        }
        return state;
    }

    public void setMimicBlock(BlockState mimicBlock) {
        if (mimicBlock == null) {
            mimicBlock = Blocks.AIR.defaultBlockState();
        }
        setData(FacadeModule.MIMIC_DATA, new MimicData(mimicBlock));
        markDirtyClient();
    }

    @Override
    public void setPowerInput(int powered) {
        if (powerLevel == 0 && powered > 0) {
            pulseCounter++;
        }
        super.setPowerInput(powered);
    }

    @Override
    public int getPulseCounter() {
        return pulseCounter;
    }

    // Optimization to only increase the network if there is an actual block change
    public void possiblyMarkNetworkDirty(@Nonnull BlockPos neighbor) {
        for (Direction facing : OrientationTools.DIRECTION_VALUES) {
            if (getBlockPos().relative(facing).equals(neighbor)) {
                Block newblock = level.getBlockState(neighbor).getBlock();
                if (newblock != cachedNeighbours[facing.ordinal()]) {
                    cachedNeighbours[facing.ordinal()] = newblock;
                    WorldBlob worldBlob = XNetBlobData.get(level).getWorldBlob(level);
                    worldBlob.markNetworkDirty(worldBlob.getNetworkAt(getBlockPos()));
                }
                return;
            }
        }
    }

    @Override
    public void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        CompoundTag tagCompound = input.read(com.mojang.serialization.MapCodec.assumeMapUnsafe(CompoundTag.CODEC)).orElseGet(CompoundTag::new);
        HolderLookup.Provider provider = input.lookup();
        energy = tagCompound.getIntOr("energy", 0);
        inputFromSide = tagCompound.getIntArray("inputs").orElseGet(() -> new int[6]);
        if (inputFromSide.length != 6) {
            inputFromSide = new int[] { 0, 0, 0, 0, 0, 0 };
        }
        pulseCounter = tagCompound.getIntOr("pulse", 0);
        for (int i = 0 ; i < 6 ; i++) {
            powerOut[i] = tagCompound.getByteOr("p" + i, (byte) 0);
        }
    }

    @Override
    public void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tagCompound = new CompoundTag();
        HolderLookup.Provider provider = (level != null ? level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY);
        tagCompound.putInt("energy", energy);
        tagCompound.putIntArray("inputs", inputFromSide);
        tagCompound.putInt("pulse", pulseCounter);
        for (int i = 0 ; i < 6 ; i++) {
            tagCompound.putByte("p" + i, (byte) powerOut[i]);
        }

        output.store(com.mojang.serialization.MapCodec.assumeMapUnsafe(CompoundTag.CODEC), tagCompound);
    }

    @Override
    protected void applyImplicitComponents(net.minecraft.core.component.DataComponentGetter input) {
        super.applyImplicitComponents(input);
        var data = input.get(CableModule.ITEM_CONNECTOR_DATA);
        if (data != null) {
            setData(CableModule.CONNECTOR_DATA, data);
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(CableModule.ITEM_CONNECTOR_DATA, getData(CableModule.CONNECTOR_DATA));
    }

    @Override
    public void saveClientDataToNBT(CompoundTag tagCompound, HolderLookup.Provider provider) {
        MimicBlockSupport.writeToNBT(tagCompound, getMimicBlock());
    }

    @Override
    public void loadClientDataFromNBT(CompoundTag tagCompound, HolderLookup.Provider provider) {
        BlockState state = MimicBlockSupport.readFromNBT(provider, tagCompound);
        if (state == null) {
            state = Blocks.AIR.defaultBlockState();
        }
        setData(FacadeModule.MIMIC_DATA, new MimicData(state));
    }

    public void setConnectorName(String n) {
        ConnectorData data = getData(CableModule.CONNECTOR_DATA);
        data = data.withName(n);
        setData(CableModule.CONNECTOR_DATA, data);
    }


    public String getConnectorName() {
        ConnectorData data = getData(CableModule.CONNECTOR_DATA);
        return data.name();
    }

    public int getEnergy() {
        return energy;
    }

    public void setEnergy(int energy) {
        if (this.energy != energy) {
            if (energy < 0) {
                energy = 0;
            }
            this.energy = energy;
            markDirtyQuick();
        }
    }

    public void setEnergyInputFrom(Direction from, int rate) {
        if (inputFromSide[from.ordinal()] != rate) {
            inputFromSide[from.ordinal()] = rate;
            markDirtyQuick();
        }
    }

    public int getMaxEnergy() {
        return Config.maxRfConnector.get();
    }

    private int receiveEnergyInternal(Direction from, int maxReceive, boolean simulate) {
        if (from == null) {
            return 0;
        }
        int m = inputFromSide[from.ordinal()];
        if (m > 0) {
            int toreceive = Math.min(maxReceive, m);
            int newenergy = energy + toreceive;
            if (newenergy > getMaxEnergy()) {
                toreceive -= newenergy - getMaxEnergy();
                newenergy = getMaxEnergy();
            }
            if (!simulate && energy != newenergy) {
                energy = newenergy;
                inputFromSide[from.ordinal()] = 0;
                markDirtyQuick();
            }
            return toreceive;
        }
        return 0;
    }

    private int getEnergyStoredInternal() {
        return energy;
    }

    private int getMaxEnergyStoredInternal() {
        return getMaxEnergy();
    }


    public static final Key<Integer> PARAM_FACING = new Key<>(TAG_FACING, Type.INTEGER);
    public static final Key<Boolean> PARAM_ENABLED = new Key<>(TAG_ENABLED, Type.BOOLEAN);
    public static final Key<String> PARAM_NAME = new Key<>(TAG_NAME, Type.STRING);

    @ServerCommand
    public static final Command<?> CMD_ENABLE = Command.<ConnectorTileEntity>create("connector.enable",
            (te, playerEntity, params) -> {
                int f = params.get(PARAM_FACING);
                boolean e = params.get(PARAM_ENABLED);
                te.setEnabled(OrientationTools.DIRECTION_VALUES[f], e);
            });
    @ServerCommand
    public static final ResultCommand<?> CMD_GET_NAME = ResultCommand.<ConnectorTileEntity>create("xnet.connector.name",
            (te, player, params) -> TypedMap.builder().put(PARAM_NAME, te.getConnectorName()).build(),
            (te, player, params) -> te.setConnectorName(params.get(PARAM_NAME)));


    private record EnergySnapshot(int energy, int[] inputs) { }
    private final mcjty.rftoolsbase.tools.EnergyTransactions.Journal<EnergySnapshot> energyJournal =
            new mcjty.rftoolsbase.tools.EnergyTransactions.Journal<>(
                    () -> new EnergySnapshot(energy, inputFromSide.clone()), snapshot -> {
                        energy = snapshot.energy();
                        inputFromSide = snapshot.inputs().clone();
                    });

    public net.neoforged.neoforge.transfer.energy.EnergyHandler getEnergyHandler(Direction side) {
        return mcjty.rftoolsbase.tools.EnergyTransactions.input(getEnergyStorage(side), () -> java.util.List.of(energyJournal));
    }

    public IEnergyStorage getEnergyStorage(Direction facing) {
        if (facing == null) {
            return null;
        } else {
            return sidedStorages[facing.ordinal()].get();
        }
    }

    private SidedHandler createSidedHandler(Direction facing) {
        return new SidedHandler(facing);
    }

    class SidedHandler implements IEnergyStorage {

        private final Direction facing;

        public SidedHandler(Direction facing) {
            this.facing = facing;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return ConnectorTileEntity.this.receiveEnergyInternal(facing, maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return ConnectorTileEntity.this.getEnergyStoredInternal();
        }

        @Override
        public int getMaxEnergyStored() {
            return ConnectorTileEntity.this.getMaxEnergyStoredInternal();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }

}
