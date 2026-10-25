package mcjty.xnet.modules.cables.client;

import com.mojang.serialization.MapCodec;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.neoforged.neoforge.client.event.RegisterBlockStateModels;
import net.neoforged.neoforge.client.model.block.CustomBlockModelDefinition;

/** Load cable models before Minecraft groups visually equivalent block states. */
public final class CableModelLoader implements CustomBlockModelDefinition {
    public static final MapCodec<CableModelLoader> CODEC = MapCodec.unit(CableModelLoader::new);

    public static void register(RegisterBlockStateModels event) {
        event.registerDefinition(Identifier.fromNamespaceAndPath("xnet", "cable"), CODEC);
    }

    @Override
    public Map<BlockState, BlockStateModel.UnbakedRoot> instantiate(StateDefinition<Block, BlockState> states,
            Supplier<String> source) {
        var model = new GenericCableBakedModel();
        var root = new BlockStateModel.UnbakedRoot() {
            @Override public BlockStateModel bake(BlockState state, ModelBaker baker) { return model; }
            // Connections and cable color change the geometry. Do not suppress their redraws.
            @Override public Object visualEqualityGroup(BlockState state) { return state; }
            @Override public void resolveDependencies(ResolvableModel.Resolver resolver) { }
        };
        Map<BlockState, BlockStateModel.UnbakedRoot> models = new IdentityHashMap<>();
        states.getPossibleStates().forEach(state -> models.put(state, root));
        return models;
    }

    @Override public MapCodec<CableModelLoader> codec() { return CODEC; }
}
