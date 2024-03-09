package mcjty.xnet.apiimpl.logic.enums;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;

public enum Operator {
    EQUAL("=", Integer::equals),
    NOTEQUAL("!=", (i1, i2) -> !i1.equals(i2)),
    LESS("<", (i1, i2) -> i1 < i2),
    GREATER(">", (i1, i2) -> i1 > i2),
    LESSOREQUAL("<=", (i1, i2) -> i1 <= i2),
    GREATOROREQUAL(">=", (i1, i2) -> i1 >= i2);

    private final String code;
    private final BiPredicate<Integer, Integer> matcher;

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
}
