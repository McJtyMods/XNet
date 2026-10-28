package mcjty.xnet.modules.controller;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
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
import mcjty.lib.varia.OrientationTools;
import mcjty.rftoolsbase.modules.various.VariousModule;
import mcjty.rftoolsbase.tools.ModelDataGen;
import mcjty.xnet.modules.controller.blocks.TileEntityController;
import mcjty.xnet.modules.controller.client.GuiController;
import mcjty.xnet.modules.controller.data.ControllerData;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredHolder;

import static mcjty.lib.datagen.DataGen.has;
import static mcjty.xnet.XNet.tab;
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

    public static final Supplier<AttachmentType<ControllerData>> CONTROLLER_DATA = ATTACHMENT_TYPES.register(
            "controller_data", () -> AttachmentType.builder(() -> {
                        var data = new ControllerData(0, new ArrayList<>());
                        for (int i = 0; i < MAX_CHANNELS; i++) {
                            data.channels().add(ChannelInfo.EMPTY);
                        }
                        return data;
                    })
                    .serialize(MapCodec.assumeMapUnsafe(ControllerData.CODEC))
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
                        .blockTags(List.of(BlockTags.BLOCKS_MOTION_NO_LEAVES))
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
                                        .define('g', Tags.Items.INGOTS_GOLD)
                                        .unlockedBy("frame", has(VariousModule.MACHINE_FRAME.get())),
                                "ICI", "rFr", "igi"));
    }
}
