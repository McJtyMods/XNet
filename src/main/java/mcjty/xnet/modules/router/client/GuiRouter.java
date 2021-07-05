package mcjty.xnet.modules.router.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import mcjty.lib.client.GuiTools;
import mcjty.lib.container.GenericContainer;
import mcjty.lib.gui.GenericGuiContainer;
import mcjty.lib.gui.Window;
import mcjty.lib.gui.widgets.BlockRender;
import mcjty.lib.gui.widgets.ImageLabel;
import mcjty.lib.gui.widgets.Label;
import mcjty.lib.gui.widgets.Panel;
import mcjty.lib.gui.widgets.TextField;
import mcjty.lib.gui.widgets.Widget;
import mcjty.lib.gui.widgets.WidgetList;
import mcjty.lib.typed.TypedMap;
import mcjty.lib.varia.BlockPosTools;
import mcjty.rftoolsbase.api.xnet.channels.IChannelSettings;
import mcjty.rftoolsbase.api.xnet.channels.IChannelType;
import mcjty.rftoolsbase.api.xnet.gui.IndicatorIcon;
import mcjty.xnet.XNet;
import mcjty.xnet.client.ControllerChannelClientInfo;
import mcjty.xnet.modules.controller.ControllerModule;
import mcjty.xnet.modules.router.RouterModule;
import mcjty.xnet.modules.router.blocks.TileEntityRouter;
import mcjty.xnet.modules.router.network.PacketGetLocalChannelsRouter;
import mcjty.xnet.modules.router.network.PacketGetRemoteChannelsRouter;
import mcjty.xnet.setup.XNetMessages;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static mcjty.lib.gui.widgets.Widgets.*;
import static mcjty.xnet.modules.router.blocks.TileEntityRouter.*;

public class GuiRouter extends GenericGuiContainer<TileEntityRouter, GenericContainer> {

    private WidgetList localChannelList;
    private WidgetList remoteChannelList;
    public static List<ControllerChannelClientInfo> fromServer_localChannels = null;
    public static List<ControllerChannelClientInfo> fromServer_remoteChannels = null;
    private boolean needsRefresh = true;
    private int listDirty;

    private static final ResourceLocation iconGuiElements = new ResourceLocation(XNet.MODID, "textures/gui/guielements.png");

    public GuiRouter(TileEntityRouter router, GenericContainer container, PlayerInventory inventory) {
        super(router, container, inventory, RouterModule.ROUTER.get().getManualEntry());
    }

    public static void register() {
        register(RouterModule.CONTAINER_ROUTER.get(), GuiRouter::new);
    }

    @Override
    public void init() {
        window = new Window(this, tileEntity, XNetMessages.INSTANCE, new ResourceLocation(XNet.MODID, "gui/router.gui"));
        super.init();

        localChannelList = window.findChild("localchannels");
        remoteChannelList = window.findChild("remotechannels");

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
        fromServer_localChannels = null;
        fromServer_remoteChannels = null;
        needsRefresh = true;
        listDirty = 3;
        requestListsIfNeeded();
    }


    private boolean listsReady() {
        return fromServer_localChannels != null && fromServer_remoteChannels != null;
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
        Map<BlockPos, Integer> ctrlNums = new HashMap<BlockPos, Integer>();

        for (ControllerChannelClientInfo channel : fromServer_localChannels) {
            localChannelList.children(makeChannelLine(channel, true, ctrlNums));
        }

        ctrlNums.clear();
        localChannelList.selected(sel);

        remoteChannelList.removeChildren();
        remoteChannelList.rowheight(40);
        sel = remoteChannelList.getSelected();

        for (ControllerChannelClientInfo channel : fromServer_remoteChannels) {
            remoteChannelList.children(makeChannelLine(channel, false, ctrlNums));
        }

        ctrlNums.clear();
        remoteChannelList.selected(sel);
    }

