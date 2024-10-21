package mcjty.xnet.logic;

import mcjty.xnet.apiimpl.logic.RSOutput;

public class LogicOperations {


    public static boolean applyFilter (RSOutput connector, boolean input1, boolean input2) {
        switch (connector.getLogicFilter()) {
            case DIRECT, INVERTED, OR, AND, NOR, NAND, XOR, XNOR -> {return processImpulseMode(connector, input1, input2);}
            case LATCH -> {return toggleLatch(connector, input1);}
            case COUNTER -> {return counting(connector, input1);}
            case TIMER -> {return timer(connector);}
            case STATIC -> {return true;}
            default -> {return false;}
        }
    }

    private static boolean not(boolean input) {
        return !input;
    }
    private static boolean or(boolean input1, boolean input2) {
        return input1 | input2;
    }
    private static boolean and(boolean input1, boolean input2) {
        return input1 & input2;
    }
    private static boolean nor(boolean input1, boolean input2) {
        return !(input1 | input2);
    }
    private static boolean nand(boolean input1, boolean input2) {
        return !(input1 & input2);
    }
    private static boolean xor(boolean input1, boolean input2) {
        return input1 != input2;
    }
    private static boolean xnor(boolean input1, boolean input2) {
        return input1 == input2;
    }

    private static boolean toggleLatch(RSOutput connector, boolean input) {
        if (connector.isLastInputTrue() != input) {
            if (input) {
                connector.setFlipFlapState(!connector.isFlipFlapState());
            }
            connector.setLastInputTrue(input);
        }
        return connector.isFlipFlapState();
    }

    private static boolean counting(RSOutput connector, boolean input) {
        if (connector.isLastInputTrue() != input) {
            if (input) {
                connector.setCountingCurrent(connector.getCountingCurrent() + 1);
            }
            connector.setLastInputTrue(input);
        }
        if (connector.getCountingCurrent() >= connector.getCountingHolder()) {
            connector.setCountingCurrent(0);
            return true;
        }
        return false;
    }

    private static boolean timer(RSOutput connector) {
        if (connector.getTicksCurrent() >= connector.getTicksHolder()) {
            connector.setTicksCurrent(0);
            return true;
        } else {
            connector.setTicksCurrent(connector.getTicksCurrent() + 1);
        }
        return false;
    }

    private static boolean processImpulseMode(RSOutput connector, boolean input1, boolean input2) {
        boolean isFilterActive;
        switch (connector.getLogicFilter()) {
            case DIRECT -> {isFilterActive = input1;}
            case INVERTED -> {isFilterActive = not(input1);}
            case OR -> {isFilterActive = or(input1, input2);}
            case AND -> {isFilterActive = and(input1, input2);}
            case NOR -> {isFilterActive = nor(input1, input2);}
            case NAND -> {isFilterActive = nand(input1, input2);}
            case XOR -> {isFilterActive = xor(input1, input2);}
            case XNOR -> {isFilterActive = xnor(input1, input2);}
            default -> {return false;}
        }
        if (connector.isImpulse()) {
            if (connector.isLastInputTrue()) {
                connector.setLastInputTrue(isFilterActive);
                return connector.decreaseImpulseRemaining();
            }
            connector.setLastInputTrue(isFilterActive);
            if (isFilterActive) {
                connector.setAndDecreaseImpulseRemaining();
                return true;
            }
            return connector.decreaseImpulseRemaining();
        } else {
            return isFilterActive;
        }
    }

}
