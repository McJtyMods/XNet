package mcjty.xnet.modules.cables;

import mcjty.xnet.modules.cables.blocks.GenericCableBlock;
import net.minecraft.advancements.predicates.StatePropertiesPredicate;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

public class DataGenHelper {

    public static LootPool.Builder getLootTableEntry(String cableName, Block cableBlock, Item cable, CableColor color) {
        return LootPool.lootPool()
                .name(cableName)
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(cable))
                .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(cableBlock)
                        .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(GenericCableBlock.COLOR, color)));
    }

}
