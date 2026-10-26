package mcjty.xnet.modules.facade;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.state.BlockState;

import static mcjty.xnet.apiimpl.Constants.TAG_MIMIC;

public class MimicBlockSupport {

    // 26.3 renamed block-state fields. Mod components and custom NBT do not get vanilla data fixes.
    public static final Codec<BlockState> BLOCK_STATE_CODEC = Codec.withAlternative(BlockState.CODEC,
            CompoundTag.CODEC.comapFlatMap(tag -> BlockState.CODEC.parse(NbtOps.INSTANCE, currentStateTag(tag)), NbtUtils::writeBlockState));

    private static CompoundTag currentStateTag(CompoundTag tag) {
        if (!tag.contains("Name")) return tag;
        CompoundTag converted = tag.copy();
        converted.put("id", converted.get("Name"));
        converted.remove("Name");
        if (converted.contains("Properties")) {
            converted.put("properties", converted.get("Properties"));
            converted.remove("Properties");
        }
        return converted;
    }

    public static BlockState readFromNBT(HolderLookup.Provider provider, CompoundTag tagCompound) {
        BlockState mimicBlock;
        if (tagCompound != null && tagCompound.contains("mimic")) {
            mimicBlock = NbtUtils.readBlockState(provider.lookup(Registries.BLOCK).get(), currentStateTag(tagCompound.getCompoundOrEmpty("mimic")));
        } else {
            mimicBlock = null;
        }
        return mimicBlock;
    }

    public static void writeToNBT(CompoundTag tagCompound, BlockState mimicBlock) {
        if (mimicBlock != null) {
            CompoundTag tag = NbtUtils.writeBlockState(mimicBlock);
            tagCompound.put(TAG_MIMIC, tag);
        }
    }
}
