package mcjty.xnet.modules.cables;

import mcjty.lib.container.GenericContainer;
import mcjty.lib.datagen.DataGen;
import mcjty.lib.datagen.Dob;
import mcjty.lib.modules.IModule;
import mcjty.lib.varia.TagTools;
import mcjty.xnet.XNet;
import mcjty.xnet.modules.cables.blocks.AdvancedConnectorBlock;
import mcjty.xnet.modules.cables.blocks.AdvancedConnectorTileEntity;
import mcjty.xnet.modules.cables.blocks.ColorBlockItem;
import mcjty.xnet.modules.cables.blocks.ConnectorBlock;
import mcjty.xnet.modules.cables.blocks.ConnectorTileEntity;
import mcjty.xnet.modules.cables.blocks.GenericCableBlock.CableBlockType;
import mcjty.xnet.modules.cables.blocks.NetCableBlock;
import mcjty.xnet.modules.cables.client.ClientSetup;
import mcjty.xnet.modules.cables.client.GuiConnector;
import mcjty.xnet.modules.cables.data.CableItemData;
import mcjty.xnet.modules.cables.data.ConnectorData;
import mcjty.xnet.setup.Registration;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import mcjty.lib.setup.DeferredBlock;
import mcjty.lib.setup.DeferredHolder;
import mcjty.lib.setup.DeferredItem;

import java.util.List;
import java.util.function.Supplier;

import static mcjty.lib.datagen.DataGen.has;
import static mcjty.xnet.XNet.tab;
import static mcjty.xnet.apiimpl.Constants.TAG_CONNECTORS;
import static mcjty.xnet.setup.Registration.*;

public class CableModule implements IModule {

    public static final DeferredBlock<NetCableBlock> NETCABLE = BLOCKS.register("netcable", properties -> new NetCableBlock(CableBlockType.CABLE, properties));
    public static final DeferredItem<Item> NETCABLE_RED = ITEMS.register("netcable_red", tab(properties -> new ColorBlockItem(NETCABLE.get(), properties, CableColor.RED)));
    public static final DeferredItem<Item> NETCABLE_GREEN = ITEMS.register("netcable_green", tab(properties -> new ColorBlockItem(NETCABLE.get(), properties, CableColor.GREEN)));
    public static final DeferredItem<Item> NETCABLE_BLUE = ITEMS.register("netcable_blue", tab(properties -> new ColorBlockItem(NETCABLE.get(), properties, CableColor.BLUE)));
    public static final DeferredItem<Item> NETCABLE_YELLOW = ITEMS.register("netcable_yellow", tab(properties -> new ColorBlockItem(NETCABLE.get(), properties, CableColor.YELLOW)));
    public static final DeferredItem<Item> NETCABLE_ROUTING = ITEMS.register("netcable_routing", tab(properties -> new ColorBlockItem(NETCABLE.get(), properties, CableColor.ROUTING)));

    public static final DeferredBlock<ConnectorBlock> CONNECTOR = BLOCKS.register("connector", properties -> new ConnectorBlock(CableBlockType.CONNECTOR, properties));
    public static final DeferredItem<Item> CONNECTOR_RED = ITEMS.register("connector_red", tab(properties -> new ColorBlockItem(CONNECTOR.get(), properties, CableColor.RED)));
    public static final DeferredItem<Item> CONNECTOR_GREEN = ITEMS.register("connector_green", tab(properties -> new ColorBlockItem(CONNECTOR.get(), properties, CableColor.GREEN)));
    public static final DeferredItem<Item> CONNECTOR_BLUE = ITEMS.register("connector_blue", tab(properties -> new ColorBlockItem(CONNECTOR.get(), properties, CableColor.BLUE)));
    public static final DeferredItem<Item> CONNECTOR_YELLOW = ITEMS.register("connector_yellow", tab(properties -> new ColorBlockItem(CONNECTOR.get(), properties, CableColor.YELLOW)));
    public static final DeferredItem<Item> CONNECTOR_ROUTING = ITEMS.register("connector_routing", tab(properties -> new ColorBlockItem(CONNECTOR.get(), properties, CableColor.ROUTING)));

