package com.limechain.runtime.hostapi;

import com.limechain.storage.crypto.KeyType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SeedStringKeyTypePair {
    private String seed;
    private KeyType keyType;
}
