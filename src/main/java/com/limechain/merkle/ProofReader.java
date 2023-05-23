package com.limechain.merkle;

import io.emeraldpay.polkaj.scale.ScaleCodecReader;

public class ProofReader implements io.emeraldpay.polkaj.scale.ScaleReader<Proof> {
    @Override
    public Proof read(ScaleCodecReader reader) {
        Proof proof = new Proof();
        proof.proof = reader.readByteArray();
        return proof;
    }
}
