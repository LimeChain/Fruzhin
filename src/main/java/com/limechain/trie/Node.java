package com.limechain.trie;

import com.limechain.trie.еncoder.TrieEncoder;
import com.limechain.utils.HashUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.javatuples.Pair;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
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
        if (children == null) return false;
        for (Node child : children) {
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
            TrieEncoder.encode(this, out);
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

    public int getStorageValueLength() {
        if (this.storageValue == null) {
            return 0;
        }
        return this.storageValue.length;
    }

    public Node getChild(int pos) {
        if (this.children == null) return null;
        else return this.children[pos];
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
