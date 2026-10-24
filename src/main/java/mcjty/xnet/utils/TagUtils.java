package mcjty.xnet.utils;

import net.minecraft.nbt.CompoundTag;

public class TagUtils {

    public static Integer getIntOrNull(CompoundTag tag, String key) {
        if (key == null || key.isEmpty() || !tag.contains(key)) {
            return null;
        } else {
            return tag.getIntOr(key, 0);
        }
    }

    public static Integer getIntOrValue(CompoundTag tag, String key, int value) {
        if (key == null || key.isEmpty() || !tag.contains(key)) {
            return value;
        } else {
            return tag.getIntOr(key, 0);
        }
    }

    public static String getStringOrNull(CompoundTag tag, String key) {
        if (key == null || key.isEmpty() || !tag.contains(key)) {
            return null;
        } else {
            return tag.getStringOr(key, "");
        }
    }

    public static void putIntIfNotNull(CompoundTag tag, String key, Integer value) {
        if (key != null && !key.isEmpty() && value != null) {
            tag.putInt(key, value);
        }
    }
}
