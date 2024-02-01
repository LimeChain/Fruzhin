package com.limechain.chain.spec;

import lombok.Getter;

// Serves as a wrapper around the raw JSON parsed chain spec
// for easier in-memory access to the spec data
@Getter
public class ChainSpec {
    // we store the raw chain spec in order to still access it, since this class hasn't got its full API yet
    // (extend as needed, WIP)
    // "raw" meaning as-is deserialized from the json file
    // TODO: Maybe think of a better name...? To not be confused with genesis storage's "raw" field?
    private final RawChainSpec rawChainSpec;

    private final Genesis genesis;

    private ChainSpec(RawChainSpec rawChainSpec) {
        this.rawChainSpec = rawChainSpec;
        this.genesis = new Genesis(rawChainSpec);
    }

    public static ChainSpec fromRaw(RawChainSpec rawChainSpec) {
        return new ChainSpec(rawChainSpec);
    }
}
