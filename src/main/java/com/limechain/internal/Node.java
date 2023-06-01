package com.limechain.internal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bouncycastle.crypto.digests.Blake2bDigest;
import org.bouncycastle.crypto.digests.Blake2sDigest;
import org.bouncycastle.jcajce.provider.digest.Blake2b;
import org.bouncycastle.oer.its.HashAlgorithm;

import java.math.BigInteger;

@Getter
@Setter
@NoArgsConstructor
public class Node {
    public static final int CHILDREN_CAPACITY = 16;
    byte[] partialKey;
    byte[] storageValue;
    BigInteger generation;
    Node[] children = new Node[16];
    boolean dirty;
    byte[] merkleValue;
    BigInteger descendants = BigInteger.ZERO;

    public void setChildrenAt(Node child, int position) {
        children[position] = child;
    }

    public byte[] MerkleValueRoot(byte[] encoding) {
        byte[] hashed = new Blake2b.Blake2b256().digest(encoding);
        return new byte[1];
    }

    
    private static byte[] blake2Hash(HashAlgorithm algorithm, byte[] value) {
        int digestLengthBytes = 32;
        Blake2bDigest blake2bDigest = new Blake2bDigest(digestLengthBytes * 8);
        byte[] rawHash = new byte[blake2bDigest.getDigestSize()];
        blake2bDigest.update(value, 0, value.length);
        blake2bDigest.doFinal(rawHash, 0);
        return rawHash;
    }
}
