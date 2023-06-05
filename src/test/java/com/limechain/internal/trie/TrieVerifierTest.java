package com.limechain.internal.trie;

import com.limechain.internal.Node;
import com.limechain.internal.TreeEncoder;
import com.limechain.internal.Trie;
import com.limechain.internal.tree.decoder.TrieDecoderException;
import com.limechain.internal.trie.TrieVerifier;
import com.limechain.utils.HashUtils;
import com.limechain.utils.RandomGenerationUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class TrieVerifierTest {
    Node leafAShort = new Node() {{
        this.setPartialKey(new byte[]{1});
        this.setStorageValue(new byte[]{2});
        this.setDirty(true);
    }};

    Node leafBLarge = new Node() {{
        this.setPartialKey(new byte[]{2});
        this.setStorageValue(RandomGenerationUtils.generateBytes(40));
        this.setDirty(true);
    }};

    Node leafCLarge = new Node() {{
        this.setPartialKey(new byte[]{3});
        this.setStorageValue(RandomGenerationUtils.generateBytes(40));
        this.setDirty(true);
    }};


    @Test
    void buildTrieThrowsEmptyProofError() {
        assertThrows(IllegalStateException.class, () -> TrieVerifier.buildTrie(new byte[][]{}, new byte[]{1}));
    }

    @Test
    void buildTrieThrowsHeaderByteDecodingError() {
        var encodedProofNodes = new byte[][]{getBadNodeEncoding()};
        var rootHash = HashUtils.hashWithBlake2b(getBadNodeEncoding());
        assertThrows(TrieDecoderException.class, () -> TrieVerifier.buildTrie(encodedProofNodes, rootHash));
    }

    @Test
    void buildTrieThrowsRootProofEncodingLessThan32Bytes() throws Exception {
        assertShortEncoding(leafAShort);

        var encodedProofNodes = new byte[][]{getBadNodeEncoding()};
        var rootHash = HashUtils.hashWithBlake2b(getBadNodeEncoding());
        assertThrows(TrieDecoderException.class, () -> TrieVerifier.buildTrie(encodedProofNodes, rootHash));
    }

    @Test
    void buildTrieRootProofEncodingLessThan32Bytes() throws Exception {
        assertShortEncoding(leafAShort);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        TreeEncoder.encode(leafAShort, outputStream);

        byte[] rootHash = HashUtils.hashWithBlake2b(outputStream.toByteArray());
        byte[][] encodedProofNodes = new byte[][]{outputStream.toByteArray()};
        Trie expectedTrie = Trie.newTrie(leafAShort);
        Trie trie = TrieVerifier.buildTrie(encodedProofNodes, rootHash);

        assertEquals(expectedTrie.toString(), trie.toString());
    }

    @Test
    void buildTrieRootProofEncodingMoreThan32Bytes() throws Exception {
        assertLongEncoding(leafBLarge);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        TreeEncoder.encode(leafBLarge, outputStream);

        byte[] rootHash = HashUtils.hashWithBlake2b(outputStream.toByteArray());
        byte[][] encodedProofNodes = new byte[][]{outputStream.toByteArray()};
        Trie expectedTrie = Trie.newTrie(leafBLarge);
        Trie trie = TrieVerifier.buildTrie(encodedProofNodes, rootHash);

        assertEquals(expectedTrie.toString(), trie.toString());
    }

    @Test
    void buildTrieDiscardUnusedNode() throws Exception {
        assertShortEncoding(leafAShort);
        assertLongEncoding(leafBLarge);

        ByteArrayOutputStream leafAEncodedStream = new ByteArrayOutputStream();
        TreeEncoder.encode(leafAShort, leafAEncodedStream);
        ByteArrayOutputStream leafBEncodedStream = new ByteArrayOutputStream();
        TreeEncoder.encode(leafBLarge, leafBEncodedStream);

        byte[] rootHash = HashUtils.hashWithBlake2b(leafAEncodedStream.toByteArray());
        byte[][] encodedProofNodes = new byte[][]{leafAEncodedStream.toByteArray(), leafBEncodedStream.toByteArray()};
        Trie expectedTrie = Trie.newTrie(leafAShort);
        Trie trie = TrieVerifier.buildTrie(encodedProofNodes, rootHash);

        assertEquals(expectedTrie.toString(), trie.toString());
    }

    @Test
    void buildTrieMultipleUnorderedNodes() throws Exception {
        ByteArrayOutputStream leafBEncodedStream = new ByteArrayOutputStream();
        TreeEncoder.encode(leafBLarge, leafBEncodedStream);

        assertShortEncoding(leafAShort);
        assertLongEncoding(leafBLarge);
        assertLongEncoding(leafCLarge);

        ByteArrayOutputStream leafAEncodedStream = new ByteArrayOutputStream();
        TreeEncoder.encode(leafAShort, leafAEncodedStream);

        ByteArrayOutputStream leafCEncodedStream = new ByteArrayOutputStream();
        TreeEncoder.encode(leafCLarge, leafCEncodedStream);

        ByteArrayOutputStream rootNodeEncodedStream = new ByteArrayOutputStream();
        TreeEncoder.encode(new Node() {{
            this.setPartialKey(new byte[]{1});
            this.setChildren(padRightChildren(new Node[]{leafAShort, leafBLarge, leafCLarge, leafBLarge}));
        }}, rootNodeEncodedStream);

        byte[] rootHash = HashUtils.hashWithBlake2b(rootNodeEncodedStream.toByteArray());
        byte[][] encodedProofNodes = new byte[][]{
                leafBEncodedStream.toByteArray(),
                rootNodeEncodedStream.toByteArray(),
                leafCEncodedStream.toByteArray()
        };
        Trie expectedTrie = Trie.newTrie(new Node() {{
            this.setPartialKey(new byte[]{1});
            this.setDescendants(4);
            this.setDirty(true);
            this.setChildren(padRightChildren(new Node[]{
                    new Node() {{
                        this.setPartialKey(leafAShort.getPartialKey());
                        this.setStorageValue(leafAShort.getStorageValue());
                        this.setDirty(true);
                    }},
                    new Node() {{
                        this.setPartialKey(leafBLarge.getPartialKey());
                        this.setStorageValue(leafBLarge.getStorageValue());
                        this.setDirty(true);
                    }},
                    new Node() {{
                        this.setPartialKey(leafCLarge.getPartialKey());
                        this.setStorageValue(leafCLarge.getStorageValue());
                        this.setDirty(true);
                    }},
                    new Node() {{
                        this.setPartialKey(leafBLarge.getPartialKey());
                        this.setStorageValue(leafBLarge.getStorageValue());
                        this.setDirty(true);
                    }}
            }));
        }});

        Trie trie = TrieVerifier.buildTrie(encodedProofNodes, rootHash);

        assertEquals(expectedTrie.toString(), trie.toString());
    }

    public Node[] padRightChildren(Node[] children) {
        Node[] paddedSlice = new Node[Node.CHILDREN_CAPACITY];
        System.arraycopy(children, 0, paddedSlice, 0, Math.min(children.length, Node.CHILDREN_CAPACITY));
        return paddedSlice;
    }

    private void assertLongEncoding(Node node) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        TreeEncoder.encode(node, outputStream);

        assertTrue(outputStream.toByteArray().length > 32);
    }

    private void assertShortEncoding(Node node) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        TreeEncoder.encode(node, outputStream);

        assertTrue(outputStream.toByteArray().length < 32);
    }


    private byte[] getBadNodeEncoding() {
        return new byte[]{0x1, (byte) 0x94, (byte) 0xfd, (byte) 0xc2, (byte) 0xfa, 0x2f,
                (byte) 0xfc, (byte) 0xc0, 0x41, (byte) 0xd3, (byte) 0xff, 0x12, 0x4, 0x5b, 0x73, (byte) 0xc8, 0x6e,
                0x4f, (byte) 0xf9, 0x5f, (byte) 0xf6, 0x62, (byte) 0xa5, (byte) 0xee, (byte) 0xe8, 0x2a, (byte) 0xbd,
                (byte) 0xf4, 0x4a, 0x2d, 0xb, 0x75, (byte) 0xfb};
    }

}