package mcjty.xnet.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import mcjty.lib.base.StyleConfig;
import mcjty.lib.client.RenderHelper;
import mcjty.lib.gui.GuiParser;
import mcjty.lib.gui.Window;
import mcjty.lib.gui.widgets.AbstractLabel;
import mcjty.lib.gui.widgets.Widget;
import mcjty.lib.typed.Key;
import mcjty.lib.typed.Type;
import mcjty.lib.typed.TypedMap;
import mcjty.lib.varia.SafeClientTools;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class EnumChoiceLabel extends AbstractLabel<EnumChoiceLabel>  {

    public static final String TYPE_ENUM_CHOICE_LABEL = "enumchoicelabel";
    public static final Key<Integer> PARAM_CHOICE = new Key<>("choice", Type.INTEGER);
    public static final Key<Integer> PARAM_CHOICE_IDX = new Key<>("choiceIdx", Type.INTEGER);

    private Integer choiceIndex = null;
    private ITranslatableEnum<?>[] enumChoices = null;
    private final Map<ITranslatableEnum<?>, List<String>> tooltipMap = new HashMap<>();
    private List<ChoiceEvent<ITranslatableEnum<?>>> choiceEvents = null;

    public EnumChoiceLabel() {
        text("");
    }

    private void setCurrentChoice(ITranslatableEnum<?> enumChoice) {
        this.choiceIndex = enumChoice.ordinal();
    }

    public EnumChoiceLabel choices(ITranslatableEnum<?>[] choices) {
        enumChoices = choices;
        if (choiceIndex == null) {
            choiceIndex = choices[0].ordinal();
            text(choices[0].getI18n());
            fireChoiceEvents(choices[0]);
        }
        if (tooltipMap.isEmpty()) {
            for (ITranslatableEnum<?> choice : choices) {
                setChoiceTooltip(choice);
            }
        }

        return this;
    }

    public EnumChoiceLabel setChoiceTooltip(ITranslatableEnum<?> choice) {
        tooltipMap.put(choice, Arrays.asList(choice.getI18nSplitedTooltip()));
        return this;
    }

    public EnumChoiceLabel choice(ITranslatableEnum<?> choice) {
        if (Objects.equals(choiceIndex, choice.ordinal())) {
            return this;
        }
        choiceIndex = choice.ordinal();
        text(choice.getI18n());
        return this;
    }

    @Override
    public List<String> getTooltips() {
        List<String> tooltips = tooltipMap.get(enumChoices[choiceIndex]);
        if (tooltips == null) {
            return super.getTooltips();
        } else {
            return tooltips;
        }
    }

    @Override
    public void draw(Screen gui, PoseStack matrixStack, int x, int y) {
        if (!visible) {
            return;
        }
        int xx = x + bounds.x;
        int yy = y + bounds.y;

        if (isEnabled()) {
            if (isHovering()) {
                drawStyledBoxHovering(window, matrixStack, xx, yy, xx + bounds.width - 1, yy + bounds.height - 1);
            } else {
                drawStyledBoxNormal(window, matrixStack, xx, yy, xx + bounds.width - 1, yy + bounds.height - 1);
            }
            RenderHelper.drawLeftTriangle(matrixStack, xx + bounds.width - 10, yy + bounds.height / 2, StyleConfig.colorCycleButtonTriangleNormal);
            RenderHelper.drawRightTriangle(matrixStack, xx + bounds.width - 4, yy + bounds.height / 2, StyleConfig.colorCycleButtonTriangleNormal);
        } else {
            drawStyledBoxDisabled(window, matrixStack, xx, yy, xx + bounds.width - 1, yy + bounds.height - 1);
            RenderHelper.drawLeftTriangle(matrixStack, xx + bounds.width - 10, yy + bounds.height / 2, StyleConfig.colorCycleButtonTriangleDisabled);
            RenderHelper.drawRightTriangle(matrixStack, xx + bounds.width - 4, yy + bounds.height / 2, StyleConfig.colorCycleButtonTriangleDisabled);
        }

        super.drawOffset(gui, matrixStack, x, y, -3, 1);
    }

    @Override
    public Widget<?> mouseClick(double x, double y, int button) {
        if (isEnabledAndVisible()) {
            if (enumChoices.length == 0) {
                return null;
            }
            if (button == 1 || SafeClientTools.isSneaking()) {
                choiceIndex--;
                if (choiceIndex < 0) {
                    choiceIndex = enumChoices.length - 1;
                }
            } else {
                choiceIndex++;
                if (choiceIndex >= enumChoices.length) {
                    choiceIndex = 0;
                }
            }

            text(enumChoices[choiceIndex].getI18n());
            fireChoiceEvents(enumChoices[choiceIndex]);
        }
        return null;
    }

    public EnumChoiceLabel event(ChoiceEvent<ITranslatableEnum<?>> event) {
        if (choiceEvents == null) {
            choiceEvents = new ArrayList<>();
        }
        choiceEvents.add(event);
        return this;
    }

    public void removeChoiceEvent(ChoiceEvent<ITranslatableEnum<?>> event) {
        if (choiceEvents != null) {
            choiceEvents.remove(event);
        }
    }

    private void fireChoiceEvents(ITranslatableEnum<?> choice) {

        fireChannelEvents(TypedMap.builder()
                                  .put(Window.PARAM_ID, "choice")
                                  .put(PARAM_CHOICE, choice.ordinal())
                                  .put(PARAM_CHOICE_IDX, choice.ordinal())
                                  .build());

        if (choiceEvents != null) {
            for (ChoiceEvent<ITranslatableEnum<?>> event : choiceEvents) {
                event.choiceChanged(choice);
            }
        }
    }

    @Override
    public void readFromGuiCommand(GuiParser.GuiCommand command) {
        System.out.println("readFromGuiCommand");
        super.readFromGuiCommand(command);
        command.findCommand("choices").ifPresent(cmd -> {
            cmd.commands().forEach(choiceCmd -> {
                Integer enumIndex = choiceCmd.getOptionalPar(0, 0);

//                Arrays.fill(enumChoices, choice);
                choiceCmd.findCommand("tooltips")
                        .ifPresent(tooltipsCmd -> tooltipMap.put(enumChoices[enumIndex], tooltipsCmd.parameters()
                                                                                 .map(Object::toString)
                                                                                 .collect(Collectors.toList())));
            });
        });
    }

    @Override
    public void fillGuiCommand(GuiParser.GuiCommand command) {
        System.out.println("fillGuiCommand");
        super.fillGuiCommand(command);
        command.removeParameter(1); // We don't need the name as set by the label
        GuiParser.GuiCommand choicesCmd = new GuiParser.GuiCommand("choices");
        for (ITranslatableEnum<?> s : enumChoices) {
            GuiParser.GuiCommand choiceCmd = new GuiParser.GuiCommand("choice").parameter(s.ordinal());
            choicesCmd.command(choiceCmd);
            List<String> tooltips = tooltipMap.get(s);
            if (tooltips != null && !tooltips.isEmpty()) {
                GuiParser.GuiCommand tooltipsCmd = new GuiParser.GuiCommand("tooltips");
                choiceCmd.command(tooltipsCmd);
                for (String tt : tooltips) {
                    tooltipsCmd.parameter(tt);
                }
            }
        }
        command.command(choicesCmd);
    }

    @Override
    public GuiParser.GuiCommand createGuiCommand() {
        return new GuiParser.GuiCommand(TYPE_ENUM_CHOICE_LABEL);
    }
}
