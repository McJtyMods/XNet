package mcjty.xnet.modules.controller.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mcjty.lib.base.StyleConfig;
import mcjty.lib.client.GuiTools;
import mcjty.lib.client.RenderHelper;
import mcjty.lib.container.GenericContainer;
import mcjty.lib.gui.GenericGuiContainer;
import mcjty.lib.gui.Window;
import mcjty.lib.gui.WindowManager;
import mcjty.lib.gui.events.ButtonEvent;
import mcjty.lib.gui.events.DefaultSelectionEvent;
import mcjty.lib.gui.widgets.*;
import mcjty.lib.network.Networking;
import mcjty.lib.network.PacketGetListFromServer;
import mcjty.lib.network.PacketServerCommandTyped;
import mcjty.lib.typed.TypedMap;
import mcjty.lib.varia.BlockPosTools;
import mcjty.lib.varia.Logging;
import mcjty.lib.varia.SafeClientTools;
import mcjty.rftoolsbase.RFToolsBase;
import mcjty.rftoolsbase.api.xnet.channels.IChannelType;
import mcjty.rftoolsbase.api.xnet.gui.IndicatorIcon;
import mcjty.rftoolsbase.api.xnet.keys.SidedConsumer;
import mcjty.rftoolsbase.api.xnet.keys.SidedPos;
import mcjty.xnet.XNet;
import mcjty.xnet.client.ChannelClientInfo;
import mcjty.xnet.client.ConnectedBlockClientInfo;
import mcjty.xnet.client.ConnectorClientInfo;
import mcjty.xnet.modules.controller.ChannelInfo;
import mcjty.xnet.modules.controller.ControllerModule;
import mcjty.xnet.modules.controller.blocks.TileEntityController;
import mcjty.xnet.setup.Config;
import mcjty.xnet.utils.ChannelChoiceLabel;
import mcjty.xnet.utils.I18nUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static mcjty.lib.gui.widgets.Widgets.*;
import static mcjty.xnet.apiimpl.Constants.*;
import static mcjty.xnet.modules.controller.ChannelInfo.MAX_CHANNELS;
import static mcjty.xnet.modules.controller.blocks.TileEntityController.*;
import static mcjty.xnet.utils.I18nConstants.*;


public class GuiController extends GenericGuiContainer<TileEntityController, GenericContainer> {

    private WidgetList connectorList;
    private final List<SidedPos> connectorPositions = new ArrayList<>();
    private int listDirty;
    private TextField searchBar;

    private int delayedSelectedChannel = -1;
    private int delayedSelectedLine = -1;
    private SidedPos delayedSelectedConnector = null;

    private Panel channelEditPanel;
    private Panel connectorEditPanel;

    private final ToggleButton[] channelButtons = new ToggleButton[MAX_CHANNELS];

    private SidedPos editingConnector = null;
    private int editingChannel = -1;

    private int showingChannel = -1;
    private SidedPos showingConnector = null;

    private static GuiController openController = null;

    private EnergyBar energyBar;
    private Button copyConnector = null;

    private boolean needsRefresh = true;

    public GuiController(GenericContainer container, Inventory inventory, Component title) {
        super(container, inventory, title, ControllerModule.CONTROLLER.block().get().getManualEntry());
        openController = this;
    }

    public static void register(RegisterMenuScreensEvent event) {
        event.register(ControllerModule.CONTAINER_CONTROLLER.get(), GuiController::new);
    }

    @Override
    public void removed() {
        super.removed();
        openController = null;
    }

    @Override
    public void init() {
        window = new Window(this, getBE(), ResourceLocation.fromNamespaceAndPath(XNet.MODID, "gui/controller.gui"));
        super.init();

        initializeFields();
        setupEvents();

        editingConnector = null;
        editingChannel = -1;

        refresh();
        listDirty = 0;
    }

    private void setupEvents() {
        window.event(WIDGET_SEARCH_BAR, (source, params) -> needsRefresh = true);
        for (int i = 0 ; i < MAX_CHANNELS ; i++) {
            String channel = TAG_CHANNEL + (i+1);
            int finalI = i;
            window.event(channel, (source, params) -> selectChannelEditor(finalI));
        }
    }

