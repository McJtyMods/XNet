package mcjty.xnet.port;

import java.util.List;
import java.util.function.Consumer;
import mcjty.rftoolsbase.api.various.ITabletSupport;
import mcjty.rftoolsbase.modules.tablet.TabletModule;
import mcjty.xnet.XNet;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.minecraft.world.level.block.Blocks;

@EventBusSubscriber(modid = XNet.MODID)
public final class TestRegistration {
    private record Test(String name, Consumer<GameTestHelper> function) {}
    private static final List<Test> TESTS = List.of(
            new Test("partial_item_transfer", PortGameTests::partialItemTransfer),
            new Test("refused_item_extraction", PortGameTests::refusedItemExtraction),
            new Test("full_fluid_destination", PortGameTests::fullFluidDestination),
            new Test("partial_fluid_transfer", PortGameTests::partialFluidTransfer),
            new Test("fluid_limits", PortGameTests::fluidLimits),
            new Test("tablet_transactions", PortGameTests::tabletTransactions),
            new Test("legacy_facade", PortGameTests::legacyFacade));
    static Item tabletCard;

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.ITEM, registry -> {
            tabletCard = new TabletCard(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id("test_tablet_card"))));
            registry.register(id("test_tablet_card"), tabletCard);
        });
        event.register(Registries.TEST_FUNCTION, registry -> {
            for (var test : TESTS) registry.register(id(test.name), test.function);
        });
    }

    @SubscribeEvent
    public static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(Capabilities.Fluid.BLOCK,
                (level, pos, state, blockEntity, side) -> PortGameTests.tanks.get(blockEntity), Blocks.BARREL);
    }

    @SubscribeEvent
    public static void tests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(id("port"));
        for (var test : TESTS) event.registerTest(id(test.name), new FunctionGameTestInstance(
                ResourceKey.create(Registries.TEST_FUNCTION, id(test.name)),
                new TestData<>(environment, id("empty"), 100, 0, true)));
    }

    private static Identifier id(String name) { return Identifier.fromNamespaceAndPath(XNet.MODID, name); }

    private static final class TabletCard extends Item implements ITabletSupport {
        private TabletCard(Properties properties) { super(properties); }
        @Override public Item getInstalledTablet() { return TabletModule.TABLET_FILLED.get(); }
        @Override public void openGui(Player player, ItemStack tablet, ItemStack card) { }
    }
}
