package com.limechain.internal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bouncycastle.crypto.digests.Blake2bDigest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Getter
@Setter
@NoArgsConstructor
public class Node {
    public static final int CHILDREN_CAPACITY = 16;
    private byte[] partialKey;
    private byte[] storageValue;
    private int generation;
    private Node[] children;
    private boolean dirty;
    private byte[] merkleValue;
    private int descendants;

    private static byte[] getBlake2bHash(byte[] value) {
        Blake2bDigest blake2bDigest = new Blake2bDigest(256);
        byte[] rawHash = new byte[256];
        blake2bDigest.update(value, 0, value.length);
        blake2bDigest.doFinal(rawHash, 0);
        return rawHash;
    }

    public static byte[] getMerkleValueRoot(byte[] encoding) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        md.update(encoding);
        return md.digest();
    }

    public NodeKind getKind() {
        if (this.getChildren() != null) {
            return NodeKind.Branch;
        }
        return NodeKind.Leaf;
    }

    public void setChildrenAt(Node child, int position) {
        children[position] = child;
    }

    public boolean hasChild() {
        for (Node child : children) {
            if (child != null) {
                return true;
            }
        }
        return false;
    }
}
