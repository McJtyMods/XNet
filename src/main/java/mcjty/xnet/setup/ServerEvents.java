package mcjty.xnet.setup;

import mcjty.xnet.multiblock.XNetWirelessChannels;
import net.minecraft.world.level.Level;

public class ServerEvents {

    public void register() { net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.START_LEVEL_TICK.register(this::onWorldTick); }

    private static final int AMOUNT = 10;

    private int cnt = AMOUNT;

    public void onWorldTick(net.minecraft.server.level.ServerLevel level) {
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
