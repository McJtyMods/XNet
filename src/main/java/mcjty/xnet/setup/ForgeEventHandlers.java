package mcjty.xnet.setup;

import mcjty.xnet.multiblock.XNetWirelessChannels;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

public class ForgeEventHandlers {

    private static final int AMOUNT = 10;

    private int cnt = AMOUNT;

    @SubscribeEvent
    public void onWorldTick(LevelTickEvent.Pre event) {
        if (!event.getLevel().isClientSide() && event.getLevel().dimension().equals(Level.OVERWORLD)) {
            cnt--;
            if (cnt > 0) {
                return;
            }
            cnt = AMOUNT;

            XNetWirelessChannels data = XNetWirelessChannels.get(event.getLevel());
            data.tick(event.getLevel(), AMOUNT);
        }
    }
}
