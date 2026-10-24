package mcjty.xnet.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IntPosTest {
    @Test void negativePositionsSurviveNbtRoundTrip() {
        var original = new IntPos(new BlockPos(-33, -64, -17));
        var saved = new CompoundTag();
        saved.putInt("position", original.pos());
        var restored = new IntPos(saved.getIntOr("position", 0));
        assertEquals(15, restored.getX());
        assertEquals(-64, restored.getY());
        assertEquals(15, restored.getZ());
        assertEquals(original, restored);
    }

    @Test void encodedHeightsRetainTheExistingSaveFormat() {
        for (int height : new int[]{ -32768, -64, -1, 0, 319, 32767 }) {
            var local = new IntPos(new BlockPos(13, height, 7));
            assertEquals(height, local.getY());
            assertEquals(13, local.getX());
            assertEquals(7, local.getZ());
        }
    }

    @Test void crossingAChunkBorderPreservesHeight() {
        var northEdge = new IntPos(new BlockPos(5, -48, 0));
        assertTrue(northEdge.isBorder(Direction.NORTH));
        var adjacent = northEdge.otherSide(Direction.NORTH);
        assertEquals(5, adjacent.getX());
        assertEquals(15, adjacent.getZ());
        assertEquals(-48, adjacent.getY());
    }
}
