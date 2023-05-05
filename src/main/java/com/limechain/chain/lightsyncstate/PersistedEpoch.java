package com.limechain.chain.lightsyncstate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PersistedEpoch {
    private BabeEpoch[] babeEpochs;
}
