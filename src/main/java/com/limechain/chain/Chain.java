package com.limechain.chain;

public enum Chain {
    POLKADOT("polkadot"),
    KUSAMA("kusama"),
    WESTEND("westend");

    private final String value;

    public static Chain fromString(String chain) {
        for (Chain type : values()) {
            if (type.getValue() == chain) {
                return type;
            }
        }
        return null;
    }

    Chain(String value) {
        this.value = value;
    }

    public final String getValue() {
        return value;
    }
}