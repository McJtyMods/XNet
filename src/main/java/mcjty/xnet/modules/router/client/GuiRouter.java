package mcjty.xnet.modules.router.client;

import com.mojang.blaze3d.vertex.PoseStack;
import mcjty.lib.container.GenericContainer;
import mcjty.lib.gui.GenericGuiContainer;
import mcjty.lib.gui.Window;
import mcjty.lib.gui.widgets.ImageLabel;
import mcjty.lib.gui.widgets.Panel;
import mcjty.lib.gui.widgets.TextField;
import mcjty.lib.gui.widgets.WidgetList;
import mcjty.lib.network.PacketGetListFromServer;
import mcjty.lib.typed.TypedMap;
import mcjty.lib.varia.BlockPosTools;
import mcjty.rftoolsbase.api.xnet.channels.IChannelType;
import mcjty.xnet.XNet;
import mcjty.xnet.client.ControllerChannelClientInfo;
import mcjty.xnet.modules.router.RouterModule;
import mcjty.xnet.modules.router.blocks.TileEntityRouter;
import mcjty.xnet.setup.XNetMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.Nonnull;

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

    private WidgetList localChannelList;
    private WidgetList remoteChannelList;
    private boolean needsRefresh = true;
    private int listDirty;

    private static final ResourceLocation iconGuiElements = new ResourceLocation(XNet.MODID, "textures/gui/guielements.png");

    public GuiRouter(TileEntityRouter router, GenericContainer container, Inventory inventory) {
        super(router, container, inventory, RouterModule.ROUTER.get().getManualEntry());
    }

    public static void register() {
        register(RouterModule.CONTAINER_ROUTER.get(), GuiRouter::new);
    }

    @Override
    public void init() {
        window = new Window(this, tileEntity, XNetMessages.INSTANCE, new ResourceLocation(XNet.MODID, "gui/router.gui"));
        super.init();

        localChannelList = window.findChild(WIDGET_LOCAL_CHANNELS);
        remoteChannelList = window.findChild(WIDGET_REMOTE_CHANNELS);

        refresh();
        listDirty = 0;
    }

    private void updatePublish(BlockPos pos, int index, String name) {
        sendServerCommandTyped(XNetMessages.INSTANCE, TileEntityRouter.CMD_UPDATENAME,
                TypedMap.builder()
                        .put(PARAM_POS, pos)
                        .put(PARAM_CHANNEL, index)
                        .put(PARAM_NAME, name)
                        .build());
    }

    private void refresh() {
        tileEntity.clientLocalChannels = null;
        tileEntity.clientRemoteChannels = null;
        needsRefresh = true;
        listDirty = 3;
        requestListsIfNeeded();
    }


    private boolean listsReady() {
        return tileEntity.clientLocalChannels != null && tileEntity.clientRemoteChannels != null;
    }

    private void populateList() {
        if (!listsReady()) {
            return;
        }
        if (!needsRefresh) {
            return;
        }
        needsRefresh = false;

        localChannelList.removeChildren();
        localChannelList.rowheight(40);
        int sel = localChannelList.getSelected();

        for (ControllerChannelClientInfo channel : tileEntity.clientLocalChannels) {
            localChannelList.children(makeChannelLine(channel, true));
        }

        localChannelList.selected(sel);

        remoteChannelList.removeChildren();
        remoteChannelList.rowheight(40);
        sel = remoteChannelList.getSelected();

        for (ControllerChannelClientInfo channel : tileEntity.clientRemoteChannels) {
            remoteChannelList.children(makeChannelLine(channel, false));
        }

        remoteChannelList.selected(sel);
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
        if (tileEntity.clientLocalChannels != null && tileEntity.clientRemoteChannels != null) {
            return;
        }
        listDirty--;
        if (listDirty <= 0) {
            XNetMessages.INSTANCE.sendToServer(new PacketGetListFromServer(tileEntity.getBlockPos(), CMD_GETCHANNELS.name()));
            XNetMessages.INSTANCE.sendToServer(new PacketGetListFromServer(tileEntity.getBlockPos(), CMD_GETREMOTECHANNELS.name()));
            listDirty = 10;
        }
    }


    @Override
    protected void renderBg(@Nonnull PoseStack matrixStack, float v, int x1, int x2) {
        requestListsIfNeeded();
        populateList();
        drawWindow(matrixStack);
    }
}
