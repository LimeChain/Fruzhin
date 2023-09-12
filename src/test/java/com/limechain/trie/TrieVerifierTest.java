package com.limechain.trie;

import com.limechain.trie.decoder.TrieDecoderException;
import com.limechain.trie.encoder.TrieEncoder;
import com.limechain.utils.HashUtils;
import com.limechain.utils.RandomGenerationUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static com.limechain.trie.Helper.assertLongEncoding;
import static com.limechain.trie.Helper.assertShortEncoding;
import static com.limechain.trie.Helper.getBadNodeEncoding;
import static com.limechain.trie.Helper.leafAShort;
import static com.limechain.trie.Helper.leafBLarge;
import static com.limechain.trie.Helper.leafCLarge;
import static com.limechain.trie.Helper.padRightChildren;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrieVerifierTest {
    @Test
    void buildTrieThrowsEmptyProofError() {
        Exception e = assertThrows(IllegalArgumentException.class, () ->
                TrieVerifier.buildTrie(new byte[][]{}, new byte[]{1}));
        assertTrue(e.getMessage().contains("Encoded proof nodes is empty"));
    }

    @Test
    void buildTrieHeaderByteDecodingExceptionTest() {
        var encodedProofNodes = new byte[][]{getBadNodeEncoding()};
        var rootHash = HashUtils.hashWithBlake2b(getBadNodeEncoding());
        Exception e = assertThrows(TrieDecoderException.class, () ->
                TrieVerifier.buildTrie(encodedProofNodes, rootHash));
        assertTrue(e.getMessage().contains("Unknown variant: COMPACT_ENCODING"));
    }

    @Test
    void buildTrieRootProofEncodingLessThan32BytesExceptionTest() {
        assertShortEncoding(leafAShort);

        var encodedProofNodes = new byte[][]{getBadNodeEncoding()};
        var rootHash = HashUtils.hashWithBlake2b(getBadNodeEncoding());
        Exception e = assertThrows(TrieDecoderException.class, () ->
                TrieVerifier.buildTrie(encodedProofNodes, rootHash));
        assertTrue(e.getMessage().contains("Unknown variant: COMPACT_ENCODING"));
    }

    @Test
    void buildTrieRootProofEncodingLessThan32Bytes() {
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
    void buildTrieRootProofEncodingMoreThan32Bytes() {
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
    void buildTrieDiscardUnusedNode() {
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
    void buildTrieMultipleUnorderedNodes() {
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

    @Test
    void buildingProofTrieExceptionTest() {
        byte[] keyLE = new byte[]{1, 1};
        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            byte[] rootHash = new byte[]{1, 2, 3};
            TrieVerifier.verify(new byte[][]{}, rootHash, keyLE, new byte[]{});
        });
        assertTrue(e.getMessage().contains("Encoded proof nodes is empty"));
    }

    @Test
    void valueNotFoundExceptionTest() {
        ByteArrayOutputStream branchBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream leafBuffer = new ByteArrayOutputStream();
        TrieEncoder.encode(Helper.branch, branchBuffer);
        TrieEncoder.encode(leafAShort, leafBuffer);
        byte[] value = new byte[]{1, 1};
        byte[] rootHash = HashUtils.hashWithBlake2b(branchBuffer.toByteArray());
        byte[] keyLE = new byte[]{1, 1};

        byte[][] encodedNodes = new byte[][]{branchBuffer.toByteArray(), leafBuffer.toByteArray()};
        Exception e = assertThrows(Exception.class, () ->
                TrieVerifier.verify(encodedNodes,
                        rootHash,
                        keyLE,
                        value)
        );
        assertTrue(e.getMessage().contains("Key not found in proof trie hash"));
    }

    @Test
    void keyFoundWithNullSearchValueTest() {
        ByteArrayOutputStream branchBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream leafBuffer = new ByteArrayOutputStream();
        TrieEncoder.encode(Helper.branch, branchBuffer);
        TrieEncoder.encode(leafAShort, leafBuffer);
        byte[][] encodedNodes = new byte[][]{branchBuffer.toByteArray(), leafBuffer.toByteArray()};
        byte[] rootHash = HashUtils.hashWithBlake2b(branchBuffer.toByteArray());
        byte[] value = new byte[]{};
        byte[] keyLE = new byte[]{0x34, 0x21};
        boolean result = TrieVerifier.verify(encodedNodes, rootHash, keyLE, value);
        assertTrue(result);
    }

    @Test
    void keyFoundWithMismatchValueExceptionTest() {
        ByteArrayOutputStream branchBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream leafBuffer = new ByteArrayOutputStream();
        TrieEncoder.encode(Helper.branch, branchBuffer);
        TrieEncoder.encode(leafBLarge, leafBuffer);
        byte[][] encodedNodes = new byte[][]{branchBuffer.toByteArray(), leafBuffer.toByteArray()};
        byte[] rootHash = HashUtils.hashWithBlake2b(branchBuffer.toByteArray());
        byte[] value = new byte[]{2};
        byte[] keyLE = new byte[]{0x34, 0x21};
        Exception e = assertThrows(IllegalStateException.class, () ->
                TrieVerifier.verify(encodedNodes, rootHash, keyLE, value));
        assertTrue(e.getMessage().contains("Value mismatch"));
    }

    @Test
    void keyFoundWithMatchingValue() {
        ByteArrayOutputStream branchBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream leafBuffer = new ByteArrayOutputStream();
        TrieEncoder.encode(Helper.branch, branchBuffer);
        TrieEncoder.encode(leafBLarge, leafBuffer);
        byte[][] encodedNodes = new byte[][]{branchBuffer.toByteArray(), leafBuffer.toByteArray()};
        byte[] rootHash = HashUtils.hashWithBlake2b(branchBuffer.toByteArray());
        byte[] value = RandomGenerationUtils.generateBytes(40);
        byte[] keyLE = new byte[]{0x34, 0x32};
        boolean verified = TrieVerifier.verify(encodedNodes, rootHash, keyLE, value);
        assertTrue(verified);
    }
}