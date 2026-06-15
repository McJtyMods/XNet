package mcjty.xnet.modules.wireless;

import mcjty.lib.blocks.BaseBlock;
import mcjty.lib.blocks.RotationType;
import mcjty.lib.builder.BlockBuilder;
import mcjty.lib.builder.TooltipBuilder;
import mcjty.lib.container.GenericContainer;
import mcjty.lib.datagen.DataGen;
import mcjty.lib.datagen.Dob;
import mcjty.lib.modules.IModule;
import mcjty.lib.setup.DeferredBlock;
import mcjty.lib.setup.DeferredItem;
import mcjty.lib.varia.OrientationTools;
import mcjty.rftoolsbase.modules.various.VariousModule;
import mcjty.rftoolsbase.tools.ManualHelper;
import mcjty.xnet.modules.controller.blocks.TileEntityController;
import mcjty.xnet.modules.wireless.blocks.TileEntityWirelessRouter;
import mcjty.xnet.modules.wireless.client.GuiWirelessRouter;
import mcjty.xnet.setup.Config;
import mcjty.xnet.setup.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.VariantBlockStateBuilder;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import javax.annotation.Nonnull;

import java.util.function.Supplier;

import static mcjty.lib.datagen.DataGen.has;
import static mcjty.xnet.XNet.tab;
import static mcjty.xnet.apiimpl.Constants.ITEM_ANTENNA;
import static mcjty.xnet.apiimpl.Constants.ITEM_ANTENNA_BASE;
import static mcjty.xnet.apiimpl.Constants.ITEM_ANTENNA_DISH;
import static mcjty.xnet.apiimpl.Constants.ITEM_WIRELESS_ROUTER;
import static mcjty.xnet.setup.Registration.BLOCKS;
import static mcjty.xnet.setup.Registration.CONTAINERS;
import static mcjty.xnet.setup.Registration.ITEMS;
import static mcjty.xnet.setup.Registration.TILES;

public class WirelessRouterModule implements IModule {

    public static final DeferredBlock<BaseBlock> WIRELESS_ROUTER = BLOCKS.register(ITEM_WIRELESS_ROUTER, TileEntityWirelessRouter::createBlock);
    public static final DeferredItem<Item> WIRELESS_ROUTER_ITEM = ITEMS.register(ITEM_WIRELESS_ROUTER, tab(() -> new BlockItem(WIRELESS_ROUTER.get(), Registration.createStandardProperties())));
    public static final Supplier<BlockEntityType<?>> TYPE_WIRELESS_ROUTER = TILES.register(ITEM_WIRELESS_ROUTER, () -> BlockEntityType.Builder.of(TileEntityWirelessRouter::new, WIRELESS_ROUTER.get()).build(null));
    public static final Supplier<MenuType<GenericContainer>> CONTAINER_WIRELESS_ROUTER = CONTAINERS.register(ITEM_WIRELESS_ROUTER, GenericContainer::createContainerType);

    public static final DeferredBlock<BaseBlock> ANTENNA = BLOCKS.register(ITEM_ANTENNA, WirelessRouterModule::createAntennaBlock);
    public static final DeferredItem<Item> ANTENNA_ITEM = ITEMS.register(ITEM_ANTENNA, tab(() -> new BlockItem(ANTENNA.get(), Registration.createStandardProperties())));
    public static final DeferredBlock<BaseBlock> ANTENNA_BASE = BLOCKS.register(ITEM_ANTENNA_BASE, WirelessRouterModule::createAntennaBaseBlock);
    public static final DeferredItem<Item> ANTENNA_BASE_ITEM = ITEMS.register(ITEM_ANTENNA_BASE, tab(() -> new BlockItem(ANTENNA_BASE.get(), Registration.createStandardProperties())));
    public static final DeferredBlock<BaseBlock> ANTENNA_DISH = BLOCKS.register(ITEM_ANTENNA_DISH, WirelessRouterModule::createAntennaDishBlock);
    public static final DeferredItem<Item> ANTENNA_DISH_ITEM = ITEMS.register(ITEM_ANTENNA_DISH, tab(() -> new BlockItem(ANTENNA_DISH.get(), Registration.createStandardProperties())));

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

    @Override
    public void init(FMLCommonSetupEvent event) {

    }

    @Override
    public void initClient(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            GuiWirelessRouter.register();
        });
    }

    @Override
    public void initConfig(IEventBus bus) {

    }

    @Override
    public void initDatagen(DataGen dataGen) {
        dataGen.add(
                Dob.blockBuilder(WIRELESS_ROUTER)
                        .ironPickaxeTags()
                        .parentedItem("block/wireless_router")
                        .standardLoot(TYPE_WIRELESS_ROUTER)
                        .blockState(p -> {
                            ModelFile modelOk = p.frontBasedModel(ITEM_WIRELESS_ROUTER, p.modLoc("block/machine_wireless_router"));
                            ModelFile modelError = p.frontBasedModel("wireless_router_error", p.modLoc("block/machine_wireless_router_error"));
                            VariantBlockStateBuilder builder = p.getVariantBuilder(WIRELESS_ROUTER.get());
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
