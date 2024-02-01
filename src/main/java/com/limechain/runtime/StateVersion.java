package com.limechain.runtime;

public enum StateVersion {
    V0,
    V1;

    public static StateVersion fromInt(int val) {
        return switch(val) {
            case 0 -> V0;
            case 1 -> V1;
            default -> throw new IllegalArgumentException("State version must be either 0 or 1.");
        };
    }
}