    private Panel makeChannelLine(ControllerChannelClientInfo channel, boolean local, Map<BlockPos, Integer> ctrlNums) {
        String name = channel.getChannelName();
        String publishedName = channel.getPublishedName();
        BlockPos controllerPos = channel.getPos();
        // @todo, this could be derived from the connector number + sidedness if this info was available;
        // the idea of counting the controllers is for the player to not need full block position
        // in the GUI anymore to easily be able to identify whether two controllers are the same.
        // (The info is still available via tooltips.)
        int ctrlNum = 0;
        if (ctrlNums.containsKey(controllerPos)) {
                ctrlNum = ctrlNums.get(controllerPos);
        }
        else {
                ctrlNum = ctrlNums.size() + 1;
                ctrlNums.put(controllerPos, ctrlNum);
        }
        String ctrlShortName = (local ? "LC" : "RC") + String.valueOf(ctrlNum);
        IChannelType type = channel.getChannelType();
        int index = channel.getIndex();
        String num = String.valueOf(index + 1);

        Panel panel = positional().desiredHeight(30);
        Panel panel1 = horizontal(0, 0).hint(0, 0, 160, 13);
        int labelColor = 0xff2244aa;
        // @todo, better way to show remote channels
        if (channel.isRemote()) {
            labelColor = 0xffaa1133;
        }

        // @todo, for non-local channels add something similar with router icon + number,
        // with router position (+ name?) as tooltip
        BlockRender ctrlBr = new BlockRender().renderItem(ControllerModule.CONTROLLER.get());
        ctrlBr.userObject("block");  // (See drawStackTooltips() below.)
        // @todo, have meta-data in even more tooltips
        // (e.g., controller cable color, numbers + name from the connectors,
        // and/or the same for the relevant router connector)
        ctrlBr.tooltips(TextFormatting.GREEN + "Controller position: " +
                TextFormatting.WHITE + BlockPosTools.toString(controllerPos));
        panel1.children(
                ctrlBr,
                label(ctrlShortName));

        // Represent channel number/type in the same way as
        // in GuiController.drawGuiContainerBackgroundLayer().
        Label numTypeLabel = label(num);
        numTypeLabel.tooltips(TextFormatting.GREEN + "Channel type: " + TextFormatting.WHITE + type.getName());
        IChannelSettings settings = type.createChannel();
        if (settings != null) {
                IndicatorIcon icon = settings.getIndicatorIcon();
                if (icon != null) {
                        numTypeLabel.image(icon.getImage(), icon.getU(), icon.getV(), icon.getIw(), icon.getIh());
                }
                String indicator = settings.getIndicator();
                if (indicator != null) {
                        numTypeLabel.text(indicator + num);
                }
        }

        panel1.children(
                label("Ch").color(labelColor),
                numTypeLabel,
                label(name));

        Panel panel2 = horizontal(0, 0).hint(0, 13, 160, 13)
                .children(label("Pub").color(labelColor));
        if (channel.isRemote()) {
            panel2.children(new ImageLabel().image(iconGuiElements, 48, 80).desiredWidth(16));
        }
        if (local) {
            TextField pubName = new TextField().text(publishedName).desiredWidth(50).desiredHeight(13)
                    .event((newText) -> updatePublish(controllerPos, index, newText));
            panel2.children(pubName);
        } else {
            panel2.children(label(publishedName).color(0xff33ff00));
        }

        panel.children(panel1, panel2);
        return panel;
    }

    private void requestListsIfNeeded() {
        if (fromServer_localChannels != null && fromServer_remoteChannels != null) {
            return;
        }
        listDirty--;
        if (listDirty <= 0) {
            XNetMessages.INSTANCE.sendToServer(new PacketGetLocalChannelsRouter(tileEntity.getBlockPos()));
            XNetMessages.INSTANCE.sendToServer(new PacketGetRemoteChannelsRouter(tileEntity.getBlockPos()));
            listDirty = 10;
        }
    }


    @Override
    protected void renderBg(MatrixStack matrixStack, float v, int x1, int x2) {
        requestListsIfNeeded();
        populateList();
        drawWindow(matrixStack);
    }

    // Much the same as in GuiController.
    @Override
    protected void drawStackTooltips(MatrixStack matrixStack, int mouseX, int mouseY) {
        int x = GuiTools.getRelativeX(this);
        int y = GuiTools.getRelativeY(this);
        Widget<?> widget = window.getToplevel().getWidgetAtPosition(x, y);
        if (widget instanceof BlockRender) {
            if ("block".equals(widget.getUserObject())) {
                return;     // Don't do the normal tooltip rendering
            }
        }
        super.drawStackTooltips(matrixStack, mouseX, mouseY);
    }
}
