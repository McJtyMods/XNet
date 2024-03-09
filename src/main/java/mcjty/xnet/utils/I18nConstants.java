package mcjty.xnet.utils;

import mcjty.lib.varia.ComponentFactory;

public enum I18nConstants {


    BLOCK_WIRELESS_ROUTER("block.xnet.wireless_router"),
    BLOCK_CONTROLLER("block.xnet.controller"),
    BLOCK_CONNECTOR("block.xnet.connector"),

    CHANNEL_ENERGY("xnet.channel.energy"),
    CHANNEL_FLUID("xnet.channel.fluid"),
    CHANNEL_ITEM("xnet.channel.item"),
    CHANNEL_LOGIC("xnet.channel.logic"),

    MIN("xnet.min"),
    MAX("xnet.max"),

    SPEED_TOOLTIP("xnet.speed.tooltip"),

    PRIORITY_LABEL("xnet.priority.label"),
    PRIORITY_TOOLTIP("xnet.priority.tooltip"),

    RATE_LABEL("xnet.rate.label"),
    FILTER_LABEL("xnet.filter.label"),
    EXT_ENDING("xnet.ext.ending"),
    INS_ENDING("xnet.ins.ending"),
    ENERGY_RATE_TOOLTIP_FORMATTED("xnet.energy.rate.tooltip.formatted"),
    LOW_FORMAT("xnet.low.format"),
    HIGH_FORMAT("xnet.high.format"),
    ENERGY_MINMAX_TOOLTIP_FORMATTED("xnet.energy.minmax.tooltip.formatted"),

    FLUID_RATE_TOOLTIP_FORMATTED("xnet.fluid.rate.tooltip.formatted"),
    FLUID_MINMAX_TOOLTIP_FORMATTED("xnet.fluid.minmax.tooltip.formatted"),

    ITEM_EXT_COUNT_TOOLTIP("xnet.item.ext.count.tooltip"),
    ITEM_COUNT_TOOLTIP_FORMATTED("xnet.item.count.tooltip.formatted"),
    ITEM_BLACKLIST_LABEL("xnet.item.blacklist.label"),
    ITEM_BLACKLIST_TOOLTIP("xnet.item.blacklist.tooltip"),
    ITEM_TAGS_LABEL("xnet.item.tags.label"),
    ITEM_TAGS_TOOLTIP("xnet.item.tags.tooltip"),
    ITEM_META_LABEL("xnet.item.meta.label"),
    ITEM_META_TOOLTIP("xnet.item.meta.tooltip"),
    ITEM_NBT_LABEL("xnet.item.nbt.label"),
    ITEM_NBT_TOOLTIP("xnet.item.nbt.tooltip"),
    ITEM_FILTER_INDEX_TOOLTIP("xnet.item.filter.index.tooltip"),
    ITEM_FILTER_OFF("xnet.item.filter.off"),

    LOGIC_INPUT_CHANNEL_TOOLTIP("xnet.logic.input.channel.tooltip"),
    LOGIC_COUNTER_FILTER_TOOLTIP("xnet.logic.counter.filter.tooltip"),
    LOGIC_TIMER_FILTER_TOOLTIP("xnet.logic.timer.filter.tooltip"),
    LOGIC_RS_LABEL("xnet.logic.rs.label"),
    LOGIC_RS_TOOLTIP("xnet.logic.rs.tooltip"),
    LOGIC_SENSOR_OPERATOR_TOOLTIP("xnet.logic.sensor.operator.tooltip"),
    LOGIC_SENSOR_AMOUNT_TOOLTIP("xnet.logic.sensor.amount.tooltip"),
    LOGIC_SENSOR_OUT_COLOR_TOOLTIP("xnet.logic.sensor.out.color.tooltip"),

    RS_MODE_IGNORED_TOOLTIP("xnet.rs.mode.ignored.tooltip"),
    RS_MODE_OFF_TOOLTIP("xnet.rs.mode.off.tooltip"),
    RS_MODE_ON_TOOLTIP("xnet.rs.mode.on.tooltip"),
    RS_MODE_PULSE_TOOLTIP("xnet.rs.mode.pulse.tooltip"),

    CONNECTOR_NAME_TOOLTIP("xnet.connector.name.tooltip"),
    CONNECTOR_COPY_TOOLTIP("xnet.connector.copy.tooltip"),
    CONNECTOR_UPGRADE("xnet.connector.upgrade"),
    CONNECTOR_WAS_UPGRADED("xnet.connector.was.upgraded"),
    CONNECTOR_ALREADY_ADVANCED("xnet.connector.already.advanced"),
    CONNECTOR_USE_IT_TO_UPGRADE("xnet.connector.use.it.to.upgrade"),
    CONNECTOR_REMOVE_TOOLTIP("xnet.connector.remove.tooltip"),
    CONNECTOR_PASTE_TOOLTIP("xnet.connector.paste.tooltip"),

    CREATE_LABEL("xnet.create.label"),
    PASTE_LABEL("xnet.paste.label"),
    CHANNEL_LABEL_FORMATTED("xnet.channel.label.formatted"),
    CHANNEL_ENABLE_TOOLTIP("xnet.channel.enable.tooltip"),
    CHANNEL_NAME_LABEL("xnet.channel.name.label"),
    CHANNEL_REMOVE_TOOLTIP("xnet.channel.remove.tooltip"),
    CHANNEL_COPY_TOOLTIP("xnet.channel.copy.tooltip"),
    CHANNEL_PASTE_TOOLTIP("xnet.channel.paste.tooltip"),

    FACADE_CURRENT_MIMIC_FORMATTED("xnet.facade.current.mimic.formatted"),

    DIRECTIONS_LABEL("xnet.directions.label"),
    CANCEL_LABEL("xnet.cancel.label"),
    NAME_LABEL("xnet.name.label"),
    CONNECTOR_LABEL("xnet.connector.label"),
    BLOCK_LABEL("xnet.block.label"),
    POSITON_LABEL("xnet.positon.label"),
    POS_LABEL("xnet.pos.label"),
    INDEX_LABEL("xnet.index.label"),
    DOUBLE_CLICK_HIGHLIGHT("xnet.double.click.highlight"),

    MESSAGE_BLOCK_HIGHLIGHTED("xnet.message.block.highlighted"),
    MESSAGE_CHANNEL_COPIED("xnet.message.channel.copied"),
    MESSAGE_CONFIRM_REMOVE_CHANNEL_FORMATTED("xnet.message.confirm.remove.channel.formatted"),
    ERROR_NOTHING_SELECTED("xnet.error.nothing.selected"),
    ERROR_COPY("xnet.error.copy"),
    ERROR_READ_COPY("xnet.error.read.copy"),
    ERROR_LARGE_COPY("xnet.error.large.copy"),
    ERROR_UNS_CH_FORMATTED("xnet.uns.ch.formatted");


    private final String langKey;

    I18nConstants(String langKey) {this.langKey = langKey;}

//    public String i18n() {
//
//    }

    public String i18n(Object... formatArgs) {
        if (formatArgs == null) {
            return ComponentFactory.translatable(this.langKey).getString();
        }
        return ComponentFactory.translatable(this.langKey, formatArgs).getString();
    }

}
