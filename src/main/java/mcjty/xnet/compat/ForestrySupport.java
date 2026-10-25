package mcjty.xnet.compat;


import mcjty.lib.varia.Tools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ForestrySupport {
    public enum Tag {
    	GENOME("Genome", 1),	// Bees, Trees, Butterflies
    	MATE("Mate", 2),		// Bees, Butterflies
    	GEN("GEN", 4),			// Bees
    	HEALTH("Health", 8),	// Bees, Butterflies
    	IS_ANALYZED("IsAnalyzed", 16),	// Bees, Trees, Butterflies
    	MAX_HEALTH("MaxH", 32),	// Bees, Butterflies
    	AGE("Age", 64);			// Butterflies

    	private final String name;
    	private final int flag;

    	Tag(String name, int flag) {
    		this.name = name;
    		this.flag = flag;
    	}

    	public int getFlag() {
    		return flag;
    	}
    }

    private static final String ID = "forestry";

    private static final String QUEEN_BEE = "forestry:bee_queen_ge";
    private static final String PRINCESS_BEE = "forestry:bee_princess_ge";
    private static final String DRONE_BEE = "forestry:bee_drone_ge";
    private static final String LARVAE_BEE = "forestry:bee_larvae_ge";

    private static final String SAPLING = "forestry:sapling";
    private static final String POLLEN = "forestry:pollen_fertile";

    private static final String BUTTERFLY = "forestry:butterfly_ge";
    private static final String SERUM = "forestry:serum_ge";
    private static final String CATERPILLAR = "forestry:caterpillar_ge";
    private static final String COCOON = "forestry:cocoon_ge";

    private static final String[] FORESTRY_NAMES = { QUEEN_BEE, PRINCESS_BEE, DRONE_BEE, LARVAE_BEE, SAPLING, POLLEN,
	    BUTTERFLY, SERUM, CATERPILLAR, COCOON };

    public static boolean isLoaded() {
	    return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(ID);
    }

    /**
     * Determines if the item is breedable in Forestry.
     *
     * @param item	item being determined if breedable
     * @return		true if breedable, else false
     */
    public static boolean isBreedable(ItemStack item) {
        String itemName = Tools.getId(item).toString();
        for (String forestryName : FORESTRY_NAMES) {
	        if (itemName.equals(forestryName)) {
	        	return true;
	        }
	    }
	    return false;
    }

    /**
     * Removes NBT tags based on the item to make it easier for comparison with
     * other similar items.
     *
     * @param item	item to remove NBT tags from
     * @return		the item with appropriate NBT tags removed
     */
    public static ItemStack sanitize(ItemStack item, int flags) {
		// @todo 1.21 NBT
//	    CompoundTag tagCompound = item.getTag().copy();
//	    List<Tag> tagsToRemove = new ArrayList<>();
//	    switch (Tools.getId(item).toString()) {
//	        case QUEEN_BEE:
//	        case PRINCESS_BEE:
//	            tagsToRemove.add(Tag.GEN);
//	            //$FALL-THROUGH$
//	        case DRONE_BEE:
//	        case LARVAE_BEE:
//	            Collections.addAll(tagsToRemove, Tag.GENOME, Tag.MATE, Tag.HEALTH, Tag.IS_ANALYZED, Tag.MAX_HEALTH);
//	            item.setTag(removeTags(tagsToRemove, tagCompound, flags));
//	            break;
//	        case SAPLING:
//	        case POLLEN:
//	            Collections.addAll(tagsToRemove, Tag.GENOME, Tag.IS_ANALYZED);
//	            item.setTag(removeTags(tagsToRemove, tagCompound, flags));
//	            break;
//	        case BUTTERFLY:
//	        case SERUM:
//	        case CATERPILLAR:
//	        case COCOON:
//	            Collections.addAll(tagsToRemove, Tag.GENOME, Tag.MATE, Tag.HEALTH, Tag.IS_ANALYZED, Tag.MAX_HEALTH, Tag.AGE);
//	            item.setTag(removeTags(tagsToRemove, tagCompound, flags));
//	            break;
//	        default:
//	            throw new IllegalArgumentException("Tried to sanitize \"" + Tools.getId(item).toString() + "\" for Forestry!");
//	    }
	    return item;
    }

    private static CompoundTag removeTags(Iterable<Tag> tagsToRemove, CompoundTag compound, int flags) {
	    for (Tag tag : tagsToRemove) {
	        if ((flags & tag.flag) == tag.flag && compound.contains(tag.name)) {
		        compound.remove(tag.name);
	        }
	    }
	    return compound;
    }
}
