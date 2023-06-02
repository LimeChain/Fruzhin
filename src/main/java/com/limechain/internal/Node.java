package com.limechain.internal;

import com.limechain.utils.HashUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.javatuples.Pair;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

@Getter
@Setter
@NoArgsConstructor
public class Node {
    public static final int CHILDREN_CAPACITY = 16;
    private byte[] partialKey = new byte[]{};
    private byte[] storageValue;
    private int generation;
    private Node[] children;
    private boolean dirty;
    private byte[] merkleValue = new byte[]{};
    private int descendants;

    private static byte[] getBlake2bHash(byte[] value) {
        return HashUtils.hashWithBlake2b(value);
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

    public int getChildrenBitmap() {
        int bitmap = 0;
        for (int i = 0; i < children.length; i++) {
            if (children[i] != null) {
                bitmap |= 1 << i;
            }
        }
        return bitmap;
    }

    public int getChildrenCount() {
        int count = 0;
        for (Node child : children) {
            if (child != null) {
                count++;
            }
        }
        return count;
    }

    public byte[] calculateMerkleValue() {
        if (!this.isDirty() && this.getMerkleValue() != null) {
            return this.getMerkleValue();
        }

        return this.encodeAndHash().getValue1();
    }

    private Pair<byte[], byte[]> encodeAndHash() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            TreeEncoder.encode(this, out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        byte[] encoding = out.toByteArray();
        ByteBuffer merkleValueBuf = ByteBuffer.allocate(32);
        writeMerkleValue(encoding, merkleValueBuf);

        byte[] merkleValue = merkleValueBuf.array();
        this.setMerkleValue(merkleValue);
        return new Pair<>(encoding, merkleValue);
    }

    private void writeMerkleValue(byte[] encoding, ByteBuffer buffer) {
        if (encoding.length < 32) {
            buffer.put(encoding);
            return;
        }
        buffer.put(HashUtils.hashWithBlake2b(encoding));
    }

    @Override
    public String toString() {
        return "Node{" +
                "partialKey=" + Arrays.toString(partialKey) +
                ", storageValue=" + Arrays.toString(storageValue) +
                ", generation=" + generation +
                ", children=" + Arrays.toString(children) +
                ", dirty=" + dirty +
                ", merkleValue=" + Arrays.toString(merkleValue) +
                ", descendants=" + descendants +
                '}';
    }
}
