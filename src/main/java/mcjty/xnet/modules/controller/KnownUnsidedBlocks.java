package mcjty.xnet.modules.controller;

import mcjty.xnet.setup.Config;
import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.Set;

public class KnownUnsidedBlocks {

    private static final Set<Identifier> UNSIDED_BLOCKS = new HashSet<>();

    public static boolean isUnsided(Identifier resourceLocation) {
        if (UNSIDED_BLOCKS.isEmpty()) {
            for (String block : Config.unsidedBlocks.get()) {
                UNSIDED_BLOCKS.add(Identifier.parse(block));
            }
        }
        return UNSIDED_BLOCKS.contains(resourceLocation);
    }

}
