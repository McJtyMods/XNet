package mcjty.xnet.multiblock;

import mcjty.lib.varia.OrientationTools;
import mcjty.rftoolsbase.api.xnet.keys.ConsumerId;
import mcjty.rftoolsbase.api.xnet.keys.NetworkId;
import mcjty.rftoolsbase.api.xnet.net.IWorldBlob;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static mcjty.xnet.apiimpl.Constants.TAG_CHUNKS;
import static mcjty.xnet.apiimpl.Constants.TAG_CHUNK_X;
import static mcjty.xnet.apiimpl.Constants.TAG_CHUNK_Z;
import static mcjty.xnet.apiimpl.Constants.TAG_LAST_CONSUMER;
import static mcjty.xnet.apiimpl.Constants.TAG_LAST_NETWORK;

public class WorldBlob implements IWorldBlob {

    private final ResourceKey<Level> dimensionType;
    private final Map<Long, ChunkBlob> chunkBlobMap = new HashMap<>();
    private int lastNetworkId = 0;              // Network ID
    private int lastConsumerId = 0;             // Network consumer ID

    // All consumers (as position) for a given network. If an entry in this map does not
    // exist for a certain network that means the information has to be calculated
    private final Map<NetworkId, Set<BlockPos>> consumersOnNetwork = new HashMap<>();

    // For every network we maintain a version number. If something on a network changes
    // this increases and so things that depend on network topology can detect this change
    // and do the needed updates
    private final Map<NetworkId, VersionNumber> networkVersions = new HashMap<>();

    // Transient map containing all consumers and their position
    private final Map<ConsumerId, BlockPos> consumerPositions = new HashMap<>();

    // Transient map containing all providers and their position
    private final Map<NetworkId, BlockPos> providerPositions = new HashMap<>();

    public WorldBlob(ResourceKey<Level> dimensionType) {
        this.dimensionType = dimensionType;
    }

    public ResourceKey<Level> getDimensionType() {
        return dimensionType;
    }

    @Nonnull
    public NetworkId newNetwork() {
        lastNetworkId++;
        return new NetworkId(lastNetworkId);
    }

    @Nonnull
    public ConsumerId newConsumer() {
        lastConsumerId++;
        return new ConsumerId(lastConsumerId);
    }

    @Nullable
    public BlobId getBlobAt(@Nonnull BlockPos pos) {
        ChunkBlob blob = getBlob(pos);
        if (blob == null) {
            return null;
        }
        IntPos intPos = new IntPos(pos);
        return blob.getBlobIdForPosition(intPos);
    }

    @Override
    @Nonnull
    public Set<NetworkId> getNetworksAt(@Nonnull BlockPos pos) {
        ChunkBlob blob = getBlob(pos);
        if (blob == null) {
            return Collections.emptySet();
        }
        IntPos intPos = new IntPos(pos);
        return blob.getNetworksForPosition(intPos);
    }

    @Nullable
    public NetworkId getNetworkAt(@Nonnull BlockPos pos) {
        Set<NetworkId> networks = getNetworksAt(pos);
        if (networks.isEmpty()) {
            return null;
        }
        return networks.iterator().next();
    }

    @Override
    @Nullable
    public ConsumerId getConsumerAt(@Nonnull BlockPos pos) {
        ChunkBlob blob = getBlob(pos);
        if (blob == null) {
            return null;
        }
        IntPos intPos = new IntPos(pos);
        return blob.getNetworkConsumers().get(intPos);
    }

    @Nullable
    public ColorId getColorAt(@Nonnull BlockPos pos) {
        ChunkBlob blob = getBlob(pos);
        if (blob == null) {
            return null;
        }
        IntPos intPos = new IntPos(pos);
        return blob.getColorIdForPosition(intPos);
    }

    // Find the position of the network provider
    @Override
    @Nullable
    public BlockPos getProviderPosition(@Nonnull NetworkId networkId) {
        if (!providerPositions.containsKey(networkId)) {
            // @todo avoid scanning all blobs?
            providerPositions.put(networkId, null);
            for (ChunkBlob blob : chunkBlobMap.values()) {
                IntPos intPos = blob.getProviderPosition(networkId);
                if (intPos != null) {
                    providerPositions.put(networkId, blob.getPosition(intPos));
                    break;
                }
            }
        }
        return providerPositions.get(networkId);
    }

