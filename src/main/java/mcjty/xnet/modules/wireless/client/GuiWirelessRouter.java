package mcjty.xnet.modules.wireless.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import mcjty.lib.container.GenericContainer;
import mcjty.lib.gui.GenericGuiContainer;
import mcjty.lib.gui.Window;
import mcjty.xnet.XNet;
import mcjty.xnet.modules.wireless.WirelessRouterModule;
import mcjty.xnet.modules.wireless.blocks.TileEntityWirelessRouter;
import mcjty.xnet.setup.XNetMessages;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

public class GuiWirelessRouter extends GenericGuiContainer<TileEntityWirelessRouter, GenericContainer> {

    public GuiWirelessRouter(TileEntityWirelessRouter router, GenericContainer container, PlayerInventory inventory) {
        super(router, container, inventory, WirelessRouterModule.WIRELESS_ROUTER.get().getManualEntry());
    }

    public static void register() {
        register(WirelessRouterModule.CONTAINER_WIRELESS_ROUTER.get(), GuiWirelessRouter::new);
    }

    @Override
    public void init() {
        window = new Window(this, tileEntity, XNetMessages.INSTANCE, new ResourceLocation(XNet.MODID, "gui/wireless_router.gui"));
        super.init();
    }


    @Override
    protected void renderBg(@Nonnull MatrixStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        drawWindow(matrixStack);
    }
}
