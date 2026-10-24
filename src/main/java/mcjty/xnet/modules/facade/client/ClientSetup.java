package mcjty.xnet.modules.facade.client;

import mcjty.xnet.modules.cables.CableModule;
import mcjty.xnet.modules.facade.FacadeModule;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

public class ClientSetup {

    public static void registerBlockColor(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(java.util.stream.IntStream.range(0, 16).mapToObj(i -> (net.minecraft.client.color.block.BlockTintSource) new FacadeBlockColor(i)).toList(),
                FacadeModule.FACADE.get(), CableModule.CONNECTOR.get(), CableModule.ADVANCED_CONNECTOR.get());
    }
}
