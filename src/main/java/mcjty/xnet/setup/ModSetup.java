package mcjty.xnet.setup;

import mcjty.lib.compat.MainCompatHandler;
import mcjty.lib.setup.DefaultModSetup;
import mcjty.lib.varia.Logging;
import mcjty.xnet.XNet;
import mcjty.xnet.apiimpl.energy.EnergyChannelType;
import mcjty.xnet.apiimpl.fluids.FluidChannelType;
import mcjty.xnet.apiimpl.items.ItemChannelType;
import mcjty.xnet.apiimpl.logic.LogicChannelType;
import mcjty.xnet.apiimpl.none.NoneChannelType;

public class ModSetup extends DefaultModSetup {

    public static boolean rftoolsControl = false;

    public final ItemChannelType itemChannelType = new ItemChannelType();
    public final EnergyChannelType energyChannelType = new EnergyChannelType();
    public final FluidChannelType fluidChannelType = new FluidChannelType();
    public final LogicChannelType logicChannelType = new LogicChannelType();
    public final NoneChannelType noneChannelType = new NoneChannelType();

    @Override
    public void init() {
        super.init();

        new ServerEvents().register();
//        CommandHandler.registerCommands();

        XNet.xNetApi.registerConsumerProvider((world, blob, net) -> blob.getConsumers(net));
        XNet.xNetApi.registerChannelType(itemChannelType);
        XNet.xNetApi.registerChannelType(energyChannelType);
        XNet.xNetApi.registerChannelType(fluidChannelType);
        XNet.xNetApi.registerChannelType(logicChannelType);
        XNet.xNetApi.registerChannelType(noneChannelType);
    }

    @Override
    protected void setupModCompat() {
        rftoolsControl = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("rftoolscontrol");

        if (rftoolsControl) {
            Logging.log("XNet Detected RFTools Control: enabling support");
            // @todo 1.14
//            FMLInterModComms.sendFunctionMessage("rftoolscontrol", "getOpcodeRegistry", "mcjty.xnet.compat.rftoolscontrol.RFToolsControlSupport$GetOpcodeRegistry");
        }

        MainCompatHandler.registerWaila();
        MainCompatHandler.registerTOP();

        // TOP discovers TopExtras through the theoneprobe entrypoint.
    }
}
