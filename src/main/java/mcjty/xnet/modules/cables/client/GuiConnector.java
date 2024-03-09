package mcjty.xnet.modules.cables.client;

import com.mojang.blaze3d.vertex.PoseStack;
import mcjty.lib.container.GenericContainer;
import mcjty.lib.gui.GenericGuiContainer;
import mcjty.lib.gui.Window;
import mcjty.lib.gui.widgets.Panel;
import mcjty.lib.gui.widgets.TextField;
import mcjty.lib.gui.widgets.ToggleButton;
import mcjty.lib.typed.TypedMap;
import mcjty.lib.varia.OrientationTools;
import mcjty.xnet.modules.cables.CableModule;
import mcjty.xnet.modules.cables.blocks.ConnectorTileEntity;
import mcjty.xnet.setup.XNetMessages;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.Nonnull;

import static mcjty.lib.gui.widgets.Widgets.horizontal;
import static mcjty.lib.gui.widgets.Widgets.label;
import static mcjty.lib.gui.widgets.Widgets.vertical;
import static mcjty.xnet.apiimpl.Constants.TAG_NAME;
import static mcjty.xnet.modules.cables.blocks.ConnectorTileEntity.CMD_ENABLE;
import static mcjty.xnet.modules.cables.blocks.ConnectorTileEntity.PARAM_ENABLED;
import static mcjty.xnet.modules.cables.blocks.ConnectorTileEntity.PARAM_FACING;
import static mcjty.xnet.utils.I18nConstants.CONNECTOR_NAME_TOOLTIP;
import static mcjty.xnet.utils.I18nConstants.DIRECTIONS_LABEL;
import static mcjty.xnet.utils.I18nConstants.NAME_LABEL;

public class GuiConnector extends GenericGuiContainer<ConnectorTileEntity, GenericContainer> {

    public static final int WIDTH = 220;
    public static final int HEIGHT = 50;

    private final ToggleButton[] toggleButtons = new ToggleButton[6];

//    public GuiConnector(AdvancedConnectorTileEntity te, EmptyContainer container, PlayerInventory inventory) {
//        this((ConnectorTileEntity) te, container, inventory);
//    }

    public GuiConnector(ConnectorTileEntity tileEntity, GenericContainer container, Inventory inventory) {
        super(tileEntity, container, inventory, CableModule.CONNECTOR.get().getManualEntry());

        imageWidth = WIDTH;
        imageHeight = HEIGHT;
    }

    public static void register() {
        register(CableModule.CONTAINER_CONNECTOR.get(), GuiConnector::new);
    }

    @Override
    public void init() {
        super.init();

        Panel toplevel = vertical().filledRectThickness(2);

        TextField nameField = new TextField().name(TAG_NAME).tooltips(CONNECTOR_NAME_TOOLTIP.i18n());

        Panel namePanel = horizontal().children(label(NAME_LABEL.i18n()), nameField);
        toplevel.children(namePanel);

        Panel togglePanel = horizontal().
                children(label(DIRECTIONS_LABEL.i18n()));
        for (Direction facing : OrientationTools.DIRECTION_VALUES) {
            toggleButtons[facing.ordinal()] = new ToggleButton().text(facing.getSerializedName().substring(0, 1).toUpperCase())
                .event(() -> {
                    sendServerCommandTyped(XNetMessages.INSTANCE, CMD_ENABLE,
                            TypedMap.builder()
                                    .put(PARAM_FACING, facing.ordinal())
                                    .put(PARAM_ENABLED, toggleButtons[facing.ordinal()].isPressed())
                                    .build());
                });
            toggleButtons[facing.ordinal()].pressed(tileEntity.isEnabled(facing));
            togglePanel.children(toggleButtons[facing.ordinal()]);
        }
        toplevel.children(togglePanel);

        toplevel.bounds(leftPos, topPos, WIDTH, HEIGHT);
        window = new Window(this, toplevel);

        window.bind(XNetMessages.INSTANCE, TAG_NAME, tileEntity, TAG_NAME);
    }

    @Override
    protected void renderBg(@Nonnull PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        drawWindow(matrixStack);
    }
}
