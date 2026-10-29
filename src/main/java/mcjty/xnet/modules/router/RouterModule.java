package mcjty.xnet.modules.router;

import java.util.function.Supplier;
import mcjty.lib.blocks.BaseBlock;
import mcjty.lib.blocks.RBlock;
import mcjty.lib.container.GenericContainer;
import mcjty.lib.datagen.DataGen;
import mcjty.lib.datagen.Dob;
import mcjty.lib.datagen.model.ModelFile;
import mcjty.lib.datagen.model.VariantBlockStateBuilder;
import mcjty.lib.items.BaseBlockItem;
import mcjty.lib.modules.IModule;
import mcjty.rftoolsbase.modules.various.VariousModule;
import mcjty.rftoolsbase.tools.ModelDataGen;
import mcjty.xnet.modules.controller.blocks.TileEntityController;
import mcjty.xnet.modules.router.blocks.TileEntityRouter;
import mcjty.xnet.modules.router.client.GuiRouter;
import mcjty.xnet.modules.router.data.RouterData;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import mcjty.lib.setup.DeferredHolder;
import static mcjty.lib.datagen.DataGen.has;
import static mcjty.xnet.apiimpl.Constants.ITEM_ROUTER;
import static mcjty.xnet.setup.Registration.*;

public class RouterModule implements IModule {

    public static final RBlock<BaseBlock, BlockItem, TileEntityRouter> ROUTER = RBLOCKS.registerBlock("router",
            TileEntityRouter.class,
            TileEntityRouter::createBlock,
            (block, itemProperties) -> new BaseBlockItem(block.get(), itemProperties.useBlockDescriptionPrefix()),
            TileEntityRouter::new);
    public static final Supplier<MenuType<GenericContainer>> CONTAINER_ROUTER = CONTAINERS.register("router", GenericContainer::createContainerType);

    public static final AttachmentType<RouterData> ROUTER_DATA = net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.create(
            net.minecraft.resources.Identifier.fromNamespaceAndPath("xnet", "router_data"), builder -> builder.initializer(() -> RouterData.EMPTY).persistent(RouterData.CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<RouterData>> ITEM_ROUTER_DATA = COMPONENTS.registerComponentType(
            "router_data",
            builder -> builder
                    .persistent(RouterData.CODEC)
                    .networkSynchronized(RouterData.STREAM_CODEC));

    @Override
    public void init() {

    }

    @Override
    public void initClient() {
        registerScreens();
    }

    public void registerScreens() {
        GuiRouter.register();
    }

    @Override
    public void initConfig() {

    }

    @Override
    public void initDatagen(DataGen dataGen, HolderLookup.Provider provider) {
        dataGen.add(
                Dob.blockBuilder(ROUTER)
                        .ironPickaxeTags()
                        .parentedItem("block/router")
                        .standardLoot(ITEM_ROUTER_DATA.get())
                        .blockState(p -> {
                            ModelFile modelOk = p.frontBasedModel(ITEM_ROUTER, p.modLoc("block/machine_router"));
                            ModelFile modelError = p.frontBasedModel("router_error", p.modLoc("block/machine_router_error"));
                            VariantBlockStateBuilder builder = p.getVariantBuilder(ROUTER.block().get());
                            builder.forAllStates(state -> ModelDataGen.rotated(
                                    state.getValue(TileEntityController.ERROR) ? modelError : modelOk, state.getValue(BlockStateProperties.FACING)));
                        })
                        .shaped(builder -> builder
                                        .define('F', VariousModule.MACHINE_FRAME.get())
                                        .define('I', Items.POWERED_RAIL)
                                        .define('C', Items.COMPARATOR)
                                        .unlockedBy("frame", has(VariousModule.MACHINE_FRAME.get())),
                                "ICI", "rFr", "ioi")
        );
    }
}
