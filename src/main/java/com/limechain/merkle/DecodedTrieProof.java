package com.limechain.merkle;

import io.emeraldpay.polkaj.types.Hash256;

import java.util.HashMap;
import java.util.TreeMap;

public class DecodedTrieProof extends HashMap<Hash256, Integer> {

    public final TreeMap<Object, Object> entries;
    public final byte[] proofs;

    public DecodedTrieProof(byte[] proofs, TreeMap<Object, Object> entries) {
        this.proofs = proofs;
        this.entries = entries;
    }
}
