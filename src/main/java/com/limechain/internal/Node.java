package com.limechain.internal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bouncycastle.crypto.digests.Blake2bDigest;
import org.bouncycastle.jcajce.provider.digest.Blake2b;
import org.bouncycastle.oer.its.HashAlgorithm;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

    private static byte[] getBlake2bHash(HashAlgorithm algorithm, byte[] value) {
        Blake2bDigest blake2bDigest = new Blake2bDigest(256);
        byte[] rawHash = new byte[256];
        blake2bDigest.update(value, 0, value.length);
        blake2bDigest.doFinal(rawHash, 0);
        return rawHash;
    }

    public void setChildrenAt(Node child, int position) {
        children[position] = child;
    }

    public byte[] getMerkleValueRoot(byte[] encoding) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(encoding);
        return md.digest();
    }
}
