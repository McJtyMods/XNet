package mcjty.xnet.modules.facade.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import java.util.Objects;

public record MimicData(@Nonnull BlockState state) {

    public static final MimicData EMPTY = new MimicData(Blocks.AIR.defaultBlockState());

    public MimicData(@Nonnull BlockState state) {
        this.state = Objects.requireNonNull(state);
    }

    public static final Codec<MimicData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            mcjty.xnet.modules.facade.MimicBlockSupport.BLOCK_STATE_CODEC.fieldOf("state").forGetter(MimicData::state)
    ).apply(instance, MimicData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MimicData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY), MimicData::state,
            MimicData::new
    );
}
