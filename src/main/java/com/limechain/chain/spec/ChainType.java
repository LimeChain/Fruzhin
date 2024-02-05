package com.limechain.chain.spec;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * An enum modelling all possible chain types
 * (as per <a href="https://spec.polkadot.network/id-cryptography-encoding#section-chainspec">the spec</a>).
 */
public enum ChainType implements Serializable {
    @JsonProperty("Live")
    LIVE,

    @JsonProperty("Development")
    DEVELOPMENT,

    @JsonProperty("Local")
    LOCAL
}
