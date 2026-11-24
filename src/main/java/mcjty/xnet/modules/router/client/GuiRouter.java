package mcjty.xnet.modules.router.client;

import mcjty.lib.container.GenericContainer;
import mcjty.lib.gui.GenericGuiContainer;
import mcjty.lib.gui.Window;
import mcjty.lib.gui.widgets.ImageLabel;
import mcjty.lib.gui.widgets.Panel;
import mcjty.lib.gui.widgets.TextField;
import mcjty.lib.gui.widgets.WidgetList;
import mcjty.lib.network.Networking;
import mcjty.lib.network.PacketGetListFromServer;
import mcjty.lib.typed.TypedMap;
import mcjty.lib.varia.BlockPosTools;
import mcjty.rftoolsbase.api.xnet.channels.IChannelType;
import mcjty.xnet.XNet;
import mcjty.xnet.client.ControllerChannelClientInfo;
import mcjty.xnet.modules.router.RouterModule;
import mcjty.xnet.modules.router.blocks.TileEntityRouter;
import net.minecraft.client.gui.GuiGraphics;
import mcjty.xnet.setup.XNetMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import javax.annotation.Nonnull;
import java.util.List;

import static mcjty.lib.gui.widgets.Widgets.horizontal;
import static mcjty.lib.gui.widgets.Widgets.label;
import static mcjty.lib.gui.widgets.Widgets.positional;
import static mcjty.xnet.apiimpl.Constants.WIDGET_LOCAL_CHANNELS;
import static mcjty.xnet.apiimpl.Constants.WIDGET_REMOTE_CHANNELS;
import static mcjty.xnet.modules.router.blocks.TileEntityRouter.CMD_GETCHANNELS;
import static mcjty.xnet.modules.router.blocks.TileEntityRouter.CMD_GETREMOTECHANNELS;
import static mcjty.xnet.modules.router.blocks.TileEntityRouter.PARAM_CHANNEL;
import static mcjty.xnet.modules.router.blocks.TileEntityRouter.PARAM_NAME;
import static mcjty.xnet.modules.router.blocks.TileEntityRouter.PARAM_POS;
import static mcjty.xnet.utils.I18nConstants.INDEX_LABEL;
import static mcjty.xnet.utils.I18nConstants.POS_LABEL;

public class GuiRouter extends GenericGuiContainer<TileEntityRouter, GenericContainer> {

    private static final int REMOTE_REFRESH_TICKS = 20;

    private WidgetList localChannelList;
    private WidgetList remoteChannelList;
    private boolean needsRefresh = true;
    private int listDirty;
    private int remoteRefreshTicks;
    private List<ControllerChannelClientInfo> displayedRemoteChannels;

    private static final ResourceLocation iconGuiElements = ResourceLocation.fromNamespaceAndPath(XNet.MODID, "textures/gui/guielements.png");

    public GuiRouter(GenericContainer container, Inventory inventory, Component title) {
        super(container, inventory, title, RouterModule.ROUTER.block().get().getManualEntry());
    }

    public static void register(RegisterMenuScreensEvent event) {
        event.register(RouterModule.CONTAINER_ROUTER.get(), GuiRouter::new);
    }

    @Override
    public void init() {
        window = new Window(this, getBE(), ResourceLocation.fromNamespaceAndPath(XNet.MODID, "gui/router.gui"));
        super.init();

        localChannelList = window.findChild(WIDGET_LOCAL_CHANNELS);
        remoteChannelList = window.findChild(WIDGET_REMOTE_CHANNELS);

        refresh();
        listDirty = 0;
    }

    private void updatePublish(BlockPos pos, int index, String name) {
        sendServerCommandTyped(TileEntityRouter.CMD_UPDATENAME,
                TypedMap.builder()
                        .put(PARAM_POS, pos)
                        .put(PARAM_CHANNEL, index)
                        .put(PARAM_NAME, name)
                        .build());
    }

    private void refresh() {
        TileEntityRouter tileEntity = getBE();
        tileEntity.clientLocalChannels = null;
        tileEntity.clientRemoteChannels = null;
        needsRefresh = true;
        displayedRemoteChannels = null;
        remoteRefreshTicks = REMOTE_REFRESH_TICKS;
        listDirty = 3;
        requestListsIfNeeded();
    }