    @Override
    @Nonnull
    public Set<BlockPos> getConsumers(NetworkId network) {
        if (!consumersOnNetwork.containsKey(network)) {
            Set<BlockPos> positions = new HashSet<>();

            // @todo can this be done more optimal instead of traversing all the chunk blobs?
            for (ChunkBlob blob : chunkBlobMap.values()) {
                Set<IntPos> consumersForNetwork = blob.getConsumersForNetwork(network);
                for (IntPos intPos : consumersForNetwork) {
                    BlockPos pos = blob.getPosition(intPos);
                    positions.add(pos);
                }
            }
            consumersOnNetwork.put(network, positions);
        }
        return consumersOnNetwork.get(network);
    }

    private void removeCachedNetworksForBlob(ChunkBlob blob) {
        for (NetworkId id : blob.getNetworks()) {
            consumersOnNetwork.remove(id);
            markNetworkDirty(id);
        }
    }

    @Override
    public void markNetworkDirty(NetworkId id) {
        if (!networkVersions.containsKey(id)) {
            networkVersions.put(id, new VersionNumber(1));
        }
        networkVersions.get(id).inc();
    }

    public int getNetworkVersion(NetworkId id) {
        if (!networkVersions.containsKey(id)) {
            return 0;
        } else {
            return networkVersions.get(id).getVersion();
        }
    }

    @Override
    @Nullable
    public BlockPos getConsumerPosition(@Nonnull ConsumerId consumer) {
        if (!consumerPositions.containsKey(consumer)) {
            // @todo avoid scanning all blobs?
            consumerPositions.put(consumer, null);
            for (ChunkBlob blob : chunkBlobMap.values()) {
                IntPos intPos = blob.getConsumerPosition(consumer);
                if (intPos != null) {
                    consumerPositions.put(consumer, blob.getPosition(intPos));
                    break;
                }
            }
        }
        return consumerPositions.get(consumer);
    }

    /**
     * Create a cable segment that is also a network provider at this section
     */
    public void createNetworkProvider(BlockPos pos, ColorId color, NetworkId network) {
        providerPositions.remove(network);
        ChunkBlob blob = getOrCreateBlob(pos);
        blob.createNetworkProvider(pos, color, network);
        recalculateNetwork(blob);
    }

    /**
     * Create a cable segment that is also a network consumer at this section
     */
    public void createNetworkConsumer(BlockPos pos, ColorId color, ConsumerId consumer) {
        ChunkBlob blob = getOrCreateBlob(pos);
        blob.createNetworkConsumer(pos, color, consumer);
        recalculateNetwork(blob);
        consumerPositions.remove(consumer);
    }

    /**
     * Create a cable segment at a position
     */
    public void createCableSegment(BlockPos pos, ColorId color) {
        ChunkBlob blob = getOrCreateBlob(pos);
        blob.createCableSegment(pos, color);
        recalculateNetwork(blob);
    }

    @Nonnull
    private ChunkBlob getOrCreateBlob(BlockPos pos) {
        ChunkPos cpos = ChunkPos.containing(pos);
        long chunkId = ChunkPos.pack(cpos.x(), cpos.z());
        if (!chunkBlobMap.containsKey(chunkId)) {
            chunkBlobMap.put(chunkId, new ChunkBlob(cpos));
        }
        return chunkBlobMap.get(chunkId);
    }

    @Nullable
    private ChunkBlob getBlob(BlockPos pos) {
        ChunkPos cpos = ChunkPos.containing(pos);
        long chunkId = ChunkPos.pack(cpos.x(), cpos.z());
        return chunkBlobMap.get(chunkId);
    }

    public void removeCableSegment(BlockPos pos) {
        ConsumerId consumerId = getConsumerAt(pos);
        if (consumerId != null) {
            consumerPositions.remove(consumerId);
        }
        NetworkId providerId = getNetworkAt(pos);
        if (providerId != null) {
            providerPositions.remove(providerId);
        }
        ChunkBlob blob = getOrCreateBlob(pos);
        blob.removeCableSegment(pos);
        recalculateNetwork();
    }

    /**
     * Recalculate the network starting from the given block
     */
    public void recalculateNetwork(ChunkBlob blob) {
        removeCachedNetworksForBlob(blob);
        blob.fixNetworkAllocations();
        removeCachedNetworksForBlob(blob);

        Set<ChunkBlob> todo = new HashSet<>();
        Set<ChunkBlob> recalculated = new HashSet<>();  // Keep track of which chunks we already recalculated
        recalculated.add(blob);
        todo.add(blob);
        recalculateNetwork(todo, recalculated);
    }