    public TileEntityController getController() {
        return getBE();
    }

    public Panel getConnectorEditPanel() {
        return connectorEditPanel;
    }

    private void initializeFields() {
        channelEditPanel = window.findChild(WIDGET_CHANNEL_EDIT_PANEL);
        connectorEditPanel = window.findChild(WIDGET_CONNECTOR_EDIT_PANEL);

        searchBar = window.findChild(WIDGET_SEARCH_BAR);
        connectorList = window.findChild(TAG_CONNECTORS);

        connectorList.event(new DefaultSelectionEvent() {
            @Override
            public void doubleClick(int index) {
                hilightSelectedContainer(index);
            }
        });

        List<ChannelInfo> channels =  getController().getChannels();
        for (int i = 0 ; i < MAX_CHANNELS ; i++) {
            String name = TAG_CHANNEL + (i+1);
            channelButtons[i] = window.findChild(name);
            if (i >= channels.size() || channels.get(i) == null) {
                channelButtons[i].tooltips(EDIT_CHANNEL_TOOLTIP_NOT_EXIST_FORMATTED.i18n(i + 1));
            } else {
                channelButtons[i]
                        .tooltips(I18nUtils.getSplitedTooltip(
                                EDIT_CHANNEL_TOOLTIP_EXIST_FORMATTED.i18n(i + 1, channels.get(i).getType().getName()))
                        );
            }
        }

        energyBar = window.findChild(WIDGET_ENERGY_BAR);
    }

    private void updateFields() {

    }

