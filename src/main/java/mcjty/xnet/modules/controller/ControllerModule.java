package mcjty.xnet.modules.controller;

import java.util.ArrayList;
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
import mcjty.xnet.modules.controller.client.GuiController;
import mcjty.xnet.modules.controller.data.ControllerData;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import mcjty.lib.setup.DeferredHolder;
import static mcjty.lib.datagen.DataGen.has;
import static mcjty.xnet.apiimpl.Constants.ITEM_CONTROLLER;
import static mcjty.xnet.modules.controller.ChannelInfo.MAX_CHANNELS;
import static mcjty.xnet.setup.Registration.*;

public class ControllerModule implements IModule {

    public static final RBlock<BaseBlock, BlockItem, TileEntityController> CONTROLLER = RBLOCKS.registerBlock("controller",
            TileEntityController.class,
            TileEntityController::createBlock,
            (block, itemProperties) -> new BaseBlockItem(block.get(), itemProperties.useBlockDescriptionPrefix()),
            TileEntityController::new);
    public static final Supplier<MenuType<GenericContainer>> CONTAINER_CONTROLLER = CONTAINERS.register("controller", GenericContainer::createContainerType);

    public static final AttachmentType<ControllerData> CONTROLLER_DATA = net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.create(
            net.minecraft.resources.Identifier.fromNamespaceAndPath("xnet", "controller_data"), builder -> builder.initializer(() -> {
                        var data = new ControllerData(0, new ArrayList<>());
                        for (int i = 0; i < MAX_CHANNELS; i++) {
                            data.channels().add(ChannelInfo.EMPTY);
                        }
                        return data;
                    }).persistent(ControllerData.CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ControllerData>> ITEM_CONTROLLER_DATA = COMPONENTS.registerComponentType(
            "controller_data",
            builder -> builder
                    .persistent(ControllerData.CODEC)
                    .networkSynchronized(ControllerData.STREAM_CODEC));

    @Override
    public void init() {

    }

    @Override
    public void initClient() {
        registerScreens();
    }

    public void registerScreens() {
        GuiController.register();
    }

    @Override
    public void initConfig() {

    }

    @Override
    public void initDatagen(DataGen dataGen, HolderLookup.Provider provider) {
        dataGen.add(
                Dob.blockBuilder(CONTROLLER)
                        .ironPickaxeTags()
                        .parentedItem("block/controller")
                        .standardLoot(ITEM_CONTROLLER_DATA.get())
                        .blockState(p -> {
                            ModelFile modelOk = p.frontBasedModel(ITEM_CONTROLLER, p.modLoc("block/machine_controller"));
                            ModelFile modelError = p.frontBasedModel("controller_error", p.modLoc("block/machine_controller_error"));
                            VariantBlockStateBuilder builder = p.getVariantBuilder(ControllerModule.CONTROLLER.block().get());
                            builder.forAllStates(state -> ModelDataGen.rotated(
                                    state.getValue(TileEntityController.ERROR) ? modelError : modelOk, state.getValue(BlockStateProperties.FACING)));
                        })
                        .shaped(builder -> builder
                                        .define('F', VariousModule.MACHINE_FRAME.get())
                                        .define('I', Items.REPEATER)
                                        .define('C', Items.COMPARATOR)
                                        .define('g', net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:ingots/gold")))
                                        .unlockedBy("frame", has(VariousModule.MACHINE_FRAME.get())),
                                "ICI", "rFr", "igi"));
    }
}
