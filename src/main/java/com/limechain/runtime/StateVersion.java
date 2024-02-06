package com.limechain.runtime;

public enum StateVersion {
    V0(0),
    V1(1);

    private final int value;

    StateVersion(int value) {
        this.value = value;
    }

    public static StateVersion fromInt(int val) {
        return switch(val) {
            case 0 -> V0;
            case 1 -> V1;
            default -> throw new IllegalArgumentException("State version must be either 0 or 1.");
        };
    }

    public int asInt() {
        return value;
    }
}