    private void hilightSelectedContainer(int index) {
        if (index < 0) {
            return;
        }
        TileEntityController tileEntity = getBE();
        ConnectedBlockClientInfo c = tileEntity.clientConnectedBlocks.get(index);
        if (c != null) {
            RFToolsBase.instance.clientInfo.hilightBlock(c.getPos().pos(), System.currentTimeMillis() + 1000 * 5);
            Logging.message(minecraft.player, MESSAGE_BLOCK_HIGHLIGHTED.i18n());
            minecraft.player.closeContainer();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (handleClipboard(keyCode)) {
            return true;
        }
        if (handleKeyUpDown(keyCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void keyTypedFromEvent(int keyCode, int scanCode) {
        if (handleClipboard(keyCode)) {
            return;
        }
        if (handleKeyUpDown(keyCode)) {
            return;
        }
        super.keyTypedFromEvent(keyCode, scanCode);
    }

    private boolean handleKeyUpDown(int keyCode) {
        if (getSelectedChannel() == -1) {
            return false;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            int sel = connectorList.getSelected();
            if (sel > 0) {
                sel--;
                connectorList.selected(sel);
                selectConnectorEditor(connectorPositions.get(sel), getSelectedChannel());
            }
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_DOWN) {
            int sel = connectorList.getSelected();
            if (sel != -1) {
                if (sel < connectorList.getChildCount() - 1) {
                    sel++;
                    connectorList.selected(sel);
                    selectConnectorEditor(connectorPositions.get(sel), getSelectedChannel());
                }
            }
            return true;
        }
        return false;
    }


    private boolean handleClipboard(int keyCode) {
        if (SafeClientTools.isCtrlKeyDown()) {
            if (keyCode == GLFW.GLFW_KEY_C) {
                if (getSelectedChannel() != -1) {
                    copyConnector();
                } else {
                    showMessage(minecraft, this, getWindowManager(), 50, 50, ChatFormatting.RED + ERROR_NOTHING_SELECTED.i18n());
                }
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_V) {
                if (getSelectedChannel() != -1) {
                    pasteConnector();
                } else {
                    showMessage(minecraft, this, getWindowManager(), 50, 50, ChatFormatting.RED + ERROR_NOTHING_SELECTED.i18n());
                }
                return true;
            }
        }
        return false;
    }

    private void selectChannelEditor(int finalI) {
        editingChannel = -1;
        showingConnector = null;
        for (int j = 0 ; j < MAX_CHANNELS ; j++) {
            if (j != finalI) {
                channelButtons[j].pressed(false);
                editingChannel = finalI;
            }
        }
    }

    private void removeConnector(SidedPos sidedPos) {
        sendServerCommandTyped(TileEntityController.CMD_REMOVECONNECTOR,
                TypedMap.builder()
                        .put(PARAM_CHANNEL, getSelectedChannel())
                        .put(PARAM_POS, sidedPos.pos())
                        .put(PARAM_SIDE, sidedPos.side().ordinal())
                        .build());
        refresh();
    }

    private void createConnector(SidedPos sidedPos) {
        sendServerCommandTyped(TileEntityController.CMD_CREATECONNECTOR,
                TypedMap.builder()
                        .put(PARAM_CHANNEL, getSelectedChannel())
                        .put(PARAM_POS, sidedPos.pos())
                        .put(PARAM_SIDE, sidedPos.side().ordinal())
                        .build());
        refresh();
    }

    private void removeChannel() {
        int selectedChannel = getSelectedChannel();
        if (selectedChannel == -1) {
            return;
        }
        showMessage(minecraft, this, getWindowManager(), 50, 50,
                ChatFormatting.RED + MESSAGE_CONFIRM_REMOVE_CHANNEL_FORMATTED.i18n((selectedChannel + 1)),
                () -> {
            sendServerCommandTyped(TileEntityController.CMD_REMOVECHANNEL,
                    TypedMap.builder()
                            .put(PARAM_INDEX, selectedChannel)
                            .build());

            channelButtons[selectedChannel].tooltips(EDIT_CHANNEL_TOOLTIP_NOT_EXIST_FORMATTED.i18n(selectedChannel + 1));
            refresh();
        });
    }

    private void createChannel(String typeId) {
        int selectedChannel = getSelectedChannel();
        if (selectedChannel == -1) {
            return;
        }
        sendServerCommandTyped(TileEntityController.CMD_CREATECHANNEL,
                TypedMap.builder()
                        .put(PARAM_INDEX, selectedChannel)
                        .put(PARAM_TYPE, typeId)
                        .build());
        IChannelType type = XNet.xNetApi.findType(typeId);
        if (type != null) {
            channelButtons[selectedChannel].tooltips(I18nUtils.getSplitedTooltip(
                            EDIT_CHANNEL_TOOLTIP_EXIST_FORMATTED.i18n((selectedChannel + 1), type.getName()))
                    );
        }

        refresh();
    }

    public void refresh() {
        TileEntityController tileEntity = getBE();
        tileEntity.clientChannels = null;
        tileEntity.clientConnectedBlocks = null;
        showingChannel = -1;
        showingConnector = null;
        needsRefresh = true;
        listDirty = 3;
        requestListsIfNeeded();
    }

    private void selectConnectorEditor(SidedPos sidedPos, int finalI) {
        editingConnector = sidedPos;
        selectChannelEditor(finalI);
    }

    private void refreshChannelEditor() {
        if (!listsReady()) {
            return;
        }
        TileEntityController tileEntity = getBE();
        if (editingChannel != -1 && showingChannel != editingChannel) {
            showingChannel = editingChannel;
            channelButtons[editingChannel].pressed(true);

            copyConnector = null;
            channelEditPanel.removeChildren();
            if (channelButtons[editingChannel].isPressed()) {
                ChannelClientInfo info = tileEntity.clientChannels.get(editingChannel);
                if (info != null) {
                    ChannelEditorPanel editor = new ChannelEditorPanel(channelEditPanel, minecraft, this, editingChannel);
                    editor.label(CHANNEL_LABEL_FORMATTED.i18n(editingChannel + 1))
                            .shift(5)
                            .toggle(TAG_ENABLED, CHANNEL_ENABLE_TOOLTIP.i18n(), info.isEnabled())
                            .shift(5)
                            .text(TAG_NAME, CHANNEL_NAME_LABEL.i18n(), info.getChannelName(), 65);
                    info.getChannelSettings().createGui(editor);

                    Button remove = button(151, 1, 9, 10, "x")
                            .textOffset(0, -1)
                            .tooltips(CHANNEL_REMOVE_TOOLTIP.i18n())
                            .event(this::removeChannel);
                    channelEditPanel.children(remove);
                    editor.setState(info.getChannelSettings());

                    Button copyChannel = button(134, 19, 25, 14, "C")
                            .tooltips(I18nUtils.getSplitedTooltip(CHANNEL_COPY_TOOLTIP.i18n()))
                            .event(this::copyChannel);
                    channelEditPanel.children(copyChannel);

                    copyConnector = button(114, 19, 25, 14, "C")
                            .tooltips(I18nUtils.getSplitedTooltip(CONNECTOR_COPY_TOOLTIP.i18n()))
                            .event(this::copyConnector);
                    channelEditPanel.children(copyConnector);

                } else {
                    ChannelChoiceLabel type = new ChannelChoiceLabel()
                            .hint(5, 3, 95, 14);
                    List<IChannelType> filtered = XNet.xNetApi.getChannels().values().stream().filter(channelType -> channelType != XNet.setup.noneChannelType).toList();
                    type.choices(filtered.toArray(new IChannelType[0]));
                    Button create = button(100, 3, 53, 14, CREATE_LABEL.i18n())
                            .event(() -> createChannel(type.getCurrentChoice()));

                    Button paste = button(100, 17, 53, 14, PASTE_LABEL.i18n())
                            .tooltips(I18nUtils.getSplitedTooltip(CHANNEL_PASTE_TOOLTIP.i18n()))
                            .event(this::pasteChannel);

                    channelEditPanel.children(type, create, paste);
                }
            }
        } else if (showingChannel != -1 && editingChannel == -1) {
            showingChannel = -1;
            channelEditPanel.removeChildren();
        }
    }

    public static void showMessage(Minecraft mc, Screen gui, WindowManager windowManager, int x, int y, String title) {
        showMessage(mc, gui, windowManager, x, y, title, null);
    }

    public static void showMessage(Minecraft mc, Screen gui, WindowManager windowManager, int x, int y, String title, ButtonEvent okEvent) {
        Panel ask = vertical()
                .filledBackground(0xff666666, 0xffaaaaaa)
                .filledRectThickness(1);
        ask.bounds(x, y, 200, 40);
        Window askWindow = windowManager.createModalWindow(ask);
        ask.children(label(title));
        Panel buttons = horizontal().desiredWidth(100).desiredHeight(18);
        if (okEvent != null) {
            buttons.children(button(CANCEL_LABEL.i18n()).event((() -> {
                windowManager.closeWindow(askWindow);
            })));
            buttons.children(button("OK").event(() -> {
                windowManager.closeWindow(askWindow);
                okEvent.buttonClicked();
            }));
        } else {
            buttons.children(button("OK").event((() -> {
                windowManager.closeWindow(askWindow);
            })));
        }
        ask.children(buttons);
    }

    private void copyConnector() {
        if (editingConnector != null) {
            sendServerCommandTyped(TileEntityController.CMD_COPYCONNECTOR,
                    TypedMap.builder()
                            .put(PARAM_INDEX, getSelectedChannel())
                            .put(PARAM_POS, editingConnector.pos())
                            .put(PARAM_SIDE, editingConnector.side().ordinal())
                            .build());
        }
    }


    private void copyChannel() {
        showMessage(minecraft, this, getWindowManager(), 50, 50, ChatFormatting.GREEN + MESSAGE_CHANNEL_COPIED.i18n());
        sendServerCommandTyped(TileEntityController.CMD_COPYCHANNEL,
                TypedMap.builder()
                        .put(PARAM_INDEX, getSelectedChannel())
                        .build());
    }

    public static void showError(String error) {
        if (openController != null) {
            Minecraft mc = Minecraft.getInstance();
            showMessage(mc, openController, openController.getWindowManager(), 50, 50, ChatFormatting.RED + error);
        }
    }

    public static void toClipboard(String json) {
        try {
            Minecraft.getInstance().keyboardHandler.setClipboard(json);
        } catch (Exception e) {
            showError(ERROR_COPY.i18n());
        }
    }

    private void pasteConnector() {
        try {
            String json = Minecraft.getInstance().keyboardHandler.getClipboard();
            int max = Config.controllerMaxPaste.get();
            if (max >= 0 && json.length() > max) {
                showMessage(minecraft, this, getWindowManager(), 50, 50, ChatFormatting.RED + ERROR_LARGE_COPY.i18n());
                return;
            }
            JsonParser parser = new JsonParser();
            JsonObject root = parser.parse(json).getAsJsonObject();
            String type = root.get(TAG_TYPE).getAsString();
            IChannelType channelType = XNet.xNetApi.findType(type);
            if (channelType == null) {
                showMessage(minecraft, this, getWindowManager(), 50, 50, ChatFormatting.RED + ERROR_UNS_CH_FORMATTED.i18n(type));
                return;
            }

            TileEntityController tileEntity = getBE();
            PacketServerCommandTyped packet = PacketServerCommandTyped.create(tileEntity.getBlockPos(), tileEntity.getDimension(), CMD_PASTECONNECTOR.name(), TypedMap.builder()
                    .put(PARAM_INDEX, getSelectedChannel())
                    .put(PARAM_POS, editingConnector.pos())
                    .put(PARAM_SIDE, editingConnector.side().ordinal())
                    .put(PARAM_JSON, json)
                    .build());
            sendSplit(packet, max < 0);

            if (connectorList.getSelected() != -1) {
                delayedSelectedChannel = getSelectedChannel();
                delayedSelectedLine = connectorList.getSelected();
                delayedSelectedConnector = connectorPositions.get(connectorList.getSelected());
            }
            refresh();
        } catch (Exception e) {
            showMessage(minecraft, this, getWindowManager(), 50, 50, ChatFormatting.RED + ERROR_READ_COPY.i18n());
        }
    }

    private void pasteChannel() {
        try {
            TileEntityController tileEntity = getBE();
            String json = Minecraft.getInstance().keyboardHandler.getClipboard();
            int max = Config.controllerMaxPaste.get();
            if (max >= 0 && json.length() > max) {
                showMessage(minecraft, this, getWindowManager(), 50, 50, ChatFormatting.RED + ERROR_LARGE_COPY.i18n());
                return;
            }
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            String type = root.get(TAG_TYPE).getAsString();
            IChannelType channelType = XNet.xNetApi.findType(type);
            if (channelType == null) {
                showMessage(minecraft, this, getWindowManager(), 50, 50, ChatFormatting.RED + ERROR_UNS_CH_FORMATTED.i18n(type));
                return;
            }
            PacketServerCommandTyped packet = PacketServerCommandTyped.create(tileEntity.getBlockPos(), tileEntity.getDimension(), CMD_PASTECHANNEL.name(), TypedMap.builder()
                    .put(PARAM_INDEX, getSelectedChannel())
                    .put(PARAM_JSON, json)
                    .build());
            sendSplit(packet, max < 0);

            refresh();
        } catch (Exception e) {
            showMessage(minecraft, this, getWindowManager(), 50, 50, ChatFormatting.RED + ERROR_READ_COPY.i18n());
        }
    }

    private void sendSplit(PacketServerCommandTyped packet, boolean doSplit) {
        Networking.sendToServer(packet);
    }

    private ConnectorClientInfo findClientInfo(ChannelClientInfo info, SidedPos p) {
        for (ConnectorClientInfo connector : info.getConnectors().values()) {
            if (connector.getPos().equals(p)) {
                return connector;
            }
        }
        return null;
    }

    private void refreshConnectorEditor() {
        if (!listsReady()) {
            return;
        }
        if (editingConnector != null && !editingConnector.equals(showingConnector)) {
            TileEntityController tileEntity = getBE();
            showingConnector = editingConnector;
            connectorEditPanel.removeChildren();
            ChannelClientInfo info = tileEntity.clientChannels.get(editingChannel);
            if (info != null) {
                ConnectorClientInfo clientInfo = findClientInfo(info, editingConnector);
                if (clientInfo != null) {
                    Direction side = clientInfo.getPos().side();
                    SidedConsumer sidedConsumer = new SidedConsumer(clientInfo.getConsumerId(), side.getOpposite());
                    ConnectorClientInfo connectorInfo = info.getConnectors().get(sidedConsumer);

                    Button remove = button(151, 1, 9, 10, "x")
                            .textOffset(0, -1)
                            .tooltips(CONNECTOR_REMOVE_TOOLTIP.i18n())
                            .event(() -> removeConnector(editingConnector));

                    ConnectorEditorPanel editor = new ConnectorEditorPanel(connectorEditPanel, minecraft, this, editingChannel, editingConnector);

                    connectorInfo.getConnectorSettings().createGui(editor);
                    connectorEditPanel.children(remove);
                    editor.setState(connectorInfo.getConnectorSettings());
                } else {
                    Button create = button(85, 20, 60, 14, CREATE_LABEL.i18n())
                            .event(() -> createConnector(editingConnector));
                    connectorEditPanel.children(create);

                    Button paste = button(85, 40, 60, 14, PASTE_LABEL.i18n())
                            .tooltips(I18nUtils.getSplitedTooltip(CONNECTOR_PASTE_TOOLTIP.i18n()))
                            .event(this::pasteConnector);
                    connectorEditPanel.children(paste);
                }
            }
        } else if (showingConnector != null && editingConnector == null) {
            showingConnector = null;
            connectorEditPanel.removeChildren();
        }
    }



    private void requestListsIfNeeded() {
        TileEntityController tileEntity = getBE();
        if (tileEntity.clientChannels != null && tileEntity.clientConnectedBlocks != null) {
            return;
        }
        listDirty--;
        if (listDirty <= 0) {
            Networking.sendToServer(PacketGetListFromServer.create(tileEntity.getBlockPos(), CMD_GETCHANNELS.name()));
            Networking.sendToServer(PacketGetListFromServer.create(tileEntity.getBlockPos(), CMD_GETCONNECTEDBLOCKS.name()));
            listDirty = 10;
            showingChannel = -1;
            showingConnector = null;
        }
    }

    private int getSelectedChannel() {
        for (int i = 0 ; i < MAX_CHANNELS ; i++) {
            if (channelButtons[i].isPressed()) {
                return i;
            }
        }
        return -1;
    }

    private void populateList() {
        if (!listsReady()) {
            return;
        }
        if (!needsRefresh) {
            return;
        }
        needsRefresh = false;

        connectorList.removeChildren();
        connectorPositions.clear();

        int sel = connectorList.getSelected();
        BlockPos prevPos = null;

        String selectedText = searchBar.getText().trim().toLowerCase();

        TileEntityController tileEntity = getBE();
        for (ConnectedBlockClientInfo connectedBlock : tileEntity.clientConnectedBlocks) {
            SidedPos sidedPos = connectedBlock.getPos();
            BlockPos coordinate = sidedPos.pos();
            String name = connectedBlock.getName();
            String blockUnlocName = connectedBlock.getBlockUnlocName();
            String blockName = I18n.get(blockUnlocName).trim();

            int color = StyleConfig.colorTextInListNormal;

            Panel panel = horizontal(0, 0);
            if (!selectedText.isEmpty()) {
                if (blockName.toLowerCase().contains(selectedText)) {
                    panel.filledBackground(0xffddeeaa);
                }
            }
            BlockRender br;
            if (coordinate.equals(prevPos)) {
                br = new BlockRender();
            } else {
                br = new BlockRender().renderItem(connectedBlock.getConnectedBlock());
                prevPos = coordinate;
            }
            br.userObject("block");
            panel.children(br);
            if (!name.isEmpty()) {
                br.tooltips(ChatFormatting.GREEN + CONNECTOR_LABEL.i18n() + ChatFormatting.WHITE + name,
                        ChatFormatting.GREEN + BLOCK_LABEL.i18n() + ChatFormatting.WHITE + blockName,
                        ChatFormatting.GREEN + POSITON_LABEL.i18n() + ChatFormatting.WHITE + BlockPosTools.toString(coordinate),
                        ChatFormatting.WHITE + DOUBLE_CLICK_HIGHLIGHT.i18n());
            } else {
                br.tooltips(ChatFormatting.GREEN + BLOCK_LABEL.i18n() + ChatFormatting.WHITE + blockName,
                        ChatFormatting.GREEN + POSITON_LABEL.i18n() + ChatFormatting.WHITE + BlockPosTools.toString(coordinate),
                        ChatFormatting.WHITE + DOUBLE_CLICK_HIGHLIGHT.i18n());
            }

            panel.children(label(sidedPos.side().getSerializedName().substring(0, 1).toUpperCase()).color(color).desiredWidth(18));
            for (int i = 0 ; i < MAX_CHANNELS ; i++) {
                Button but = new Button().desiredWidth(14);
                ChannelClientInfo info = tileEntity.clientChannels.get(i);
                if (info != null) {
                    ConnectorClientInfo clientInfo = findClientInfo(info, sidedPos);
                    if (clientInfo != null) {
                        IndicatorIcon icon = clientInfo.getConnectorSettings().getIndicatorIcon();
                        if (icon != null) {
                            but.image(icon.getImage(), icon.getU(), icon.getV(), icon.getIw(), icon.getIh());
                        }
                        String indicator = clientInfo.getConnectorSettings().getIndicator();
                        but.text(indicator != null ? indicator : "");
                    }
                }
                int finalI = i;
                but.event(() -> selectConnectorEditor(sidedPos, finalI));
                panel.children(but);
            }
            connectorList.children(panel);
            connectorPositions.add(sidedPos);
        }

        connectorList.selected(sel);
        if (delayedSelectedChannel != -1) {
            connectorList.selected(delayedSelectedLine);
            selectConnectorEditor(delayedSelectedConnector, delayedSelectedChannel);
        }
        delayedSelectedChannel = -1;
        delayedSelectedLine = -1;
        delayedSelectedConnector = null;
    }

    private boolean listsReady() {
        TileEntityController tileEntity = getBE();
        return tileEntity.clientChannels != null && tileEntity.clientConnectedBlocks != null;
    }

    @Override
    protected void renderBg(@Nonnull GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        updateFields();
        requestListsIfNeeded();
        populateList();
        refreshChannelEditor();
        refreshConnectorEditor();
        TileEntityController tileEntity = getBE();
        if (listsReady() && copyConnector != null && editingChannel != -1) {
            ChannelClientInfo info = tileEntity.clientChannels.get(editingChannel);
            ConnectorClientInfo clientInfo = findClientInfo(info, editingConnector);
            copyConnector.enabled(clientInfo != null);
        }
        if (tileEntity.clientChannels != null) {
            for (int i = 0; i < MAX_CHANNELS; i++) {
                String channel = String.valueOf(i + 1);
                ChannelClientInfo info = tileEntity.clientChannels.get(i);
                if (info != null) {
                    channelButtons[i]
                            .tooltips(I18nUtils.getSplitedTooltip(
                                    EDIT_CHANNEL_TOOLTIP_EXIST_FORMATTED.i18n((i + 1), info.getType().getName()))
                            );
                    IndicatorIcon icon = info.getChannelSettings().getIndicatorIcon();
                    if (icon != null) {
                        channelButtons[i].image(icon.getImage(), icon.getU(), icon.getV(), icon.getIw(), icon.getIh());
                    }
                    String indicator = info.getChannelSettings().getIndicator();
                    if (indicator != null) {
                        channelButtons[i].text(indicator + channel);
                    } else {
                        channelButtons[i].text(channel);
                    }
                } else {
                    channelButtons[i].image(null, 0, 0, 0, 0);
                    channelButtons[i].text(channel);
                }
            }
        }
        drawWindow(graphics, partialTicks, mouseX, mouseY);
        int channel = getSelectedChannel();
        if (channel != -1) {
            int x = (int) window.getToplevel().getBounds().getX();
            int y = (int) window.getToplevel().getBounds().getY();
            RenderHelper.drawVerticalGradientRect(x+channel * 14 + 41, y+22, x+channel * 14 + 41+12, y+230, 0x33aaffff, 0x33aaffff);
        }
        IEnergyStorage h = tileEntity.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, tileEntity.getBlockPos(), null);
        if (h != null) {
            long currentRF = h.getEnergyStored();
            int max = h.getMaxEnergyStored();
            energyBar.value(currentRF).maxValue(max);
        }
    }

    @Override
    protected void drawStackTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = GuiTools.getRelativeX(this);
        int y = GuiTools.getRelativeY(this);
        Widget<?> widget = window.getToplevel().getWidgetAtPosition(x, y);
        if (widget instanceof BlockRender) {
            if ("block".equals(widget.getUserObject())) {
                //System.out.println("GuiController.drawStackTooltips");
                return;     // Don't do the normal tooltip rendering
            }
        }
        super.drawStackTooltips(graphics, mouseX, mouseY);
    }
}
