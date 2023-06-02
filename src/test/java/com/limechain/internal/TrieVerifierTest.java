package com.limechain.internal;

import com.limechain.utils.HashUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrieVerifierTest {
    Node leafAShort = new Node() {{
        this.setPartialKey(new byte[]{1});
        this.setStorageValue(new byte[]{2});
    }};

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