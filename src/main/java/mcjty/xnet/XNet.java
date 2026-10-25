package mcjty.xnet;

import mcjty.lib.api.container.CapabilityContainerProvider;
import mcjty.lib.modules.Modules;
import mcjty.xnet.apiimpl.XNetApi;
import mcjty.xnet.modules.cables.CableModule;
import mcjty.xnet.modules.cables.blocks.ConnectorTileEntity;
import mcjty.xnet.modules.controller.ControllerModule;
import mcjty.xnet.modules.facade.FacadeModule;
import mcjty.xnet.modules.router.RouterModule;
import mcjty.xnet.modules.various.VariousModule;
import mcjty.xnet.modules.wireless.WirelessRouterModule;
import mcjty.xnet.setup.Config;
import mcjty.xnet.setup.ModSetup;
import mcjty.xnet.setup.Registration;
import mcjty.xnet.setup.XNetMessages;
import net.minecraft.world.item.Item;
import java.util.function.Supplier;

public class XNet implements net.fabricmc.api.ModInitializer {
    public static final String MODID = "xnet";
    public static final ModSetup setup = new ModSetup();
    public static XNet instance;
    public final Modules modules = new Modules();
    public static final XNetApi xNetApi = new XNetApi();
    @Override
    public void onInitialize() {
        instance = this;
        modules.register(new CableModule());
        modules.register(new ControllerModule());
        modules.register(new FacadeModule());
        modules.register(new RouterModule());
        modules.register(new WirelessRouterModule());
        modules.register(new VariousModule());
        Config.register(modules);
        Registration.register();
        setup.init();
        modules.init();
        setup.registerBlockCapabilities(Registration.RBLOCKS);
        XNetMessages.registerMessages();
        mcjty.xnet.setup.BlockBreakHandler.register();
        registerCapabilities();
    }
    public static <T extends Item> java.util.function.Function<Item.Properties, T> tab(java.util.function.Function<Item.Properties, T> factory) {
        return properties -> instance.setup.tab(() -> factory.apply(properties)).get();
    }

    public static <T extends Item> Supplier<T> tab(Supplier<T> supplier) {
        return instance.setup.tab(supplier);
    }

    private void registerCapabilities() {
        CapabilityContainerProvider.CONTAINER_PROVIDER_CAPABILITY.registerForBlocks((level, pos, state, be, side) ->
                be instanceof ConnectorTileEntity connector ? ConnectorTileEntity.SCREEN_CAP.apply(connector) : null,
                CableModule.CONNECTOR.get(), CableModule.ADVANCED_CONNECTOR.get());
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlocks((level, pos, state, be, side) ->
                be instanceof ConnectorTileEntity connector ? connector.getEnergyHandler(side) : null,
                CableModule.CONNECTOR.get(), CableModule.ADVANCED_CONNECTOR.get());
        net.fabricmc.loader.api.FabricLoader.getInstance().getEntrypoints("xnet", mcjty.rftoolsbase.api.xnet.XNetPlugin.class)
                .forEach(plugin -> plugin.register(xNetApi));
    }
}
