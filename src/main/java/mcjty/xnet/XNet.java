package mcjty.xnet;


import java.util.function.Function;
import java.util.function.Supplier;
import mcjty.lib.api.container.CapabilityContainerProvider;
import mcjty.lib.datagen.DataGen;
import mcjty.lib.modules.Modules;
import mcjty.rftoolsbase.api.xnet.IXNet;
import mcjty.xnet.apiimpl.XNetApi;
import mcjty.xnet.modules.cables.CableModule;
import mcjty.xnet.modules.cables.blocks.ConnectorTileEntity;
import mcjty.xnet.modules.controller.ControllerModule;
import mcjty.xnet.modules.facade.FacadeModule;
import mcjty.xnet.modules.facade.client.ClientSetup;
import mcjty.xnet.modules.router.RouterModule;
import mcjty.xnet.modules.various.VariousModule;
import mcjty.xnet.modules.wireless.WirelessRouterModule;
import mcjty.xnet.setup.Config;
import mcjty.xnet.setup.ModSetup;
import mcjty.xnet.setup.Registration;
import mcjty.xnet.setup.XNetMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.InterModProcessEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.IBlockCapabilityProvider;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.Nullable;


@Mod(XNet.MODID)
public class XNet {

    public static final String MODID = "xnet";

    public static final ModSetup setup = new ModSetup();

    public static XNet instance;
    private final Modules modules = new Modules();

    public static final XNetApi xNetApi = new XNetApi();

    public XNet(ModContainer mod, IEventBus bus, Dist dist) {
        instance = this;
        setupModules(bus, dist);

        Config.register(mod, modules);

        Registration.register(bus);

        bus.addListener(setup::init);
        bus.addListener(modules::init);
        bus.addListener(this::processIMC);
        bus.addListener(this::onDataGen);
        bus.addListener(XNetMessages::registerMessages);
        bus.addListener(setup.getBlockCapabilityRegistrar(Registration.RBLOCKS));
        bus.addListener(this::onRegisterCapabilities);

        if (dist.isClient()) {
            bus.addListener(modules::initClient);
            bus.addListener(ClientSetup::registerBlockColor);
        }
    }

    public static <T extends Item> java.util.function.Function<Item.Properties, T> tab(java.util.function.Function<Item.Properties, T> factory) {
        return properties -> instance.setup.tab(() -> factory.apply(properties)).get();
    }

    public static <T extends Item> Supplier<T> tab(Supplier<T> supplier) {
        return instance.setup.tab(supplier);
    }

    private void onDataGen(GatherDataEvent.Client event) {
        DataGen datagen = new DataGen(MODID, event);
        modules.datagen(datagen, event.getWorldLookupProvider());
        datagen.generate();
        event.getGenerator().addProvider(true,
                new mcjty.rftoolsbase.tools.ClientItemDefinitions(event.getGenerator().getPackOutput(), MODID, java.util.Map.of()));
    }

    private void setupModules(IEventBus bus, Dist dist) {
        modules.register(new CableModule(bus, dist));
        modules.register(new ControllerModule(bus));
        modules.register(new FacadeModule());
        modules.register(new RouterModule(bus));
        modules.register(new WirelessRouterModule(bus));
        modules.register(new VariousModule());
    }

    private void processIMC(final InterModProcessEvent event) {
        event.getIMCStream().forEach(message -> {
            if ("getXNet".equalsIgnoreCase(message.method())) {
                Supplier<?> supplier = message.messageSupplier();
                if (supplier.get() instanceof Function<?, ?> function) {
                    ((Function<IXNet, ?>) function).apply(xNetApi);
                }
            }
        });
    }

    private void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(CapabilityContainerProvider.CONTAINER_PROVIDER_CAPABILITY, new IBlockCapabilityProvider<>() {
            @Override
            public @Nullable MenuProvider getCapability(Level level, BlockPos blockPos, BlockState blockState, @Nullable BlockEntity blockEntity, Direction direction) {
                if (blockEntity instanceof ConnectorTileEntity connector) {
                    return ConnectorTileEntity.SCREEN_CAP.apply(connector);
                }
                return null;
            }
        }, CableModule.CONNECTOR.get(), CableModule.ADVANCED_CONNECTOR.get());
        event.registerBlock(Capabilities.Energy.BLOCK, new IBlockCapabilityProvider<net.neoforged.neoforge.transfer.energy.EnergyHandler, Direction>() {
            @Override
            public @Nullable net.neoforged.neoforge.transfer.energy.EnergyHandler getCapability(Level level, BlockPos blockPos, BlockState blockState, @Nullable BlockEntity blockEntity, Direction direction) {
                if (blockEntity instanceof ConnectorTileEntity connector) {
                    return connector.getEnergyHandler(direction);
                }
                return null;
            }
        }, CableModule.CONNECTOR.get(), CableModule.ADVANCED_CONNECTOR.get());
    }
}
