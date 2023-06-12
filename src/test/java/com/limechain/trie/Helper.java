package com.limechain.trie;

import com.limechain.trie.encoder.TrieEncoder;
import com.limechain.utils.RandomGenerationUtils;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class Helper {
    public static Node leafAShort = new Node() {{
        this.setPartialKey(new byte[]{1});
        this.setStorageValue(new byte[]{2});
        this.setDirty(true);
    }};

    public static Node leafBLarge = new Node() {{
        this.setPartialKey(new byte[]{2});
        this.setStorageValue(RandomGenerationUtils.generateBytes(40));
        this.setDirty(true);
    }};

    public static Node leafCLarge = new Node() {{
        this.setPartialKey(new byte[]{3});
        this.setStorageValue(RandomGenerationUtils.generateBytes(40));
        this.setDirty(true);
    }};

    public static Node[] padRightChildren(Node[] children) {
        Node[] paddedSlice = new Node[Node.CHILDREN_CAPACITY];
        System.arraycopy(children, 0, paddedSlice, 0, Math.min(children.length, Node.CHILDREN_CAPACITY));
        return paddedSlice;
    }

    public static void assertLongEncoding(Node node) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        TrieEncoder.encode(node, outputStream);

        assertTrue(outputStream.toByteArray().length > 32);
    }

    public static void assertShortEncoding(Node node) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        TrieEncoder.encode(node, outputStream);

        assertTrue(outputStream.toByteArray().length < 32);
    }

    public static byte[] getBadNodeEncoding() {
        return new byte[]{0x1, (byte) 0x94, (byte) 0xfd, (byte) 0xc2, (byte) 0xfa, 0x2f,
                (byte) 0xfc, (byte) 0xc0, 0x41, (byte) 0xd3, (byte) 0xff, 0x12, 0x4, 0x5b, 0x73, (byte) 0xc8, 0x6e,
                0x4f, (byte) 0xf9, 0x5f, (byte) 0xf6, 0x62, (byte) 0xa5, (byte) 0xee, (byte) 0xe8, 0x2a, (byte) 0xbd,
                (byte) 0xf4, 0x4a, 0x2d, 0xb, 0x75, (byte) 0xfb};
    }
}