    public static final DeferredBlock<AdvancedConnectorBlock> ADVANCED_CONNECTOR = BLOCKS.register("advanced_connector", properties -> new AdvancedConnectorBlock(CableBlockType.ADVANCED_CONNECTOR, properties));
    public static final DeferredItem<Item> ADVANCED_CONNECTOR_RED = ITEMS.register("advanced_connector_red", tab(properties -> new ColorBlockItem(ADVANCED_CONNECTOR.get(), properties, CableColor.RED)));
    public static final DeferredItem<Item> ADVANCED_CONNECTOR_GREEN = ITEMS.register("advanced_connector_green", tab(properties -> new ColorBlockItem(ADVANCED_CONNECTOR.get(), properties, CableColor.GREEN)));
    public static final DeferredItem<Item> ADVANCED_CONNECTOR_BLUE = ITEMS.register("advanced_connector_blue", tab(properties -> new ColorBlockItem(ADVANCED_CONNECTOR.get(), properties, CableColor.BLUE)));
    public static final DeferredItem<Item> ADVANCED_CONNECTOR_YELLOW = ITEMS.register("advanced_connector_yellow", tab(properties -> new ColorBlockItem(ADVANCED_CONNECTOR.get(), properties, CableColor.YELLOW)));
    public static final DeferredItem<Item> ADVANCED_CONNECTOR_ROUTING = ITEMS.register("advanced_connector_routing", tab(properties -> new ColorBlockItem(ADVANCED_CONNECTOR.get(), properties, CableColor.ROUTING)));

    public static final Supplier<BlockEntityType<?>> TYPE_CONNECTOR = TILES.register("connector", () -> new BlockEntityType<>(ConnectorTileEntity::new, java.util.Set.of(CONNECTOR.get())));
    public static final Supplier<BlockEntityType<?>> TYPE_ADVANCED_CONNECTOR = TILES.register("advanced_connector", () -> new BlockEntityType<>(AdvancedConnectorTileEntity::new, java.util.Set.of(ADVANCED_CONNECTOR.get())));

    public static final Supplier<MenuType<GenericContainer>> CONTAINER_CONNECTOR = CONTAINERS.register("connector", GenericContainer::createContainerType);

    public static final TagKey<Item> TAG_KEY_CABLES = TagTools.createItemTagKey(Identifier.fromNamespaceAndPath(XNet.MODID, "cables"));
    public static final TagKey<Item> TAG_KEY_CONNECTORS = TagTools.createItemTagKey(Identifier.fromNamespaceAndPath(XNet.MODID, "connectors"));
    public static final TagKey<Item> TAG_KEY_ADVANCED_CONNECTORS = TagTools.createItemTagKey(Identifier.fromNamespaceAndPath(XNet.MODID, "advanced_connectors"));

