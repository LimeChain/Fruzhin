package com.limechain.chain.spec;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public enum ChainType implements Serializable {
    @JsonProperty("Live")
    LIVE,

    @JsonProperty("Development")
    DEVELOPMENT,

    @JsonProperty("Local")
    LOCAL
}
