package com.limechain.internal.trie;

import com.limechain.internal.Node;
import com.limechain.internal.Trie;
import com.limechain.internal.TrieEncoder;
import com.limechain.utils.HashUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static com.limechain.internal.trie.Helper.assertLongEncoding;
import static com.limechain.internal.trie.Helper.assertShortEncoding;
import static com.limechain.internal.trie.Helper.getBadNodeEncoding;
import static com.limechain.internal.trie.Helper.leafAShort;
import static com.limechain.internal.trie.Helper.leafBLarge;
import static com.limechain.internal.trie.Helper.leafCLarge;
import static com.limechain.internal.trie.Helper.padRightChildren;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TrieVerifierTest {
    @Test
    void buildTrieThrowsEmptyProofError() {
        assertThrows(IllegalStateException.class, () -> TrieVerifier.buildTrie(new byte[][]{}, new byte[]{1}));
    }

    @Test
    void buildTrieThrowsHeaderByteDecodingError() {
        var encodedProofNodes = new byte[][]{getBadNodeEncoding()};
        var rootHash = HashUtils.hashWithBlake2b(getBadNodeEncoding());
        assertThrows(IllegalStateException.class, () -> TrieVerifier.buildTrie(encodedProofNodes, rootHash));
    }

    @Test
    void buildTrieThrowsRootProofEncodingLessThan32Bytes() throws Exception {
        assertShortEncoding(leafAShort);

        var encodedProofNodes = new byte[][]{getBadNodeEncoding()};
        var rootHash = HashUtils.hashWithBlake2b(getBadNodeEncoding());
        assertThrows(IllegalStateException.class, () -> TrieVerifier.buildTrie(encodedProofNodes, rootHash));
    }

    @Test
    void buildTrieRootProofEncodingLessThan32Bytes() throws Exception {
        assertShortEncoding(leafAShort);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        TrieEncoder.encode(leafAShort, outputStream);

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
        TrieEncoder.encode(leafBLarge, outputStream);

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
        TrieEncoder.encode(leafAShort, leafAEncodedStream);
        ByteArrayOutputStream leafBEncodedStream = new ByteArrayOutputStream();
        TrieEncoder.encode(leafBLarge, leafBEncodedStream);

        byte[] rootHash = HashUtils.hashWithBlake2b(leafAEncodedStream.toByteArray());
        byte[][] encodedProofNodes = new byte[][]{leafAEncodedStream.toByteArray(), leafBEncodedStream.toByteArray()};
        Trie expectedTrie = Trie.newTrie(leafAShort);
        Trie trie = TrieVerifier.buildTrie(encodedProofNodes, rootHash);

        assertEquals(expectedTrie.toString(), trie.toString());
    }

    @Test
    void buildTrieMultipleUnorderedNodes() throws Exception {
        ByteArrayOutputStream leafBEncodedStream = new ByteArrayOutputStream();
        TrieEncoder.encode(leafBLarge, leafBEncodedStream);

        assertShortEncoding(leafAShort);
        assertLongEncoding(leafBLarge);
        assertLongEncoding(leafCLarge);

        ByteArrayOutputStream leafAEncodedStream = new ByteArrayOutputStream();
        TrieEncoder.encode(leafAShort, leafAEncodedStream);
        ByteArrayOutputStream leafCEncodedStream = new ByteArrayOutputStream();
        TrieEncoder.encode(leafCLarge, leafCEncodedStream);
        ByteArrayOutputStream rootNodeEncodedStream = new ByteArrayOutputStream();
        TrieEncoder.encode(new Node() {{
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

}