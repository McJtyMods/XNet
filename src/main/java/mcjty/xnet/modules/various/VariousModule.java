package mcjty.xnet.modules.various;

import mcjty.lib.datagen.DataGen;
import mcjty.lib.datagen.Dob;
import mcjty.lib.modules.IModule;
import mcjty.xnet.modules.various.blocks.RedstoneProxyBlock;
import mcjty.xnet.modules.various.blocks.RedstoneProxyUBlock;
import mcjty.xnet.modules.various.items.ConnectorUpgradeItem;
import mcjty.xnet.setup.Registration;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import static mcjty.lib.datagen.DataGen.has;
import static mcjty.xnet.XNet.tab;
import static mcjty.xnet.setup.Registration.BLOCKS;
import static mcjty.xnet.setup.Registration.ITEMS;
import static mcjty.lib.datagen.model.ModelProvider.BLOCK_FOLDER;

public class VariousModule implements IModule {

    public static final DeferredBlock<RedstoneProxyBlock> REDSTONE_PROXY = BLOCKS.register("redstone_proxy", RedstoneProxyBlock::new);
    public static final DeferredBlock<RedstoneProxyUBlock> REDSTONE_PROXY_UPD = BLOCKS.register("redstone_proxy_upd", RedstoneProxyUBlock::new);
    public static final DeferredItem<Item> REDSTONE_PROXY_ITEM = ITEMS.register("redstone_proxy", tab(properties -> new mcjty.xnet.modules.cables.blocks.TooltipBlockItem(REDSTONE_PROXY.get(), properties.useBlockDescriptionPrefix())));
    public static final DeferredItem<Item> REDSTONE_PROXY_UPD_ITEM = ITEMS.register("redstone_proxy_upd", tab(properties -> new mcjty.xnet.modules.cables.blocks.TooltipBlockItem(REDSTONE_PROXY_UPD.get(), properties.useBlockDescriptionPrefix())));

    public static final DeferredItem<ConnectorUpgradeItem> UPGRADE = ITEMS.register("connector_upgrade", ConnectorUpgradeItem::new);

    @Override
    public void init(FMLCommonSetupEvent event) {

    }

    @Override
    public void initClient(FMLClientSetupEvent event) {

    }

    @Override
    public void initConfig(IEventBus bus) {

    }

    @Override
    public void initDatagen(DataGen dataGen, HolderLookup.Provider provider) {
        dataGen.add(
                Dob.blockBuilder(REDSTONE_PROXY)
                        .stonePickaxeTags()
                        .simpleLoot()
                        .parentedItem("block/redstone_proxy")
                        .blockState(p -> p.singleTextureBlock(REDSTONE_PROXY.get(), BLOCK_FOLDER + "/redstone_proxy", "block/machine_proxy"))
                        .shaped(builder -> builder
                                        .define('F', mcjty.rftoolsbase.modules.various.VariousModule.MACHINE_FRAME.get())
                                        .unlockedBy("frame", has(mcjty.rftoolsbase.modules.various.VariousModule.MACHINE_FRAME.get())),
                                "rrr", "rFr", "rrr"),
                Dob.blockBuilder(REDSTONE_PROXY_UPD)
                        .stonePickaxeTags()
                        .simpleLoot()
                        .parentedItem("block/redstone_proxy_upd")
                        .blockState(p -> p.singleTextureBlock(REDSTONE_PROXY_UPD.get(), BLOCK_FOLDER + "/redstone_proxy_upd", "block/machine_proxy"))
                        .shapeless("redstoneproxy_update", builder -> builder
                                .requires(VariousModule.REDSTONE_PROXY.get())
                                .requires(Items.REDSTONE_TORCH)
                                .unlockedBy("torch", has(Items.REDSTONE_TORCH))),
                Dob.itemBuilder(UPGRADE)
                        .shaped(builder -> builder
                                        .unlockedBy("pearl", has(Items.ENDER_PEARL)),
                                "po", "dr")
        );
    }
}
