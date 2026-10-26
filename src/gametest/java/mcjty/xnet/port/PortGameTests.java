package mcjty.xnet.port;

import java.lang.reflect.Proxy;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import mcjty.rftoolsbase.api.xnet.channels.IControllerContext;
import mcjty.rftoolsbase.api.xnet.helper.AbstractConnectorSettings;
import mcjty.rftoolsbase.api.xnet.keys.ConsumerId;
import mcjty.rftoolsbase.api.xnet.keys.SidedConsumer;
import mcjty.rftoolsbase.modules.tablet.TabletModule;
import mcjty.rftoolsbase.modules.tablet.items.TabletItem;
import mcjty.rftoolsbase.modules.tablet.items.TabletItemHandler;
import mcjty.xnet.apiimpl.ConnectedEntity;
import mcjty.xnet.apiimpl.enums.InsExtMode;
import mcjty.xnet.apiimpl.fluids.FluidChannelSettings;
import mcjty.xnet.apiimpl.fluids.FluidConnectorSettings;
import mcjty.xnet.apiimpl.items.ItemChannelSettings;
import mcjty.xnet.apiimpl.items.ItemConnectorSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Exercise production channel code with real NeoForge handlers and loaded block capabilities. */
public final class PortGameTests {
    static final Map<BlockEntity, ResourceHandler<FluidResource>> tanks = new IdentityHashMap<>();

    public static void partialItemTransfer(GameTestHelper test) {
        itemTransfer(test, false);
    }

    public static void refusedItemExtraction(GameTestHelper test) {
        itemTransfer(test, true);
    }

