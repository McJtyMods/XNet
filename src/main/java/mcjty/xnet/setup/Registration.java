package mcjty.xnet.setup;


import mcjty.lib.blocks.RBlockRegistry;
import mcjty.lib.setup.DeferredBlocks;
import mcjty.lib.setup.DeferredItems;
import mcjty.xnet.XNet;
import mcjty.xnet.modules.controller.ControllerModule;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import mcjty.lib.setup.DeferredRegister;

import java.util.function.Supplier;

import static mcjty.xnet.XNet.MODID;

public class Registration {

    public static final RBlockRegistry RBLOCKS = new RBlockRegistry(XNet.MODID, XNet.setup::addTabItem);
    public static final DeferredBlocks BLOCKS = DeferredBlocks.create(MODID);
    public static final DeferredItems ITEMS = DeferredItems.create(MODID);
    public static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);

    public static Supplier<CreativeModeTab> TAB = TABS.register("xnet", () -> net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + MODID))
            .icon(() -> new ItemStack(ControllerModule.CONTROLLER.block().get()))
            .displayItems((featureFlags, output) -> {
                XNet.setup.populateTab(output);
            })
            .build());

    public static void register() {
        RBLOCKS.register();
        BLOCKS.register();
        ITEMS.register();
        TILES.register();
        CONTAINERS.register();
        SOUNDS.register();
        ENTITIES.register();
        TABS.register();
        COMPONENTS.register();
    }

    public static Item.Properties createStandardProperties() {
        return XNet.setup.defaultProperties();
    }
}
