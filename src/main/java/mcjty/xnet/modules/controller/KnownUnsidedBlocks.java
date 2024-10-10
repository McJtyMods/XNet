package mcjty.xnet.modules.controller;

import mcjty.xnet.setup.Config;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

public class KnownUnsidedBlocks {

    private static final Set<ResourceLocation> UNSIDED_BLOCKS = new HashSet<>();

    public static boolean isUnsided(ResourceLocation resourceLocation) {
        if (UNSIDED_BLOCKS.isEmpty()) {
            for (String block : Config.unsidedBlocks.get()) {
                UNSIDED_BLOCKS.add(ResourceLocation.parse(block));
            }
        }
        return UNSIDED_BLOCKS.contains(resourceLocation);
    }

}
