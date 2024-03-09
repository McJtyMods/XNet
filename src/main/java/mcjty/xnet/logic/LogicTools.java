package mcjty.xnet.logic;

import mcjty.lib.varia.LevelTools;
import mcjty.lib.varia.OrientationTools;
import mcjty.rftoolsbase.api.xnet.keys.NetworkId;
import mcjty.xnet.modules.cables.CableColor;
import mcjty.xnet.modules.cables.blocks.ConnectorBlock;
import mcjty.xnet.modules.cables.blocks.GenericCableBlock;
import mcjty.xnet.modules.controller.blocks.TileEntityController;
import mcjty.xnet.modules.router.blocks.TileEntityRouter;
import mcjty.xnet.modules.wireless.blocks.TileEntityWirelessRouter;
import mcjty.xnet.multiblock.WorldBlob;
import mcjty.xnet.multiblock.XNetBlobData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class LogicTools {

    // Find the controller attached to this connector.
    @Nullable
    public static TileEntityController getControllerForConnector(@Nonnull Level world, @Nonnull BlockPos connectorPos) {
        BlockPos controllerPos = getControllerPosForConnector(world, connectorPos);
        if (controllerPos == null) {
            return null;
        }
        if (!LevelTools.isLoaded(world, controllerPos)) {
            return null;
        }
        BlockEntity te = world.getBlockEntity(controllerPos);
        if (te instanceof TileEntityController) {
            return (TileEntityController) te;
        } else {
            return null;
        }
    }

    @Nullable
    public static BlockPos getControllerPosForConnector(@Nonnull Level world, @Nonnull BlockPos connectorPos) {
        WorldBlob worldBlob = XNetBlobData.get(world).getWorldBlob(world);
        NetworkId networkId = worldBlob.getNetworkAt(connectorPos);
        if (networkId == null) {
            return null;
        }
        return worldBlob.getProviderPosition(networkId);
    }

    // All consumers for a given network
    public static Set<BlockPos> consumers(@Nonnull Level world, @Nonnull NetworkId networkId) {
        WorldBlob worldBlob = XNetBlobData.get(world).getWorldBlob(world);
        return worldBlob.getConsumers(networkId);
    }

    // All normal connectors for a given position
    public static void forEachConnector(@Nonnull Level world, @Nonnull BlockPos pos, Consumer<BlockPos> consumer) {
        for (Direction direction : OrientationTools.DIRECTION_VALUES) {
            BlockPos connectorPos = pos.relative(direction);
            BlockState state = world.getBlockState(connectorPos);
            if (state.getBlock() instanceof ConnectorBlock) {
                CableColor color = state.getValue(GenericCableBlock.COLOR);
                if ((color != CableColor.ROUTING)) {
                    consumer.accept(connectorPos);
                }
            }
        }
    }

    // All routing connectors for a given position
    @Nullable
    public static <T> T findRoutingConnector(@Nonnull Level world, @Nonnull BlockPos pos, Function<BlockPos, T> consumer) {
        for (Direction direction : OrientationTools.DIRECTION_VALUES) {
            BlockPos connectorPos = pos.relative(direction);
            BlockState state = world.getBlockState(connectorPos);
            if (state.getBlock() instanceof ConnectorBlock) {
                CableColor color = state.getValue(GenericCableBlock.COLOR);
                if ((color == CableColor.ROUTING)) {
                    T result = consumer.apply(connectorPos);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return null;
    }

    // All routers from a given position
    public static void forEachRouter(@Nonnull Level world, @Nonnull BlockPos pos, Consumer<TileEntityRouter> consumer) {
        for (Direction direction : OrientationTools.DIRECTION_VALUES) {
            if (world.getBlockEntity(pos.relative(direction)) instanceof TileEntityRouter router) {
                consumer.accept(router);
            }
        }
    }

    // All wireless routers from a given position
    public static void forEachWirelessRouter(@Nonnull Level world, @Nonnull BlockPos pos, Consumer<TileEntityWirelessRouter> consumer) {
        for (Direction direction : OrientationTools.DIRECTION_VALUES) {
            if (world.getBlockEntity(pos.relative(direction)) instanceof TileEntityWirelessRouter router) {
                consumer.accept(router);
            }
        }
    }

    public static boolean findWirelessRouter(@Nonnull Level world, @Nonnull BlockPos pos, Predicate<TileEntityWirelessRouter> predicate) {
        for (Direction direction : OrientationTools.DIRECTION_VALUES) {
            if (world.getBlockEntity(pos.relative(direction)) instanceof TileEntityWirelessRouter router) {
                if (predicate.test(router)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Return all routers connected to a network
    public static void forEachRouter(@Nonnull Level world, @Nonnull NetworkId networkId, Consumer<TileEntityRouter> consumer) {
        Set<BlockPos> consumers = consumers(world, networkId);
        for (BlockPos pos : consumers) {
            for (Direction direction : OrientationTools.DIRECTION_VALUES) {
                if (ConnectorBlock.isConnectable(world, pos, direction)) {
                    BlockPos p = pos.relative(direction);
                    if (world.getBlockEntity(p) instanceof TileEntityRouter router) {
                        consumer.accept(router);
                    }
                }
            }
        }
    }

    public static boolean[] intToBinary(int value) {
        boolean[] bits = new boolean[16];
        for (int i = 15; i >= 0; i--) {
            bits[i] = (value & (1 << i)) != 0;
        }
        return bits;
    }
}
