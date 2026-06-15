package mcjty.xnet.modules.wireless;

import mcjty.lib.blocks.BaseBlock;
import mcjty.lib.blocks.RBlock;
import mcjty.lib.blocks.RotationType;
import mcjty.lib.builder.BlockBuilder;
import mcjty.lib.builder.TooltipBuilder;
import mcjty.lib.container.GenericContainer;
import mcjty.lib.datagen.DataGen;
import mcjty.lib.datagen.Dob;
import mcjty.lib.modules.IModule;
import mcjty.lib.varia.OrientationTools;
import mcjty.rftoolsbase.modules.various.VariousModule;
import mcjty.xnet.modules.controller.blocks.TileEntityController;
import mcjty.xnet.modules.wireless.blocks.TileEntityWirelessRouter;
import mcjty.xnet.modules.wireless.client.GuiWirelessRouter;
import mcjty.xnet.modules.wireless.data.WirelessRouterData;
import mcjty.xnet.setup.Config;
import mcjty.xnet.setup.Registration;
import mcjty.rftoolsbase.tools.ManualHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.VariantBlockStateBuilder;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

import static mcjty.lib.datagen.DataGen.has;
import static mcjty.xnet.XNet.tab;
import static mcjty.xnet.apiimpl.Constants.ITEM_ANTENNA;
import static mcjty.xnet.apiimpl.Constants.ITEM_ANTENNA_BASE;
import static mcjty.xnet.apiimpl.Constants.ITEM_ANTENNA_DISH;
import static mcjty.xnet.apiimpl.Constants.ITEM_WIRELESS_ROUTER;
import static mcjty.xnet.setup.Registration.*;

public class WirelessRouterModule implements IModule {

    public static final RBlock<BaseBlock, BlockItem, TileEntityWirelessRouter> WIRELESS_ROUTER = RBLOCKS.registerBlock("wireless_router",
            TileEntityWirelessRouter.class,
            TileEntityWirelessRouter::createBlock,
            block -> new BlockItem(block.get(), createStandardProperties()),
            TileEntityWirelessRouter::new
    );
    public static final Supplier<MenuType<GenericContainer>> CONTAINER_WIRELESS_ROUTER = CONTAINERS.register("wireless_router", GenericContainer::createContainerType);

    public static final DeferredBlock<BaseBlock> ANTENNA = BLOCKS.register(ITEM_ANTENNA, WirelessRouterModule::createAntennaBlock);
    public static final DeferredItem<Item> ANTENNA_ITEM = ITEMS.register(ITEM_ANTENNA, tab(() -> new BlockItem(ANTENNA.get(), Registration.createStandardProperties())));
    public static final DeferredBlock<BaseBlock> ANTENNA_BASE = BLOCKS.register(ITEM_ANTENNA_BASE, WirelessRouterModule::createAntennaBaseBlock);
    public static final DeferredItem<Item> ANTENNA_BASE_ITEM = ITEMS.register(ITEM_ANTENNA_BASE, tab(() -> new BlockItem(ANTENNA_BASE.get(), Registration.createStandardProperties())));
    public static final DeferredBlock<BaseBlock> ANTENNA_DISH = BLOCKS.register(ITEM_ANTENNA_DISH, WirelessRouterModule::createAntennaDishBlock);
    public static final DeferredItem<Item> ANTENNA_DISH_ITEM = ITEMS.register(ITEM_ANTENNA_DISH, tab(() -> new BlockItem(ANTENNA_DISH.get(), Registration.createStandardProperties())));

