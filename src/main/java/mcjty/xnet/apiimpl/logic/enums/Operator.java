package mcjty.xnet.apiimpl.logic.enums;

import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;

public enum Operator implements StringRepresentable {
    EQUAL("=", Integer::equals),
    NOTEQUAL("!=", (i1, i2) -> !i1.equals(i2)),
    LESS("<", (i1, i2) -> i1 < i2),
    GREATER(">", (i1, i2) -> i1 > i2),
    LESSOREQUAL("<=", (i1, i2) -> i1 <= i2),
    GREATOROREQUAL(">=", (i1, i2) -> i1 >= i2);

    private final String code;
    private final BiPredicate<Integer, Integer> matcher;

    public static final Codec<Operator> CODEC = StringRepresentable.fromEnum(Operator::values);
    public static final StreamCodec<FriendlyByteBuf, Operator> STREAM_CODEC = NeoForgeStreamCodecs.enumCodec(Operator.class);

    private static final Map<String, Operator> OPERATOR_MAP = new HashMap<>();

    static {
        for (Operator operator : values()) {
            OPERATOR_MAP.put(operator.code, operator);
        }
    }

    Operator(String code, BiPredicate<Integer, Integer> matcher) {
        this.code = code;
        this.matcher = matcher;
    }

    public String getCode() {
        return code;
    }

    public boolean match(int i1, int i2) {
        return matcher.test(i1, i2);
    }

    @Override
    public String toString() {
        return code;
    }

    public static Operator valueOfCode(String code) {
        return OPERATOR_MAP.get(code);
    }

    @Override
    public String getSerializedName() {
        return name();
    }
}