    public static final AttachmentType<ConnectorData> CONNECTOR_DATA = net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.create(
            net.minecraft.resources.Identifier.fromNamespaceAndPath("xnet", "connector_data"), builder -> builder.initializer(() -> ConnectorData.EMPTY).persistent(ConnectorData.CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ConnectorData>> ITEM_CONNECTOR_DATA = COMPONENTS.registerComponentType(
            "connector_data",
            builder -> builder
                    .persistent(ConnectorData.CODEC)
                    .networkSynchronized(ConnectorData.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CableItemData>> ITEM_CABLE_ITEM_DATA = COMPONENTS.registerComponentType(
            "cable_item_data",
            builder -> builder
                    .persistent(CableItemData.CODEC)
                    .networkSynchronized(CableItemData.STREAM_CODEC));


    @Override
    public void init() {

    }

    @Override
    public void initClient() {
        registerScreens();
        ClientSetup.registerModels();
        ClientSetup.initClient();
    }

    public void registerScreens() {
        GuiConnector.register();
    }

    @Override
    public void initConfig() {

    }

    @Override
    public void initDatagen(DataGen dataGen, HolderLookup.Provider provider) {
        dataGen.add(
                Dob.blockBuilder(ADVANCED_CONNECTOR)
                        .blockTags(java.util.List.of(net.minecraft.tags.BlockTags.BLOCKS_MOTION_NO_LEAVES))
                        // @todo 1.21 correct loot tables for copy of data
                        .loot(p -> {
                            p.addLootTable(ADVANCED_CONNECTOR.get(), LootTable.lootTable()
                                    .withPool(DataGenHelper.getLootTableEntry("advanced_connector_blue", ADVANCED_CONNECTOR.get(), ADVANCED_CONNECTOR_BLUE.get(), CableColor.BLUE))
                                    .withPool(DataGenHelper.getLootTableEntry("advanced_connector_red", ADVANCED_CONNECTOR.get(), ADVANCED_CONNECTOR_RED.get(), CableColor.RED))
                                    .withPool(DataGenHelper.getLootTableEntry("advanced_connector_green", ADVANCED_CONNECTOR.get(), ADVANCED_CONNECTOR_GREEN.get(), CableColor.GREEN))
                                    .withPool(DataGenHelper.getLootTableEntry("advanced_connector_yellow", ADVANCED_CONNECTOR.get(), ADVANCED_CONNECTOR_YELLOW.get(), CableColor.YELLOW))
                                    .withPool(DataGenHelper.getLootTableEntry("advanced_connector_routing", ADVANCED_CONNECTOR.get(), ADVANCED_CONNECTOR_ROUTING.get(), CableColor.ROUTING)));

                        })
                        .stonePickaxeTags(),
                Dob.blockBuilder(CONNECTOR)
                        .blockTags(java.util.List.of(net.minecraft.tags.BlockTags.BLOCKS_MOTION_NO_LEAVES))
                        // @todo 1.21 correct loot tables for copy of data
                        .loot(p -> {
                            p.addLootTable(CONNECTOR.get(), LootTable.lootTable()
                                    .withPool(DataGenHelper.getLootTableEntry("connector_blue", CONNECTOR.get(), CONNECTOR_BLUE.get(), CableColor.BLUE))
                                    .withPool(DataGenHelper.getLootTableEntry("connector_red", CONNECTOR.get(), CONNECTOR_RED.get(), CableColor.RED))
                                    .withPool(DataGenHelper.getLootTableEntry("connector_green", CONNECTOR.get(), CONNECTOR_GREEN.get(), CableColor.GREEN))
                                    .withPool(DataGenHelper.getLootTableEntry("connector_yellow", CONNECTOR.get(), CONNECTOR_YELLOW.get(), CableColor.YELLOW))
                                    .withPool(DataGenHelper.getLootTableEntry("connector_routing", CONNECTOR.get(), CONNECTOR_ROUTING.get(), CableColor.ROUTING)));

                        })
                        .stonePickaxeTags(),
                Dob.blockBuilder(NETCABLE)
                        .blockTags(java.util.List.of(net.minecraft.tags.BlockTags.BLOCKS_MOTION_NO_LEAVES))
                        .loot(p -> {
                            p.addLootTable(NETCABLE.get(), LootTable.lootTable()
                                    .withPool(DataGenHelper.getLootTableEntry("cable_blue", NETCABLE.get(), NETCABLE_BLUE.get(), CableColor.BLUE))
                                    .withPool(DataGenHelper.getLootTableEntry("cable_red", NETCABLE.get(), NETCABLE_RED.get(), CableColor.RED))
                                    .withPool(DataGenHelper.getLootTableEntry("cable_green", NETCABLE.get(), NETCABLE_GREEN.get(), CableColor.GREEN))
                                    .withPool(DataGenHelper.getLootTableEntry("cable_yellow", NETCABLE.get(), NETCABLE_YELLOW.get(), CableColor.YELLOW))
                                    .withPool(DataGenHelper.getLootTableEntry("cable_routing", NETCABLE.get(), NETCABLE_ROUTING.get(), CableColor.ROUTING)));
                        })
                        .stonePickaxeTags(),

                Dob.itemBuilder(NETCABLE_BLUE)
                        .itemTags(List.of(TAG_KEY_CABLES))
                        .shaped(builder -> builder
                                        .define('g', net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:nuggets/gold")))
                                        .define('s', Items.STRING)
                                        .define('1', net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:dyes/blue")))
                                        .unlockedBy("nugget", has(Items.GOLD_NUGGET)),
                                16,
                                "s1s", "rgr", "srs")
                        .shapeless("netcable_blue_dye", builder -> builder
                                .requires(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:dyes/blue")))
                                .requires(CableModule.TAG_KEY_CABLES)
                                .unlockedBy("chest", has(Items.CHEST))),
                Dob.itemBuilder(NETCABLE_GREEN)
                        .itemTags(List.of(TAG_KEY_CABLES))
                        .shaped(builder -> builder
                                        .define('g', net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:nuggets/gold")))
                                        .define('s', Items.STRING)
                                        .define('1', net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:dyes/green")))
                                        .unlockedBy("nugget", has(Items.GOLD_NUGGET)),
                                16,
                                "s1s", "rgr", "srs")
                        .shapeless("netcable_green_dye", builder -> builder
                                .requires(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:dyes/green")))
                                .requires(CableModule.TAG_KEY_CABLES)
                                .unlockedBy("chest", has(Items.CHEST))),
                Dob.itemBuilder(NETCABLE_RED)
                        .itemTags(List.of(TAG_KEY_CABLES))
                        .shaped(builder -> builder
                                        .define('g', net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:nuggets/gold")))
                                        .define('s', Items.STRING)
                                        .define('1', net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:dyes/red")))
                                        .unlockedBy("nugget", has(Items.GOLD_NUGGET)),
                                16,
                                "s1s", "rgr", "srs")
                        .shapeless("netcable_red_dye", builder -> builder
                                .requires(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:dyes/red")))
                                .requires(CableModule.TAG_KEY_CABLES)
                                .unlockedBy("chest", has(Items.CHEST))),
                Dob.itemBuilder(NETCABLE_YELLOW)
                        .itemTags(List.of(TAG_KEY_CABLES))
                        .shaped(builder -> builder
                                        .define('g', net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:nuggets/gold")))
                                        .define('s', Items.STRING)
                                        .define('1', net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:dyes/yellow")))
                                        .unlockedBy("nugget", has(Items.GOLD_NUGGET)),
                                16,
                                "s1s", "rgr", "srs")
                        .shapeless("netcable_yellow_dye", builder -> builder
                                .requires(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:dyes/yellow")))
                                .requires(CableModule.TAG_KEY_CABLES)
                                .unlockedBy("chest", has(Items.CHEST))),
                Dob.itemBuilder(NETCABLE_ROUTING)
                        .itemTags(List.of(TAG_KEY_CABLES))
                        .shaped(builder -> builder
                                        .define('g', net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:nuggets/gold")))
                                        .define('s', Items.STRING)
                                        .define('1', net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:dyes/black")))
                                        .unlockedBy("nugget", has(Items.GOLD_NUGGET)),
                                32,
                                "s1s", "rgr", "srs"),
                Dob.itemBuilder(CONNECTOR_BLUE)
                        .itemTags(List.of(TAG_KEY_CONNECTORS))
                        .shaped(builder -> builder
                                        .define('g', net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:ingots/gold")))
                                        .define('1', net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:dyes/blue")))
                                        .define('C', net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:chests")))
                                        .unlockedBy("chest", has(Items.CHEST)),
                                "1C1", "rgr", "1r1")
                        .shapeless("connector_blue_dye", builder -> builder
                                .requires(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:dyes/blue")))
                                .requires(CableModule.TAG_KEY_CONNECTORS)
                                .unlockedBy("chest", has(Items.CHEST))),
                Dob.itemBuilder(CONNECTOR_GREEN)
                        .itemTags(List.of(TAG_KEY_CONNECTORS))
                        .shaped(builder -> builder
                                        .define('g', net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:ingots/gold")))
                                        .define('1', net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:dyes/green")))
                                        .define('C', net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:chests")))
                                        .unlockedBy("chest", has(Items.CHEST)),
                                "1C1", "rgr", "1r1")
                        .shapeless("connector_green_dye", builder -> builder
                                .requires(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:dyes/green")))
                                .requires(CableModule.TAG_KEY_CONNECTORS)
                                .unlockedBy("chest", has(Items.CHEST))),
                Dob.itemBuilder(CONNECTOR_RED)
                        .itemTags(List.of(TAG_KEY_CONNECTORS))
                        .shaped(builder -> builder
                                        .define('g', net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:ingots/gold")))
                                        .define('1', net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:dyes/red")))
                                        .define('C', net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:chests")))
                                        .unlockedBy("chest", has(Items.CHEST)),
                                "1C1", "rgr", "1r1")
                        .shapeless("connector_red_dye", builder -> builder
                                .requires(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:dyes/red")))
                                .requires(CableModule.TAG_KEY_CONNECTORS)
                                .unlockedBy("chest", has(Items.CHEST))),
                Dob.itemBuilder(CONNECTOR_YELLOW)
                        .itemTags(List.of(TAG_KEY_CONNECTORS))
                        .shaped(builder -> builder
                                        .define('g', net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:ingots/gold")))
                                        .define('1', net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:dyes/yellow")))
                                        .define('C', net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:chests")))
                                        .unlockedBy("chest", has(Items.CHEST)),
                                "1C1", "rgr", "1r1")
                        .shapeless("connector_yellow_dye", builder -> builder
                                .requires(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:dyes/yellow")))
                                .requires(CableModule.TAG_KEY_CONNECTORS)
                                .unlockedBy("chest", has(Items.CHEST))),
                Dob.itemBuilder(CONNECTOR_ROUTING)
                        .itemTags(List.of(TAG_KEY_CONNECTORS))
                        .shaped(builder -> builder
                                        .define('g', net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:nuggets/gold")))
                                        .define('C', TAG_KEY_CONNECTORS)
                                        .unlockedBy("chest", has(Items.CHEST)),
                                "rrr", "gCg", "rrr"),
                Dob.itemBuilder(ADVANCED_CONNECTOR_BLUE)
                        .itemTags(List.of(TAG_KEY_ADVANCED_CONNECTORS))
                        .shaped(builder -> builder
                                        .define('C', CONNECTOR_BLUE.get())
                                        .unlockedBy("chest", has(Items.CHEST)),
                                "Co", "dr")
                        .shapeless("advanced_connector_blue_dye", builder -> builder
                                .requires(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:dyes/blue")))
                                .requires(CableModule.TAG_KEY_ADVANCED_CONNECTORS)
                                .unlockedBy("chest", has(Items.CHEST))),
                Dob.itemBuilder(ADVANCED_CONNECTOR_GREEN)
                        .itemTags(List.of(TAG_KEY_ADVANCED_CONNECTORS))
                        .shaped(builder -> builder
                                        .define('C', CONNECTOR_GREEN.get())
                                        .unlockedBy("chest", has(Items.CHEST)),
                                "Co", "dr")
                        .shapeless("advanced_connector_green_dye", builder -> builder
                                .requires(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:dyes/green")))
                                .requires(CableModule.TAG_KEY_ADVANCED_CONNECTORS)
                                .unlockedBy("chest", has(Items.CHEST))),
                Dob.itemBuilder(ADVANCED_CONNECTOR_RED)
                        .itemTags(List.of(TAG_KEY_ADVANCED_CONNECTORS))
                        .shaped(builder -> builder
                                        .define('C', CONNECTOR_RED.get())
                                        .unlockedBy("chest", has(Items.CHEST)),
                                "Co", "dr")
                        .shapeless("advanced_connector_red_dye", builder -> builder
                                .requires(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:dyes/red")))
                                .requires(CableModule.TAG_KEY_ADVANCED_CONNECTORS)
                                .unlockedBy("chest", has(Items.CHEST))),
                Dob.itemBuilder(ADVANCED_CONNECTOR_YELLOW)
                        .itemTags(List.of(TAG_KEY_ADVANCED_CONNECTORS))
                        .shaped(builder -> builder
                                        .define('C', CONNECTOR_YELLOW.get())
                                        .unlockedBy("chest", has(Items.CHEST)),
                                "Co", "dr")
                        .shapeless("advanced_connector_yellow_dye", builder -> builder
                                .requires(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.parse("c:dyes/yellow")))
                                .requires(CableModule.TAG_KEY_ADVANCED_CONNECTORS)
                                .unlockedBy("chest", has(Items.CHEST))),
                Dob.itemBuilder(ADVANCED_CONNECTOR_ROUTING)
                        .itemTags(List.of(TAG_KEY_ADVANCED_CONNECTORS))
                        .shaped(builder -> builder
                                        .define('C', CONNECTOR_ROUTING.get())
                                        .unlockedBy("chest", has(Items.CHEST)),
                                "Co", "dr")
        );
    }

}
