package mcjty.xnet.modules.facade.client;

import mcjty.xnet.modules.cables.CableModule;
import mcjty.xnet.modules.facade.FacadeModule;

public class ClientSetup {

    public static void registerBlockColor() {
        net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry.register(java.util.stream.IntStream.range(0, 16).mapToObj(i -> (net.minecraft.client.color.block.BlockTintSource) new FacadeBlockColor(i)).toList(),
                FacadeModule.FACADE.get(), CableModule.CONNECTOR.get(), CableModule.ADVANCED_CONNECTOR.get());
    }
}
