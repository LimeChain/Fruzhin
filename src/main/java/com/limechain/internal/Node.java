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
    private byte[] merkleValue;
    private int descendants;

    /**
     * Returns the merkle value of the node
     *
     * @param encoding - the encoded node
     * @return the merkle value of the node
     */
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
        if (this.children == null) {
            this.children = new Node[CHILDREN_CAPACITY];
        }
        this.children[position] = child;
    }

    public boolean hasChild() {
        for (Node child : this.children) {
            if (child != null) {
                return true;
            }
        }
        return false;
    }

    public int getChildrenBitmap() {
        int bitmap = 0;
        for (int i = 0; i < this.children.length; i++) {
            if (this.children[i] != null) {
                bitmap |= 1 << i;
            }
        }
        return bitmap;
    }

    public int getChildrenCount() {
        int count = 0;
        for (Node child : this.children) {
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

    /**
     * Encodes the node and calculates the merkle value
     *
     * @return Pair of encoded node and merkle value
     */
    private Pair<byte[], byte[]> encodeAndHash() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            TreeEncoder.encode(this, out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        byte[] encoding = out.toByteArray();
        byte[] merkleValue = writeMerkleValue(encoding);
        this.setMerkleValue(merkleValue);
        return new Pair<>(encoding, merkleValue);
    }

    private byte[] writeMerkleValue(byte[] encoding) {
        ByteBuffer merkleValueBuf = ByteBuffer.allocate(Math.min(encoding.length, 32));
        if (encoding.length < 32) {
            merkleValueBuf.put(encoding);
            return merkleValueBuf.array();
        }
        merkleValueBuf.put(HashUtils.hashWithBlake2b(encoding));
        return merkleValueBuf.array();
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
