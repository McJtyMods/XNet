package mcjty.xnet.modules.facade;

import mcjty.lib.datagen.DataGen;
import mcjty.lib.datagen.Dob;
import mcjty.lib.modules.IModule;
import mcjty.xnet.modules.cables.blocks.GenericCableBlock;
import mcjty.xnet.modules.facade.blocks.FacadeBlock;
import mcjty.xnet.modules.facade.blocks.FacadeBlockItem;
import mcjty.xnet.modules.facade.blocks.FacadeTileEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Supplier;

import static mcjty.lib.datagen.DataGen.has;
import static mcjty.xnet.XNet.tab;
import static mcjty.xnet.setup.Registration.*;

public class FacadeModule implements IModule {

    public static final DeferredBlock<FacadeBlock> FACADE = BLOCKS.register("facade", () -> new FacadeBlock(GenericCableBlock.CableBlockType.FACADE)); // @todo 1.14
    public static final DeferredItem<Item> FACADE_ITEM = ITEMS.register("facade", tab(() -> new FacadeBlockItem(FACADE.get())));
    public static final Supplier<BlockEntityType<?>> TYPE_FACADE = TILES.register("facade", () -> BlockEntityType.Builder.of(FacadeTileEntity::new, FACADE.get()).build(null));

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
                Dob.blockBuilder(FACADE)
                        .ironPickaxeTags()
                        .shaped(builder -> builder
                                        .define('w', ItemTags.WOOL)
                                        .unlockedBy("glass", has(Items.GLASS)),
                                16,
                                "pwp", "wGw", "pwp")
        );
    }
}
