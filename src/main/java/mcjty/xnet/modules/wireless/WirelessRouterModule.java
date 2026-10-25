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
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import mcjty.lib.datagen.model.ModelFile;
import mcjty.lib.datagen.model.VariantBlockStateBuilder;
import mcjty.lib.setup.DeferredBlock;
import mcjty.lib.setup.DeferredHolder;
import mcjty.lib.setup.DeferredItem;

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
            (block, itemProperties) -> new BlockItem(block.get(), itemProperties.useBlockDescriptionPrefix()),
            TileEntityWirelessRouter::new);
    public static final Supplier<MenuType<GenericContainer>> CONTAINER_WIRELESS_ROUTER = CONTAINERS.register("wireless_router", GenericContainer::createContainerType);

    public static final DeferredBlock<BaseBlock> ANTENNA = BLOCKS.register(ITEM_ANTENNA, WirelessRouterModule::createAntennaBlock);
    public static final DeferredItem<Item> ANTENNA_ITEM = ITEMS.register(ITEM_ANTENNA, tab(properties -> new BlockItem(ANTENNA.get(), properties.useBlockDescriptionPrefix())));
    public static final DeferredBlock<BaseBlock> ANTENNA_BASE = BLOCKS.register(ITEM_ANTENNA_BASE, WirelessRouterModule::createAntennaBaseBlock);
    public static final DeferredItem<Item> ANTENNA_BASE_ITEM = ITEMS.register(ITEM_ANTENNA_BASE, tab(properties -> new BlockItem(ANTENNA_BASE.get(), properties.useBlockDescriptionPrefix())));
    public static final DeferredBlock<BaseBlock> ANTENNA_DISH = BLOCKS.register(ITEM_ANTENNA_DISH, WirelessRouterModule::createAntennaDishBlock);
    public static final DeferredItem<Item> ANTENNA_DISH_ITEM = ITEMS.register(ITEM_ANTENNA_DISH, tab(properties -> new BlockItem(ANTENNA_DISH.get(), properties.useBlockDescriptionPrefix())));

    public static final AttachmentType<WirelessRouterData> WIRELESS_ROUTER_DATA = net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.create(
            net.minecraft.resources.Identifier.fromNamespaceAndPath("xnet", "wireless_router_data"), builder -> builder.initializer(() -> WirelessRouterData.EMPTY).persistent(WirelessRouterData.CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WirelessRouterData>> ITEM_WIRELESS_ROUTER_DATA = COMPONENTS.registerComponentType(
            "wireless_router_data",
            builder -> builder
                    .persistent(WirelessRouterData.CODEC)
                    .networkSynchronized(WirelessRouterData.STREAM_CODEC));


    public static final VoxelShape SMALLER_CUBE = Shapes.box(0.01f, 0.01f, 0.01f, 0.99f, 0.99f, 0.99f);

    private static BaseBlock createAntennaDishBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        return new BaseBlock(new BlockBuilder().properties(mcjty.rftoolsbase.tools.RegistryProperties.block(properties))
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
            public VoxelShape getOcclusionShape(@Nonnull BlockState state) {
                return SMALLER_CUBE;
            }
        };
    }

    private static BaseBlock createAntennaBaseBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        return new BaseBlock(new BlockBuilder().properties(mcjty.rftoolsbase.tools.RegistryProperties.block(properties))
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
            public VoxelShape getOcclusionShape(@Nonnull BlockState state) {
                return SMALLER_CUBE;
            }
        };
    }

    private static BaseBlock createAntennaBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        return new BaseBlock(new BlockBuilder().properties(mcjty.rftoolsbase.tools.RegistryProperties.block(properties))
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
            public VoxelShape getOcclusionShape(@Nonnull BlockState state) {
                return SMALLER_CUBE;
            }
        };
    }


    @Override
    public void init() {

    }

    @Override
    public void initClient() {
        registerScreens();
    }

    public void registerScreens() {
        GuiWirelessRouter.register();
    }

    @Override
    public void initConfig() {

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
                            builder.forAllStates(state -> mcjty.rftoolsbase.tools.ModelDataGen.rotated(
                                    state.getValue(TileEntityController.ERROR) ? modelError : modelOk, state.getValue(BlockStateProperties.FACING)));
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