    private static void itemTransfer(GameTestHelper test, boolean exactExtractionOnly) {
        var destination = barrel(test);
        for (int i = 0; i < destination.getContainerSize(); i++) destination.setItem(i, new ItemStack(Items.IRON_INGOT, 64));
        destination.setItem(0, new ItemStack(Items.IRON_INGOT, 63));
        var iron = ItemResource.of(Items.IRON_INGOT);
        var source = new ItemStacksResourceHandler(1) {
            @Override public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
                return exactExtractionOnly && amount != 64 ? 0 : super.extract(index, resource, amount, transaction);
            }
        };
        source.set(0, iron, 64);
        var channel = new ItemChannelSettings();
        var settings = new ItemConnectorSettings(AbstractConnectorSettings.DEFAULT_SETTINGS, Direction.UP);
        set(channel, "itemConsumers", List.of(consumer(destination, settings)));
        set(channel, "itemExtractors", List.of());
        set(channel, "consumerFull", new boolean[1]);
        int remaining = (int) invoke(channel, "insertStack",
                new Class<?>[]{IControllerContext.class, ItemStack.class, Level.class, int.class, ResourceHandler.class},
                context(test), iron.toStack(64), test.getLevel(), 0, source);
        int moved = exactExtractionOnly ? 0 : 1;
        test.assertTrue(remaining == 64 - moved, "Channel reported the wrong remainder");
        test.assertTrue(source.getAmountAsLong(0) == 64 - moved, "Source extraction did not match insertion");
        test.assertTrue(destination.getItem(0).getCount() == 63 + moved, "Partial insertion created or lost items");
        test.succeed();
    }

    public static void fullFluidDestination(GameTestHelper test) {
        fluidTransfer(test, 400, null, null, 0);
    }

    public static void partialFluidTransfer(GameTestHelper test) {
        fluidTransfer(test, 350, null, null, 50);
    }

    public static void fluidLimits(GameTestHelper test) {
        fluidTransfer(test, 0, 450, 30, 30);
    }

    private static void fluidTransfer(GameTestHelper test, int stored, Integer keep, Integer maximum, int moved) {
        var destination = barrel(test);
        var water = FluidResource.of(Fluids.WATER);
        var source = tank(1000);
        source.set(0, water, 500);
        var target = tank(400);
        target.set(0, water, stored);
        tanks.put(destination, target);
        try {
            var channel = new FluidChannelSettings();
            set(channel, "fluidConsumers", List.of(consumer(destination, fluidSettings(maximum))));
            invoke(channel, "tickFluidHandler",
                    new Class<?>[]{IControllerContext.class, FluidConnectorSettings.class, ResourceHandler.class},
                    context(test), fluidSettings(keep), source);
            test.assertTrue(source.getAmountAsLong(0) == 500 - moved, "Fluid source did not retain the unaccepted amount");
            test.assertTrue(target.getAmountAsLong(0) == stored + moved, "Fluid destination has the wrong amount");
        } finally {
            tanks.remove(destination);
        }
        test.succeed();
    }

    public static void tabletTransactions(GameTestHelper test) {
        var player = test.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TabletModule.TABLET.get()));
        var handler = new TabletItemHandler(player);
        var card = ItemResource.of(TestRegistration.tabletCard);
        try (var transaction = Transaction.openRoot()) {
            test.assertTrue(handler.insert(0, card, 5, transaction) == 1, "Tablet slot must hold one card");
            test.assertTrue(TabletItem.getContainingItem(player.getMainHandItem(), 0).isEmpty(), "Uncommitted transfer changed the held tablet");
        }
        test.assertTrue(handler.getResource(0).isEmpty(), "Aborted insertion retained a card");
        try (var transaction = Transaction.openRoot()) {
            handler.insert(0, card, 1, transaction);
            transaction.commit();
        }
        test.assertTrue(card.matches(TabletItem.getContainingItem(player.getMainHandItem(), 0)), "Committed card was not saved");
        try (var transaction = Transaction.openRoot()) {
            handler.extract(0, card, 1, transaction);
        }
        test.assertTrue(card.matches(TabletItem.getContainingItem(player.getMainHandItem(), 0)), "Aborted extraction removed the saved card");
        test.assertTrue(handler.getResource(0).equals(card), "Aborted extraction did not restore handler contents");
        test.succeed();
    }

    public static void legacyFacade(GameTestHelper test) {
        var oldState = new net.minecraft.nbt.CompoundTag();
        oldState.putString("Name", "minecraft:oak_stairs");
        var properties = new net.minecraft.nbt.CompoundTag();
        properties.putString("facing", "east");
        properties.putString("half", "top");
        oldState.put("Properties", properties);
        var saved = new net.minecraft.nbt.CompoundTag();
        saved.put("mimic", oldState);
        var expected = Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(net.minecraft.world.level.block.StairBlock.FACING, Direction.EAST)
                .setValue(net.minecraft.world.level.block.StairBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP);
        var decoded = mcjty.xnet.modules.facade.MimicBlockSupport.readFromNBT(test.getLevel().registryAccess(), saved);
        test.assertTrue(decoded.equals(expected), "Legacy facade NBT lost its block or properties");
        var component = new net.minecraft.nbt.CompoundTag();
        component.put("state", oldState);
        var codec = mcjty.xnet.modules.facade.data.MimicData.CODEC;
        var data = codec.parse(net.minecraft.nbt.NbtOps.INSTANCE, component).getOrThrow();
        test.assertTrue(data.state().equals(expected), "Legacy facade component lost its block or properties");
        var encoded = (net.minecraft.nbt.CompoundTag) codec.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, data).getOrThrow();
        test.assertTrue(encoded.getCompoundOrEmpty("state").contains("id"), "Facade should write the 26.3 state format");
        test.assertTrue(codec.parse(net.minecraft.nbt.NbtOps.INSTANCE, encoded).getOrThrow().equals(data), "26.3 facade component did not round trip");
        test.succeed();
    }

    private static BarrelBlockEntity barrel(GameTestHelper test) {
        var pos = new BlockPos(1, 1, 1);
        test.setBlock(pos, Blocks.BARREL);
        return test.getBlockEntity(pos, BarrelBlockEntity.class);
    }

    private static FluidStacksResourceHandler tank(int capacity) {
        return new FluidStacksResourceHandler(1, capacity);
    }

    private static FluidConnectorSettings fluidSettings(Integer minmax) {
        return new FluidConnectorSettings(AbstractConnectorSettings.DEFAULT_SETTINGS, Direction.UP, InsExtMode.INS,
                Optional.empty(), Optional.of(100), Optional.ofNullable(minmax), 2, ItemStack.EMPTY);
    }

    private static <T> ConnectedEntity<T> consumer(BlockEntity destination, T settings) {
        return new ConnectedEntity<>(new SidedConsumer(new ConsumerId(1), Direction.UP), settings,
                destination.getBlockPos(), destination.getBlockPos(), destination, null);
    }

    private static IControllerContext context(GameTestHelper test) {
        return (IControllerContext) Proxy.newProxyInstance(IControllerContext.class.getClassLoader(),
                new Class<?>[]{IControllerContext.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getControllerWorld" -> test.getLevel();
                    case "checkAndConsumeRF", "matchColor" -> true;
                    default -> throw new AssertionError("Unexpected context call: " + method.getName());
                });
    }

    private static void set(Object target, String name, Object value) {
        try {
            var field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static Object invoke(Object target, String name, Class<?>[] parameters, Object... arguments) {
        try {
            var method = target.getClass().getDeclaredMethod(name, parameters);
            method.setAccessible(true);
            return method.invoke(target, arguments);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
}