    /**
     * Recalculate the entire network
     */
    public void recalculateNetwork() {
        // First make sure that every chunk has its network mappings correct (mapping
        // from blob id to network id). Note that this will discard all networking
        // information from neighbouring chunks. recalculateNetwork() should fix those.
        for (ChunkBlob blob : chunkBlobMap.values()) {
            blob.fixNetworkAllocations();
            removeCachedNetworksForBlob(blob);
        }

        // For every chunk we check all border positions and see where they connect with
        // adjacent chunks
        Set<ChunkBlob> todo = new HashSet<>(chunkBlobMap.values());
        recalculateNetwork(todo, null);
    }

    private void recalculateNetwork(@Nonnull Set<ChunkBlob> todo, @Nullable Set<ChunkBlob> recalculated) {
        while (!todo.isEmpty()) {
            ChunkBlob blob = todo.iterator().next();
            todo.remove(blob);
            if (recalculated != null) {
                if (!recalculated.contains(blob)) {
                    blob.fixNetworkAllocations();
                    recalculated.add(blob);
                }
            }
            blob.clearNetworkCache();
            removeCachedNetworksForBlob(blob);


            Set<IntPos> borderPositions = blob.getBorderPositions();
            ChunkPos chunkPos = blob.getChunkPos();
            for (IntPos pos : borderPositions) {
                Set<NetworkId> networks = blob.getOrCreateNetworksForPosition(pos);
                ColorId color = blob.getColorIdForPosition(pos);

                for (Direction facing : OrientationTools.HORIZONTAL_DIRECTION_VALUES) {
                    if (pos.isBorder(facing)) {
                        Vec3i vec = facing.getUnitVec3i();
                        ChunkBlob adjacent = chunkBlobMap.get(
                                ChunkPos.pack(chunkPos.x()+vec.getX(), chunkPos.z()+vec.getZ()));
                        if (adjacent != null) {
                            IntPos connectedPos = pos.otherSide(facing);
                            if (adjacent.getBorderPositions().contains(connectedPos) && adjacent.getColorIdForPosition(connectedPos).equals(color)) {
                                // We have a connection!
                                Set<NetworkId> adjacentNetworks = adjacent.getOrCreateNetworksForPosition(connectedPos);
                                if (networks.addAll(adjacentNetworks)) {
                                    todo.add(blob);     // We changed this blob so need to push back on todo
                                }
                                if (adjacentNetworks.addAll(networks)) {
                                    todo.add(adjacent);
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    public void checkNetwork(Level world) {
        for (Map.Entry<Long, ChunkBlob> entry : chunkBlobMap.entrySet()) {
            entry.getValue().check(world);
        }
    }

    private void dump(String prefix, Set<NetworkId> networks) {
        String s = prefix + ": ";
        for (NetworkId network : networks) {
            s += network.id() + " ";
        }
        System.out.println("s = " + s);
    }


    public void dump() {
        for (ChunkBlob blob : chunkBlobMap.values()) {
            blob.dump();
        }
    }


    public void readFromNBT(CompoundTag compound) {
        chunkBlobMap.clear();
        lastNetworkId = compound.getIntOr(TAG_LAST_NETWORK, 0);
        lastConsumerId = compound.getIntOr(TAG_LAST_CONSUMER, 0);
        if (compound.contains(TAG_CHUNKS)) {
            ListTag chunks = (ListTag) compound.get(TAG_CHUNKS);
            for (net.minecraft.nbt.Tag chunk : chunks) {
                CompoundTag tc = (CompoundTag) chunk;
                int chunkX = tc.getIntOr(TAG_CHUNK_X, 0);
                int chunkZ = tc.getIntOr(TAG_CHUNK_Z, 0);
                ChunkBlob blob = new ChunkBlob(new ChunkPos(chunkX, chunkZ));
                blob.readFromNBT(tc);
                chunkBlobMap.put(blob.getChunkNum(), blob);
            }
        }
    }

    public CompoundTag writeToNBT(CompoundTag compound) {
        compound.putInt(TAG_LAST_NETWORK, lastNetworkId);
        compound.putInt(TAG_LAST_CONSUMER, lastConsumerId);
        ListTag list = new ListTag();
        for (Map.Entry<Long, ChunkBlob> entry : chunkBlobMap.entrySet()) {
            ChunkBlob blob = entry.getValue();
            CompoundTag tc = new CompoundTag();
            tc.putInt(TAG_CHUNK_X, blob.getChunkPos().x());
            tc.putInt(TAG_CHUNK_Z, blob.getChunkPos().z());
            blob.writeToNBT(tc);
            list.add(tc);
        }
        compound.put(TAG_CHUNKS, list);

        return compound;
    }

}
