package mcjty.xnet.setup;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.google.common.collect.Lists;
import mcjty.lib.modules.Modules;
import mcjty.xnet.modules.wireless.blocks.TileEntityWirelessRouter;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.nio.file.Path;
import java.util.List;

public class Config {
    public static final String CATEGORY_GENERAL = "general";

    public static ForgeConfigSpec.IntValue controllerMaxRF;
    public static ForgeConfigSpec.IntValue controllerRfPerTick;

    public static ForgeConfigSpec.IntValue wirelessRouterMaxRF;
    public static ForgeConfigSpec.IntValue wirelessRouterRfPerTick;
    public static ForgeConfigSpec.IntValue wirelessRouterRfPerChannel[] = new ForgeConfigSpec.IntValue[3];

    public static ForgeConfigSpec.IntValue maxRfConnector;
    public static ForgeConfigSpec.IntValue maxRfAdvancedConnector;

    public static ForgeConfigSpec.IntValue maxRfRateNormal;
    public static ForgeConfigSpec.IntValue maxRfRateAdvanced;

    public static ForgeConfigSpec.IntValue maxFluidRateNormal;
    public static ForgeConfigSpec.IntValue maxFluidRateAdvanced;

    public static ForgeConfigSpec.IntValue controllerMaxPaste;     // Maximum size of the copy/paste buffer for the controller
    public static ForgeConfigSpec.IntValue controllerRFT;          // RF per tick that the controller uses all the time
    public static ForgeConfigSpec.IntValue controllerChannelRFT;   // RF Per tick per enabled channel
    public static ForgeConfigSpec.IntValue controllerOperationRFT; // RF Per tick per operation

    public static ForgeConfigSpec.IntValue maxPublishedChannels;    // Maximum number of published channels on a routing network

    public static ForgeConfigSpec.IntValue antennaTier1Range;
    public static ForgeConfigSpec.IntValue antennaTier2Range;

    public static ForgeConfigSpec.BooleanValue showNonFacadedCablesWhileSneaking;

    private static String[] unsidedBlocksAr = new String[] {
            "minecraft:chest",
            "minecraft:trapped_chest",
            "rftools:modular_storage",
            "rftools:storage_scanner",
            "rftools:pearl_injector",
    };

    public static Integer getMaxRfRate(boolean isAdvanced) {
        return isAdvanced ? maxRfRateAdvanced.get() : maxRfRateNormal.get();
    }

    public static Integer getMaxFluidRate(boolean isAdvanced) {
        return isAdvanced ? maxFluidRateAdvanced.get() : maxFluidRateNormal.get();
    }
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> unsidedBlocks;

    public static final ForgeConfigSpec.Builder SERVER_BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();

