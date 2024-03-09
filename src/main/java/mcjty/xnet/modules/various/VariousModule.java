package mcjty.xnet.modules.various;

import mcjty.lib.datagen.DataGen;
import mcjty.lib.datagen.Dob;
import mcjty.lib.modules.IModule;
import mcjty.xnet.modules.various.blocks.RedstoneProxyBlock;
import mcjty.xnet.modules.various.blocks.RedstoneProxyUBlock;
import mcjty.xnet.modules.various.items.ConnectorUpgradeItem;
import mcjty.xnet.setup.Registration;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.RegistryObject;

import static mcjty.lib.datagen.DataGen.has;
import static mcjty.xnet.XNet.tab;
import static mcjty.xnet.setup.Registration.BLOCKS;
import static mcjty.xnet.setup.Registration.ITEMS;
import static net.minecraftforge.client.model.generators.ModelProvider.BLOCK_FOLDER;

public class VariousModule implements IModule {

    public static final RegistryObject<RedstoneProxyBlock> REDSTONE_PROXY = BLOCKS.register("redstone_proxy", RedstoneProxyBlock::new);
    public static final RegistryObject<RedstoneProxyUBlock> REDSTONE_PROXY_UPD = BLOCKS.register("redstone_proxy_upd", RedstoneProxyUBlock::new);
    public static final RegistryObject<Item> REDSTONE_PROXY_ITEM = ITEMS.register("redstone_proxy", tab(() -> new BlockItem(REDSTONE_PROXY.get(), Registration.createStandardProperties())));
    public static final RegistryObject<Item> REDSTONE_PROXY_UPD_ITEM = ITEMS.register("redstone_proxy_upd", tab(() -> new BlockItem(REDSTONE_PROXY_UPD.get(), Registration.createStandardProperties())));

    public static final RegistryObject<ConnectorUpgradeItem> UPGRADE = ITEMS.register("connector_upgrade", ConnectorUpgradeItem::new);

    @Override
    public void init(FMLCommonSetupEvent event) {

    }

    @Override
    public void initClient(FMLClientSetupEvent event) {

    }

    @Override
    public void initConfig() {

    }

    @Override
    public void initDatagen(DataGen dataGen) {
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
