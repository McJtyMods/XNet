package mcjty.xnet.setup;

import mcjty.xnet.multiblock.XNetBlobData;
import mcjty.xnet.multiblock.XNetWirelessChannels;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.Level;

public class ServerEvents {

    private static final int AMOUNT = 10;

    private int cnt = AMOUNT;

    public void register() {
        ServerTickEvents.START_LEVEL_TICK.register(this::onWorldTick);
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> onChunkAvailabilityChanged(level, chunk));
        ServerChunkEvents.CHUNK_UNLOAD.register(this::onChunkAvailabilityChanged);
    }

    private void onChunkAvailabilityChanged(ServerLevel level, LevelChunk chunk) {
        XNetBlobData.onChunkAvailabilityChanged(level, chunk.getPos());
    }

    public void onWorldTick(ServerLevel level) {
        if (!level.isClientSide() && level.dimension().equals(Level.OVERWORLD)) {
            cnt--;
            if (cnt > 0) {
                return;
            }
            cnt = AMOUNT;

            XNetWirelessChannels data = XNetWirelessChannels.get(level);
            data.tick(level, AMOUNT);
        }
    }
}
