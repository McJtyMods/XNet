package mcjty.xnet.modules.cables.client;

import mcjty.xnet.modules.cables.CableModule;
import mcjty.xnet.modules.facade.FacadeModule;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.world.level.block.state.BlockState;

/** Resolve cable geometry before Minecraft groups visually equivalent block states. */
public final class CableModelLoader {
    public static void register() {
        ModelLoadingPlugin.register(plugin -> {
            var model = new GenericCableBakedModel();
            var root = new BlockStateModel.UnbakedRoot() {
                @Override public BlockStateModel bake(BlockState state, ModelBaker baker) { return model; }
                @Override public Object visualEqualityGroup(BlockState state) { return state; }
                @Override public void resolveDependencies(ResolvableModel.Resolver resolver) { }
            };
            for (var block : java.util.List.of(CableModule.NETCABLE.get(), CableModule.CONNECTOR.get(),
                    CableModule.ADVANCED_CONNECTOR.get(), FacadeModule.FACADE.get())) {
                plugin.registerBlockStateResolver(block, context ->
                        context.block().getStateDefinition().getPossibleStates().forEach(state -> context.setModel(state, root)));
            }
        });
    }
}