    public static final Supplier<AttachmentType<WirelessRouterData>> WIRELESS_ROUTER_DATA = ATTACHMENT_TYPES.register(
            "wireless_router_data", () -> AttachmentType.builder(() -> WirelessRouterData.EMPTY)
                    .serialize(WirelessRouterData.CODEC)
                    .build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WirelessRouterData>> ITEM_WIRELESS_ROUTER_DATA = COMPONENTS.registerComponentType(
            "wireless_router_data",
            builder -> builder
                    .persistent(WirelessRouterData.CODEC)
                    .networkSynchronized(WirelessRouterData.STREAM_CODEC));


    public static final VoxelShape SMALLER_CUBE = Shapes.box(0.01f, 0.01f, 0.01f, 0.99f, 0.99f, 0.99f);

    private static BaseBlock createAntennaDishBlock() {
        return new BaseBlock(new BlockBuilder()
                .manualEntry(ManualHelper.create("xnet:wireless/antennas"))
                .info(TooltipBuilder.key("message.xnet.shiftmessage"))
                .infoShift(TooltipBuilder.header(),
                        TooltipBuilder.parameter("info", stack -> Integer.toString(Config.wirelessRouterRfPerChannel[TileEntityWirelessRouter.TIER_INF].get()) + " rf/t/channel"))
        ) {
            @Override
            public RotationType getRotationType() {
                return RotationType.HORIZROTATION;
            }

            @Nonnull
            @Override
            public VoxelShape getOcclusionShape(@Nonnull BlockState state, @Nonnull BlockGetter world, @Nonnull BlockPos pos) {
                return SMALLER_CUBE;
            }
        };
    }

    private static BaseBlock createAntennaBaseBlock() {
        return new BaseBlock(new BlockBuilder()
                .manualEntry(ManualHelper.create("xnet:wireless/antennas"))
                .info(TooltipBuilder.key("message.xnet.shiftmessage"))
                .infoShift(TooltipBuilder.header())
        ) {
            @Override
            public RotationType getRotationType() {
                return RotationType.NONE;
            }

            @Nonnull
            @Override
            public VoxelShape getOcclusionShape(@Nonnull BlockState state, @Nonnull BlockGetter world, @Nonnull BlockPos pos) {
                return SMALLER_CUBE;
            }
        };
    }

    private static BaseBlock createAntennaBlock() {
        return new BaseBlock(new BlockBuilder()
                .manualEntry(ManualHelper.create("xnet:wireless/antennas"))
                .info(TooltipBuilder.key("message.xnet.shiftmessage"))
                .infoShift(TooltipBuilder.header(),
                        TooltipBuilder.parameter("one", stack -> "range " + Integer.toString(Config.antennaTier1Range.get()) + " (" + Integer.toString(Config.wirelessRouterRfPerChannel[TileEntityWirelessRouter.TIER_1].get()) + " rf/t/channel)"),
                        TooltipBuilder.parameter("two", stack -> "range " + Integer.toString(Config.antennaTier2Range.get()) + " (" + Integer.toString(Config.wirelessRouterRfPerChannel[TileEntityWirelessRouter.TIER_2].get()) + " rf/t/channel)")
                )
        ) {
            @Override
            public RotationType getRotationType() {
                return RotationType.HORIZROTATION;
            }

            @Nonnull
            @Override
            public VoxelShape getOcclusionShape(@Nonnull BlockState state, @Nonnull BlockGetter world, @Nonnull BlockPos pos) {
                return SMALLER_CUBE;
            }
        };
    }

    public WirelessRouterModule(IEventBus bus) {
        bus.addListener(this::registerScreens);
    }

    @Override
    public void init(FMLCommonSetupEvent event) {

    }

    @Override
    public void initClient(FMLClientSetupEvent event) {
    }

    public void registerScreens(RegisterMenuScreensEvent event) {
        GuiWirelessRouter.register(event);
    }

    @Override
    public void initConfig(IEventBus bus) {

    }

    @Override
    public void initDatagen(DataGen dataGen, HolderLookup.Provider provider) {
        dataGen.add(
                Dob.blockBuilder(WIRELESS_ROUTER)
                        .ironPickaxeTags()
                        .parentedItem("block/wireless_router")
                        .standardLoot(ITEM_WIRELESS_ROUTER_DATA.get())
                        .blockState(p -> {
                            ModelFile modelOk = p.frontBasedModel(ITEM_WIRELESS_ROUTER, p.modLoc("block/machine_wireless_router"));
                            ModelFile modelError = p.frontBasedModel("wireless_router_error", p.modLoc("block/machine_wireless_router_error"));
                            VariantBlockStateBuilder builder = p.getVariantBuilder(WIRELESS_ROUTER.block().get());
                            for (Direction direction : OrientationTools.DIRECTION_VALUES) {
                                p.applyRotation(builder.partialState().with(BlockStateProperties.FACING, direction).with(TileEntityController.ERROR, false)
                                        .modelForState().modelFile(modelOk), direction);
                                p.applyRotation(builder.partialState().with(BlockStateProperties.FACING, direction).with(TileEntityController.ERROR, true)
                                        .modelForState().modelFile(modelError), direction);
                            }
                        })
                        .shaped(builder -> builder
                                        .define('F', VariousModule.MACHINE_FRAME.get())
                                        .define('C', Items.COMPARATOR)
                                        .unlockedBy("frame", has(VariousModule.MACHINE_FRAME.get())),
                                "oCo", "rFr", "oro"),
                Dob.blockBuilder(ANTENNA)
                        .simpleLoot()
                        .ironPickaxeTags()
                        .shaped(builder -> builder
                                        .define('I', Items.IRON_BARS)
                                        .unlockedBy("bars", has(Items.IRON_BARS)),
                                "IiI", "IiI", " i "),
                Dob.blockBuilder(ANTENNA_BASE)
                        .simpleLoot()
                        .ironPickaxeTags()
                        .shaped(builder -> builder
                                        .define('I', Items.IRON_BLOCK)
                                        .unlockedBy("block", has(Items.IRON_BLOCK)),
                                " i ", " i ", "iIi"),
                Dob.blockBuilder(ANTENNA_DISH)
                        .simpleLoot()
                        .ironPickaxeTags()
                        .shaped(builder -> builder
                                        .define('I', Items.IRON_TRAPDOOR)
                                        .unlockedBy("trapdoor", has(Items.IRON_TRAPDOOR)),
                                "III", "IoI", " i ")
        );
    }
}
