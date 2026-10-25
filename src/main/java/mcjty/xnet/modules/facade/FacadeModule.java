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
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import mcjty.lib.setup.DeferredBlock;
import mcjty.lib.setup.DeferredHolder;
import mcjty.lib.setup.DeferredItem;

import java.util.function.Supplier;

import static mcjty.lib.datagen.DataGen.has;
import static mcjty.xnet.XNet.tab;
import static mcjty.xnet.apiimpl.Constants.ITEM_FACADE;
import static mcjty.xnet.setup.Registration.*;

public class FacadeModule implements IModule {

    public static final DeferredBlock<FacadeBlock> FACADE = BLOCKS.register(ITEM_FACADE, properties -> new FacadeBlock(GenericCableBlock.CableBlockType.FACADE, properties)); // @todo 1.14
    public static final DeferredItem<Item> FACADE_ITEM = ITEMS.register(ITEM_FACADE, tab(properties -> new FacadeBlockItem(FACADE.get(), properties.useBlockDescriptionPrefix())));
    public static final Supplier<BlockEntityType<?>> TYPE_FACADE = TILES.register(ITEM_FACADE, () -> new BlockEntityType<>(FacadeTileEntity::new, java.util.Set.of(FACADE.get())));

    public static final AttachmentType<MimicData> MIMIC_DATA = net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.create(
            net.minecraft.resources.Identifier.fromNamespaceAndPath("xnet", "mimic_data"), builder -> builder.initializer(() -> MimicData.EMPTY).persistent(MimicData.CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MimicData>> ITEM_MIMIC_DATA = COMPONENTS.registerComponentType(
            "mimic_data",
            builder -> builder
                    .persistent(MimicData.CODEC)
                    .networkSynchronized(MimicData.STREAM_CODEC));


    @Override
    public void init() {

    }

    @Override
    public void initClient() {
    }

    @Override
    public void initConfig() {

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
