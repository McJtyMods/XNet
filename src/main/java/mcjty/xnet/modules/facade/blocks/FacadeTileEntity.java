package mcjty.xnet.modules.facade.blocks;

import mcjty.lib.tileentity.GenericTileEntity;
import mcjty.xnet.modules.cables.blocks.GenericCableBlock;
import mcjty.xnet.modules.facade.FacadeModule;
import mcjty.xnet.modules.facade.IFacadeSupport;
import mcjty.xnet.modules.facade.MimicBlockSupport;
import mcjty.xnet.modules.facade.data.MimicData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

public class FacadeTileEntity extends GenericTileEntity implements IFacadeSupport {

    public FacadeTileEntity(BlockPos pos, BlockState state) {
        super(FacadeModule.TYPE_FACADE.get(), pos, state);
    }

    @Override
    public void onDataPacket(Connection net, net.minecraft.world.level.storage.ValueInput input) {
        super.onDataPacket(net, input);

        if (level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            // Dynamic models read the synchronized block entity when rebuilding the section.
        }
    }


    @Override
    public BlockState getMimicBlock() {
        // Packet serialization must not create attachments and queue another chunk update.
        MimicData data = getExistingDataOrNull(FacadeModule.MIMIC_DATA.get());
        BlockState state = data == null ? null : data.state();
        if (state == null || state.isAir()) {
            return null;
        }
        return state;
    }


    public void setMimicBlock(@Nonnull BlockState mimicBlock) {
        setData(FacadeModule.MIMIC_DATA, new MimicData(mimicBlock));
        markDirtyClient();
    }

    @Override
    public void saveClientDataToNBT(CompoundTag tagCompound, HolderLookup.Provider provider) {
        MimicBlockSupport.writeToNBT(tagCompound, getMimicBlock());
    }

    @Override
    public void loadClientDataFromNBT(CompoundTag tagCompound, HolderLookup.Provider provider) {
        BlockState state = MimicBlockSupport.readFromNBT(provider, tagCompound);
        if (state == null) {
            state = Blocks.AIR.defaultBlockState();
        }
        setData(FacadeModule.MIMIC_DATA, new MimicData(state));
    }
}
