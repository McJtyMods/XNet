package mcjty.xnet.modules.cables.client;

import mcjty.xnet.modules.cables.blocks.GenericCableBlock;
import mcjty.xnet.modules.facade.blocks.FacadeBlock;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Installs world-aware cable and facade models for every registered block state. */
public final class CableModelLoader {
    public static void register(ModelEvent.ModifyBakingResult event) {
        var model = new GenericCableBakedModel();
        event.getBakingResult().blockStateModels().replaceAll((state, original) ->
                state.getBlock() instanceof GenericCableBlock || state.getBlock() instanceof FacadeBlock ? model : original);
    }
}
