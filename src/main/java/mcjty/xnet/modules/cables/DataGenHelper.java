package mcjty.xnet.modules.cables;

import mcjty.xnet.modules.cables.blocks.GenericCableBlock;
import net.minecraft.advancements.predicates.StatePropertiesPredicate;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.MatchBlock;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

public class DataGenHelper {

    public static LootPool.Builder getLootTableEntry(String cableName, Block cableBlock, Item cable, CableColor color) {
        return LootPool.lootPool()
                .setRolls(ContextIntProviders.exactly(1))
                .add(LootItem.lootTableItem(cable))
                .when(MatchBlock.blockMatches(net.minecraft.core.registries.BuiltInRegistries.BLOCK, cableBlock,
                        StatePropertiesPredicate.Builder.properties().hasProperty(GenericCableBlock.COLOR, color)));
    }

}
