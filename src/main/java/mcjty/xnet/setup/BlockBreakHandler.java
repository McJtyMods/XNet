package mcjty.xnet.setup;

import mcjty.xnet.modules.cables.blocks.ConnectorTileEntity;
import mcjty.xnet.modules.facade.FacadeModule;
import mcjty.xnet.modules.facade.blocks.FacadeBlock;
import mcjty.xnet.modules.facade.blocks.FacadeBlockItem;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** Removing a facade leaves its cable or connector and network in place. */
public final class BlockBreakHandler {
    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, be) -> {
            if (!(world instanceof net.minecraft.server.level.ServerLevel serverLevel)
                    || !(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) return true;
            if (state.getBlock() instanceof FacadeBlock facade) {
                if (!player.isCreative()) facade.playerDestroy(serverLevel, serverPlayer, pos, state, be, player.getMainHandItem());
                facade.replaceWithCable(world, pos, state);
                return false;
            }
            if (be instanceof ConnectorTileEntity connector && connector.getMimicBlock() != null) {
                if (!player.isCreative()) {
                    var item = new ItemStack(FacadeModule.FACADE_ITEM.get());
                    FacadeBlockItem.setMimicBlock(item, connector.getMimicBlock());
                    Block.popResource(world, pos, item);
                }
                connector.setMimicBlock(null);
                return false;
            }
            return true;
        });
    }
}