    private boolean listsReady() {
        TileEntityRouter tileEntity = getBE();
        return tileEntity.clientLocalChannels != null && tileEntity.clientRemoteChannels != null;
    }

    private void populateList() {
        if (!listsReady()) {
            return;
        }
        TileEntityRouter tileEntity = getBE();
        if (needsRefresh) {
            needsRefresh = false;
            int selected = localChannelList.getSelected();
            localChannelList.removeChildren();
            localChannelList.rowheight(40);
            for (ControllerChannelClientInfo channel : tileEntity.clientLocalChannels) {
                localChannelList.children(makeChannelLine(channel, true));
            }
            localChannelList.selected(selected);
        }

        List<ControllerChannelClientInfo> remoteChannels = tileEntity.clientRemoteChannels;
        if (!remoteChannels.equals(displayedRemoteChannels)) {
            displayedRemoteChannels = remoteChannels;
            int selected = remoteChannelList.getSelected();
            remoteChannelList.removeChildren();
            remoteChannelList.rowheight(40);
            for (ControllerChannelClientInfo channel : remoteChannels) {
                remoteChannelList.children(makeChannelLine(channel, false));
            }
            remoteChannelList.selected(selected);
        }
    }

    private Panel makeChannelLine(ControllerChannelClientInfo channel, boolean local) {
        String name = channel.getChannelName();
        String publishedName = channel.getPublishedName();
        BlockPos controllerPos = channel.getPos();
        IChannelType type = channel.getChannelType();
        int index = channel.getIndex();

        Panel panel = positional().desiredHeight(30);
        Panel panel1 = horizontal(0, 0).hint(0, 0, 160, 13);
        int labelColor = 0xff2244aa;
        // @todo, better way to show remote channels
        if (channel.isRemote()) {
            labelColor = 0xffaa1133;
        }
        panel1.children(
                label("Ch").color(labelColor),
                label(name),
                label(">").color(labelColor));
        if (channel.isRemote()) {
            panel1.children(new ImageLabel().image(iconGuiElements, 48, 80).desiredWidth(16));
        }
        if (local) {
            TextField pubName = new TextField().text(publishedName).desiredWidth(50).desiredHeight(13)
                    .event((newText) -> updatePublish(controllerPos, index, newText));
            panel1.children(pubName);
        } else {
            panel1.children(label(publishedName).color(0xff33ff00));
        }

        Panel panel2 = horizontal(0, 0).hint(0, 13, 160, 13)
                .children(
                        label(POS_LABEL.i18n()).color(labelColor),
                        label(BlockPosTools.toString(controllerPos)));

        Panel panel3 = horizontal(0, 0).hint(0, 26, 160, 13)
                .children(
                        label(INDEX_LABEL.i18n()).color(labelColor),
                        label(index + " (" + type.getName() + ")"));

        panel.children(panel1, panel2, panel3);
        return panel;
    }

    private void requestListsIfNeeded() {
        TileEntityRouter tileEntity = getBE();
        if (tileEntity.clientLocalChannels != null && tileEntity.clientRemoteChannels != null) {
            return;
        }
        listDirty--;
        if (listDirty <= 0) {
            Networking.sendToServer(PacketGetListFromServer.create(tileEntity.getBlockPos(), CMD_GETCHANNELS.name()));
            Networking.sendToServer(PacketGetListFromServer.create(tileEntity.getBlockPos(), CMD_GETREMOTECHANNELS.name()));
            listDirty = 10;
        }
    }


    @Override
    protected void containerTick() {
        super.containerTick();
        if (listsReady() && --remoteRefreshTicks <= 0) {
            remoteRefreshTicks = REMOTE_REFRESH_TICKS;
            Networking.sendToServer(PacketGetListFromServer.create(getBE().getBlockPos(), CMD_GETREMOTECHANNELS.name()));
        }
    }

    @Override
    protected void renderBg(@Nonnull GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        requestListsIfNeeded();
        populateList();
        drawWindow(graphics, partialTicks, mouseX, mouseY);
    }
}
