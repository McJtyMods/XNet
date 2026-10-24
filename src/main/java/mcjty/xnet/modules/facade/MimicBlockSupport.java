package mcjty.xnet.modules.facade;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.state.BlockState;

import static mcjty.xnet.apiimpl.Constants.TAG_MIMIC;

public class MimicBlockSupport {

    public static BlockState readFromNBT(HolderLookup.Provider provider, CompoundTag tagCompound) {
        BlockState mimicBlock;
        if (tagCompound != null && tagCompound.contains("mimic")) {
            mimicBlock = NbtUtils.readBlockState(provider.lookup(Registries.BLOCK).get(), tagCompound.getCompoundOrEmpty("mimic"));
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
