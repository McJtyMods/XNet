package mcjty.xnet.modules.controller;

import mcjty.lib.McJtyLib;
import mcjty.lib.blockcommands.ISerializer;
import mcjty.lib.blocks.BaseBlock;
import mcjty.lib.blocks.RBlock;
import mcjty.lib.container.GenericContainer;
import mcjty.lib.datagen.DataGen;
import mcjty.lib.datagen.Dob;
import mcjty.lib.modules.IModule;
import mcjty.lib.varia.OrientationTools;
import mcjty.rftoolsbase.modules.various.VariousModule;
import mcjty.xnet.client.ChannelClientInfo;
import mcjty.xnet.client.ConnectedBlockClientInfo;
import mcjty.xnet.modules.controller.blocks.TileEntityController;
import mcjty.xnet.modules.controller.client.GuiController;
import mcjty.xnet.modules.controller.data.ControllerData;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.VariantBlockStateBuilder;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static mcjty.lib.datagen.DataGen.has;
import static mcjty.xnet.XNet.tab;
import static mcjty.xnet.apiimpl.Constants.ITEM_CONTROLLER;
import static mcjty.xnet.modules.controller.ChannelInfo.MAX_CHANNELS;
import static mcjty.xnet.setup.Registration.*;

public class ControllerModule implements IModule {

    public static final RBlock<BaseBlock, BlockItem, TileEntityController> CONTROLLER = RBLOCKS.registerBlock("controller",
            TileEntityController.class,
            TileEntityController::createBlock,
            block -> new BlockItem(block.get(), createStandardProperties()),
            TileEntityController::new
    );
    public static final Supplier<MenuType<GenericContainer>> CONTAINER_CONTROLLER = CONTAINERS.register("controller", GenericContainer::createContainerType);

    public static final Supplier<AttachmentType<ControllerData>> CONTROLLER_DATA = ATTACHMENT_TYPES.register(
            "controller_data", () -> AttachmentType.builder(() -> {
                        var data = new ControllerData(0, new ArrayList<>());
                        for (int i = 0; i < MAX_CHANNELS; i++) {
                            data.channels().add(ChannelInfo.EMPTY);
                        }
                        return data;
                    })
                    .serialize(ControllerData.CODEC)
                    .build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ControllerData>> ITEM_CONTROLLER_DATA = COMPONENTS.registerComponentType(
            "controller_data",
            builder -> builder
                    .persistent(ControllerData.CODEC)
                    .networkSynchronized(ControllerData.STREAM_CODEC));

    public ControllerModule(IEventBus bus) {
        bus.addListener(this::registerScreens);
    }

    @Override
    public void init(FMLCommonSetupEvent event) {

    }

    @Override
    public void initClient(FMLClientSetupEvent event) {
        // Ensure the list result codecs are available before client payloads are decoded
        registerListCommandInfo(TileEntityController.CMD_GETCHANNELS.name(), ChannelClientInfo.class, new ChannelClientInfo.Serializer());
        registerListCommandInfo(TileEntityController.CMD_GETCONNECTEDBLOCKS.name(), ConnectedBlockClientInfo.class, new ConnectedBlockClientInfo.Serializer());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void registerListCommandInfo(String command, Class type, ISerializer serializer) {
        McJtyLib.registerListCommandInfo(command, type, serializer.getDeserializer(), serializer.getSerializer());
    }

    public void registerScreens(RegisterMenuScreensEvent event) {
        GuiController.register(event);
    }

    @Override
    public void initConfig(IEventBus bus) {

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
                            for (Direction direction : OrientationTools.DIRECTION_VALUES) {
                                p.applyRotation(builder.partialState().with(BlockStateProperties.FACING, direction).with(TileEntityController.ERROR, false)
                                        .modelForState().modelFile(modelOk), direction);
                                p.applyRotation(builder.partialState().with(BlockStateProperties.FACING, direction).with(TileEntityController.ERROR, true)
                                        .modelForState().modelFile(modelError), direction);
                            }
                        })
                        .shaped(builder -> builder
                                        .define('F', mcjty.rftoolsbase.modules.various.VariousModule.MACHINE_FRAME.get())
                                        .define('I', Items.REPEATER)
                                        .define('C', Items.COMPARATOR)
                                        .define('g', Tags.Items.INGOTS_GOLD)
                                        .unlockedBy("frame", has(VariousModule.MACHINE_FRAME.get())),
                                "ICI", "rFr", "igi"));
    }
}
