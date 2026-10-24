package mcjty.xnet.modules.wireless.client;

import mcjty.lib.container.GenericContainer;
import mcjty.lib.gui.GenericGuiContainer;
import mcjty.lib.gui.Window;
import mcjty.xnet.XNet;
import mcjty.xnet.modules.wireless.WirelessRouterModule;
import mcjty.xnet.modules.wireless.blocks.TileEntityWirelessRouter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import javax.annotation.Nonnull;

public class GuiWirelessRouter extends GenericGuiContainer<TileEntityWirelessRouter, GenericContainer> {

    public GuiWirelessRouter(GenericContainer container, Inventory inventory, Component title) {
        super(container, inventory, title, WirelessRouterModule.WIRELESS_ROUTER.block().get().getManualEntry());
    }

    public static void register(RegisterMenuScreensEvent event) {
        event.register(WirelessRouterModule.CONTAINER_WIRELESS_ROUTER.get(), GuiWirelessRouter::new);
    }

    @Override
    public void init() {
        window = new Window(this, getBE(), Identifier.fromNamespaceAndPath(XNet.MODID, "gui/wireless_router.gui"));
        super.init();
    }


    @Override
    protected void renderBg(@Nonnull GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {
        drawWindow(graphics, partialTicks, mouseX, mouseY);
    }
}
