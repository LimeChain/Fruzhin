package com.limechain.chain;

import lombok.Getter;

/**
 * Stores the Polkadot chain the Host is running on.
 */
@Getter
public enum Chain {
    POLKADOT("polkadot"),
    KUSAMA("kusama"),
    LOCAL("local"),
    WESTEND("westend");

    /**
     * Holds the name of the chain
     */
    private final String value;

    Chain(String value) {
        this.value = value;
    }

    /**
     * Tries to map string parameter to an enum value
     *
     * @param chain name of the enum value to map
     * @return {@link Chain} or null if mapping is unsuccessful
     */
    public static Chain fromString(String chain) {
        for (Chain type : values()) {
            if (type.getValue().equals(chain)) {
                return type;
            }
        }
        return null;
    }
}
