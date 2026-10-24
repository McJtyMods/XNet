package mcjty.xnet.modules.facade;

import mcjty.lib.datagen.DataGen;
import mcjty.lib.datagen.Dob;
import mcjty.lib.modules.IModule;
import mcjty.xnet.modules.cables.blocks.GenericCableBlock;
import mcjty.xnet.modules.facade.blocks.FacadeBlock;
import mcjty.xnet.modules.facade.blocks.FacadeBlockItem;
import mcjty.xnet.modules.facade.blocks.FacadeTileEntity;
import mcjty.xnet.modules.facade.data.MimicData;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Supplier;

import static mcjty.lib.datagen.DataGen.has;
import static mcjty.xnet.XNet.tab;
import static mcjty.xnet.apiimpl.Constants.ITEM_FACADE;
import static mcjty.xnet.setup.Registration.*;

public class FacadeModule implements IModule {

    public static final DeferredBlock<FacadeBlock> FACADE = BLOCKS.register(ITEM_FACADE, properties -> new FacadeBlock(GenericCableBlock.CableBlockType.FACADE, properties)); // @todo 1.14
    public static final DeferredItem<Item> FACADE_ITEM = ITEMS.register(ITEM_FACADE, tab(properties -> new FacadeBlockItem(FACADE.get(), properties)));
    public static final Supplier<BlockEntityType<?>> TYPE_FACADE = TILES.register(ITEM_FACADE, () -> new BlockEntityType<>(FacadeTileEntity::new, java.util.Set.of(FACADE.get())));

    public static final Supplier<AttachmentType<MimicData>> MIMIC_DATA = ATTACHMENT_TYPES.register(
            "mimic_data", () -> AttachmentType.builder(() -> MimicData.EMPTY)
                    .serialize(com.mojang.serialization.MapCodec.assumeMapUnsafe(MimicData.CODEC))
                    .build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MimicData>> ITEM_MIMIC_DATA = COMPONENTS.registerComponentType(
            "mimic_data",
            builder -> builder
                    .persistent(MimicData.CODEC)
                    .networkSynchronized(MimicData.STREAM_CODEC));


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
