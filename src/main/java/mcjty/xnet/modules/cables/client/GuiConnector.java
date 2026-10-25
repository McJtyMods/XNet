package mcjty.xnet.modules.cables.client;

import mcjty.lib.container.GenericContainer;
import mcjty.lib.gui.GenericGuiContainer;
import mcjty.lib.gui.Window;
import mcjty.lib.gui.layout.HorizontalAlignment;
import mcjty.lib.gui.layout.VerticalAlignment;
import mcjty.lib.gui.widgets.*;
import mcjty.lib.network.Networking;
import mcjty.lib.network.PacketRequestDataFromServer;
import mcjty.lib.typed.TypedMap;
import mcjty.lib.varia.OrientationTools;
import mcjty.xnet.modules.cables.CableModule;
import mcjty.xnet.modules.cables.ConnectorType;
import mcjty.xnet.modules.cables.blocks.ConnectorTileEntity;
import mcjty.xnet.modules.cables.blocks.GenericCableBlock;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import javax.annotation.Nonnull;

import static mcjty.lib.gui.widgets.Widgets.*;
import static mcjty.xnet.apiimpl.Constants.TAG_NAME;
import static mcjty.xnet.modules.cables.blocks.ConnectorTileEntity.*;
import static mcjty.xnet.utils.I18nConstants.*;

public class GuiConnector extends GenericGuiContainer<ConnectorTileEntity, GenericContainer> {

    public static final int WIDTH = 230;
    public static final int HEIGHT = 60;

    private final ToggleButton[] toggleButtons = new ToggleButton[6];
    private final BlockRender[] connectedBlockRenders = new BlockRender[6];

//    public GuiConnector(AdvancedConnectorTileEntity te, EmptyContainer container, PlayerInventory inventory) {
//        this((ConnectorTileEntity) te, container, inventory);
//    }

    public GuiConnector(GenericContainer container, Inventory inventory, Component title) {
        super(container, inventory, title, CableModule.CONNECTOR.get().getManualEntry());

        Level level = getBE().getLevel();
        if (level == null) {
            return;
        }
        Networking.sendToServer(new PacketRequestDataFromServer(level.dimension(), getBE().getBlockPos(), CMD_GET_NAME.name(), TypedMap.EMPTY, false));

        setWindowDimensions(WIDTH, HEIGHT);
    }

    public static void register() {
        net.minecraft.client.gui.screens.MenuScreens.register(CableModule.CONTAINER_CONNECTOR.get(), GuiConnector::new);
    }

    @Override
    public void init() {
        super.init();

        Panel toplevel = vertical().filledRectThickness(2);
        ConnectorTileEntity connector = getBE();
        Level level = connector.getLevel();
        if (level == null) {
            return;
        }
        BlockEntity te = level.getBlockEntity(connector.getBlockPos());
        if (te instanceof ConnectorTileEntity) {
            connector.setConnectorName(((ConnectorTileEntity) te).getConnectorName());
        }
        String connectorName = connector.getConnectorName();
        TextField nameField = new TextField().name(TAG_NAME).tooltips(CONNECTOR_NAME_TOOLTIP.i18n());

        if (connectorName != null && ! connectorName.isEmpty()) {
            nameField.text(connector.getConnectorName());
        }
        Label nameLabel = label(NAME_LABEL.i18n())
                                  .verticalAlignment(VerticalAlignment.ALIGN_CENTER)
                                  .horizontalAlignment(HorizontalAlignment.ALIGN_LEFT);
        Panel namePanel = horizontal().children(nameLabel, nameField).desiredHeight(20);
        toplevel.children(namePanel);
        Label directionsLabel = label(DIRECTIONS_LABEL.i18n())
                                        .desiredWidth(70)
                                        .horizontalAlignment(HorizontalAlignment.ALIGN_LEFT);
        Panel directionsPanel = horizontal().children(directionsLabel);
        Panel directionValuesPanel = vertical();
        Panel directionNamesPanel = horizontal(6, 5);
        Panel directionBlocksPanel = horizontal();
        EnumProperty<ConnectorType>[] properties = new EnumProperty[] {
                GenericCableBlock.DOWN, GenericCableBlock.UP,
                GenericCableBlock.NORTH, GenericCableBlock.SOUTH,
                GenericCableBlock.WEST, GenericCableBlock.EAST
        };

        for (Direction facing : OrientationTools.DIRECTION_VALUES) {
            BlockPos consumerPos = connector.getBlockPos().relative(facing);

            BlockState state = level.getBlockState(consumerPos);
            ItemStack item = state.getCloneItemStack(level, consumerPos, false);
            connectedBlockRenders[facing.ordinal()] = new BlockRender().renderItem(item).userObject("block");
            toggleButtons[facing.ordinal()] = new ToggleButton().text(facing.getSerializedName().substring(0, 1).toUpperCase())
                                                      .desiredWidth(16)
                                                      .verticalAlignment(VerticalAlignment.ALIGN_CENTER)
                                                      .horizontalAlignment(HorizontalAlignment.ALIGN_CENTER)
                .event(() -> {
                    sendServerCommandTyped(CMD_ENABLE,
                            TypedMap.builder()
                                    .put(PARAM_FACING, facing.ordinal())
                                    .put(PARAM_ENABLED, toggleButtons[facing.ordinal()].isPressed())
                                    .build());
                });
            boolean isEnabled = !connector.getBlockState().getValue(properties[facing.ordinal()]).equals(ConnectorType.NONE);
            toggleButtons[facing.ordinal()].pressed(isEnabled);
            directionNamesPanel.children(toggleButtons[facing.ordinal()]);
        }
        directionBlocksPanel.children(connectedBlockRenders);
        directionValuesPanel.children(directionBlocksPanel, directionNamesPanel);
        directionsPanel.children(directionValuesPanel);
        toplevel.children(directionsPanel);
        toplevel.bounds(leftPos, topPos, WIDTH, HEIGHT);
        window = new Window(this, toplevel);

        window.bind(TAG_NAME, connector, TAG_NAME);
    }

    @Override
    protected void renderBg(@Nonnull GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {
        drawWindow(graphics, partialTicks, mouseX, mouseY);
    }
}