    public static void register(Modules modules) {
        SERVER_BUILDER.comment("General settings").push(CATEGORY_GENERAL);
        CLIENT_BUILDER.comment("General settings").push(CATEGORY_GENERAL);


        unsidedBlocks = SERVER_BUILDER
                .comment("This is a list of blocks that XNet considers to be 'unsided' meaning that it doesn't matter from what side you access things. This is currently only used to help with pasting channels")
                .defineList("unsidedBlocks", Lists.newArrayList(unsidedBlocksAr), s -> s instanceof String);

        controllerMaxRF = SERVER_BUILDER
                .comment("Maximum RF the controller can store")
                .defineInRange("controllerMaxRF", 100000, 1, 1000000000);
        controllerRfPerTick = SERVER_BUILDER
                .comment("Maximum RF the controller can receive per tick")
                .defineInRange("controllerRfPerTick", 1000, 1, 1000000000);
        wirelessRouterMaxRF = SERVER_BUILDER
                .comment("Maximum RF the wireless router can store")
                .defineInRange("wirelessRouterMaxRF", 100000, 1, 1000000000);
        wirelessRouterRfPerTick = SERVER_BUILDER
                .comment("Maximum RF the wireless router can receive per tick")
                .defineInRange("wirelessRouterRfPerTick", 5000, 1, 1000000000);

        wirelessRouterRfPerChannel[TileEntityWirelessRouter.TIER_1] = SERVER_BUILDER
                .comment("Maximum RF per tick the wireless router (tier 1) needs to publish a channel")
                .defineInRange("wireless1RfPerChannel", 20, 0, 1000000000);
        wirelessRouterRfPerChannel[TileEntityWirelessRouter.TIER_2] = SERVER_BUILDER
                .comment("Maximum RF per tick the wireless router (tier 2) needs to publish a channel")
                .defineInRange("wireless2RfPerChannel", 50, 0, 1000000000);
        wirelessRouterRfPerChannel[TileEntityWirelessRouter.TIER_INF] = SERVER_BUILDER
                .comment("Maximum RF per tick the wireless router (infinite tier) needs to publish a channel")
                .defineInRange("wirelessInfRfPerChannel", 200, 0, 1000000000);

        maxRfConnector = SERVER_BUILDER
                .comment("Maximum RF the normal connector can store")
                .defineInRange("maxRfConnector", 50000, 1, 1000000000);
        maxRfAdvancedConnector = SERVER_BUILDER
                .comment("Maximum RF the advanced connector can store")
                .defineInRange("maxRfAdvancedConnector", 500000, 1, 1000000000);
        maxRfRateNormal = SERVER_BUILDER
                .comment("Maximum RF/rate that a normal connector can input or output")
                .defineInRange("maxRfRateNormal", 10000, 1, 1000000000);
        maxRfRateAdvanced = SERVER_BUILDER
                .comment("Maximum RF/rate that an advanced connector can input or output")
                .defineInRange("maxRfRateAdvanced", 100000, 1, 1000000000);
        maxFluidRateNormal = SERVER_BUILDER
                .comment("Maximum fluid per operation that a normal connector can input or output")
                .defineInRange("maxFluidRateNormal", 1000, 1, 1000000000);
        maxFluidRateAdvanced = SERVER_BUILDER
                .comment("Maximum fluid per operation that an advanced connector can input or output")
                .defineInRange("maxFluidRateAdvanced", 5000, 1, 1000000000);

        maxPublishedChannels = SERVER_BUILDER
                .comment("Maximum number of published channels that a routing channel can support")
                .defineInRange("maxPublishedChannels", 32, 1, 1000000000);

        controllerMaxPaste = CLIENT_BUILDER
                .comment("Maximum size of the packet used to send copy/pasted channels/connectors to the server. -1 means no maximum (packets are split)")
                .defineInRange("controllerMaxPaste", -1, -1, 1000000000);

        controllerRFT = SERVER_BUILDER
                .comment("Power usage for the controller regardless of what it is doing")
                .defineInRange("controllerRFPerTick", 0, 0, 1000000000);
        controllerChannelRFT = SERVER_BUILDER
                .comment("Power usage for the controller per active channel")
                .defineInRange("controllerChannelRFT", 1, 0, 1000000000);
        controllerOperationRFT = SERVER_BUILDER
                .comment("Power usage for the controller per operation performed by one of the channels")
                .defineInRange("controllerOperationRFT", 2, 0, 1000000000);
        showNonFacadedCablesWhileSneaking = CLIENT_BUILDER
                .comment("If true then cables are also shown when sneaking even if they are not in a facade")
                .define("showNonFacadedCablesWhileSneaking", true);

        antennaTier1Range = SERVER_BUILDER
                .comment("Range for a tier 1 antenna")
                .defineInRange("antennaTier1Range", 100, 0, 1000000000);
        antennaTier2Range = SERVER_BUILDER
                .comment("Range for a tier 2 antenna")
                .defineInRange("antennaTier2Range", 500, 0, 1000000000);

        SERVER_BUILDER.pop();
        CLIENT_BUILDER.pop();

        SERVER_CONFIG = SERVER_BUILDER.build();
        CLIENT_CONFIG = CLIENT_BUILDER.build();

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_CONFIG);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SERVER_CONFIG);
    }

    public static ForgeConfigSpec SERVER_CONFIG;
    public static ForgeConfigSpec CLIENT_CONFIG;

    public static void loadConfig(ForgeConfigSpec spec, Path path) {

        final CommentedFileConfig configData = CommentedFileConfig.builder(path)
                .sync()
                .autosave()
                .writingMode(WritingMode.REPLACE)
                .build();

        configData.load();
        spec.setConfig(configData);
    }
}
