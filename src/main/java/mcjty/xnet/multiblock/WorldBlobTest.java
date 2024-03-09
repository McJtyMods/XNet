package mcjty.xnet.multiblock;

import mcjty.rftoolsbase.api.xnet.keys.NetworkId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

public class WorldBlobTest {

    private static void createCableLine(WorldBlob world, BlockPos p1, Direction direction, int count, ColorId color) {
        for (int i = 0 ; i < count ; i++) {
            world.createCableSegment(p1, color);
            p1 = p1.relative(direction);
        }
    }


    public static void main(String[] args) {
        ColorId color1 = new ColorId(111);
        ColorId color2 = new ColorId(222);
        ColorId color3 = new ColorId(333);

        WorldBlob world = new WorldBlob(Level.OVERWORLD);

        BlockPos p1 = new BlockPos(10, 60, 10);
        world.createNetworkProvider(p1, color1, new NetworkId(1000));
        createCableLine(world, p1.east(), Direction.EAST, 20, color1);

        BlockPos p2 = new BlockPos(50, 61, 10);
        world.createNetworkProvider(p2, color2, new NetworkId(2000));
        createCableLine(world, p2.west(), Direction.WEST, 50, color2);

        BlockPos p3 = new BlockPos(50, 30, 1000);
        createCableLine(world, p3, Direction.UP, 5, color3);
        createCableLine(world, p3.east().east(), Direction.UP, 5, color3);
        world.createNetworkProvider(p3.north(), color3, new NetworkId(3000));
        world.createCableSegment(p3.east(), color3);

        world.dump();

        System.out.println("------------------------------------------------------------");
        System.out.println("------------------------------------------------------------");
        System.out.println("------------------------------------------------------------");

        world.removeCableSegment(new BlockPos(30, 61, 10));

        System.out.println("------------------------------------------------------------");
        System.out.println("------------------------------------------------------------");
        System.out.println("------------------------------------------------------------");
        world.dump();

        CompoundTag compound = new CompoundTag();
        world.writeToNBT(compound);

        world = new WorldBlob(Level.OVERWORLD);
        world.readFromNBT(compound);

        System.out.println("------------------------------------------------------------");
        System.out.println("------------------------------------------------------------");
        System.out.println("------------------------------------------------------------");
        world.dump();
    }
}
