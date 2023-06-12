package com.limechain.trie.encoder;

import com.limechain.trie.Node;
import com.limechain.trie.NodeVariant;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static com.limechain.trie.TrieVerifier.MAX_PARTIAL_KEY_LENGTH;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class TrieEncoderTest {
    @Test
    void testEncodeHeaderBranchWithNoKey() throws TrieEncoderException, IOException {
        var node = new Node() {{
            this.setChildren(new Node[Node.CHILDREN_CAPACITY]);
        }};
        OutputStream buffer = mock(OutputStream.class);

        TrieEncoder.encodeHeader(node, buffer);
        verify(buffer, times(1)).write(NodeVariant.BRANCH.bits);
    }

    @Test
    void testEncodeHeaderBranchWithValue() throws TrieEncoderException, IOException {
        var node = new Node() {{
            this.setChildren(new Node[Node.CHILDREN_CAPACITY]);
            this.setStorageValue(new byte[0]);
        }};
        OutputStream buffer = mock(OutputStream.class);

        TrieEncoder.encodeHeader(node, buffer);
        verify(buffer, times(1)).write(NodeVariant.BRANCH_WITH_VALUE.bits);
    }

    @Test
    void testEncodeHeaderBranchWithKeyOfLength30() throws TrieEncoderException, IOException {
        var node = new Node() {{
            this.setChildren(new Node[Node.CHILDREN_CAPACITY]);
            this.setPartialKey(new byte[30]);
        }};
        OutputStream buffer = mock(OutputStream.class);

        TrieEncoder.encodeHeader(node, buffer);
        verify(buffer, times(1)).write(NodeVariant.BRANCH.bits | 30);
    }

    @Test
    void testEncodeHeaderBranchWithKeyOfLength62() throws TrieEncoderException, IOException {
        var node = new Node() {{
            this.setChildren(new Node[Node.CHILDREN_CAPACITY]);
            this.setPartialKey(new byte[62]);
        }};
        OutputStream buffer = mock(OutputStream.class);

        TrieEncoder.encodeHeader(node, buffer);
        verify(buffer, times(1)).write(NodeVariant.BRANCH.bits | 62);
    }

    @Test
    void testEncodeHeaderBranchWithKeyOfLength63() throws TrieEncoderException, IOException {
        var node = new Node() {{
            this.setChildren(new Node[Node.CHILDREN_CAPACITY]);
            this.setPartialKey(new byte[63]);
        }};
        OutputStream buffer = mock(OutputStream.class);

        TrieEncoder.encodeHeader(node, buffer);
        verify(buffer, times(1)).write(NodeVariant.BRANCH.bits | 63);
    }

    @Test
    void testEncodeHeaderBranchWithKeyOfLength64() throws TrieEncoderException, IOException {
        var node = new Node() {{
            this.setChildren(new Node[Node.CHILDREN_CAPACITY]);
            this.setPartialKey(new byte[64]);
        }};
        OutputStream buffer = mock(OutputStream.class);

        TrieEncoder.encodeHeader(node, buffer);
        verify(buffer, times(1)).write(NodeVariant.BRANCH.bits | 63);
        verify(buffer, times(1)).write(0x01);
    }

    @Test
    void testEncodeHeaderLeafWithNoKey() throws TrieEncoderException, IOException {
        var node = new Node() {{
            this.setStorageValue(new byte[]{1});
        }};
        OutputStream buffer = mock(OutputStream.class);

        TrieEncoder.encodeHeader(node, buffer);

        verify(buffer, times(1)).write(NodeVariant.LEAF.bits);
    }

    @Test
    void testEncodeHeaderLeafWithKeyOfLength30() throws TrieEncoderException, IOException {
        var node = new Node() {{
            this.setPartialKey(new byte[30]);
        }};
        OutputStream buffer = mock(OutputStream.class);

        TrieEncoder.encodeHeader(node, buffer);

        verify(buffer, times(1)).write(NodeVariant.LEAF.bits | 30);
    }

    @Test
    void testEncodeHeaderLeafWithKeyOfLength62() throws TrieEncoderException, IOException {
        var node = new Node() {{
            this.setPartialKey(new byte[62]);
        }};
        OutputStream buffer = mock(OutputStream.class);

        TrieEncoder.encodeHeader(node, buffer);

        verify(buffer, times(1)).write(NodeVariant.LEAF.bits | 62);
    }

    @Test
    void testEncodeHeaderLeafWithKeyOfLength63() throws TrieEncoderException, IOException {
        var node = new Node() {{
            this.setPartialKey(new byte[63]);
        }};
        OutputStream buffer = mock(OutputStream.class);

        TrieEncoder.encodeHeader(node, buffer);

        verify(buffer, times(1)).write(NodeVariant.LEAF.bits | 63);
    }

    @Test
    void testEncodeHeaderLeafWithKeyOfLength64() throws TrieEncoderException, IOException {
        var node = new Node() {{
            this.setPartialKey(new byte[64]);
        }};
        OutputStream buffer = mock(OutputStream.class);

        TrieEncoder.encodeHeader(node, buffer);

        verify(buffer, times(1)).write(NodeVariant.LEAF.bits | 63);
        verify(buffer, times(1)).write(0x01);
    }

    @Test
    void testEncodeHeaderLeafWithKeyLengthOver3Bytes() throws TrieEncoderException, IOException {
        var node = new Node() {{
            this.setPartialKey(new byte[NodeVariant.LEAF.mask + 0b1111_1111 + 0b0000_0001]);
        }};
        OutputStream buffer = mock(OutputStream.class);

        TrieEncoder.encodeHeader(node, buffer);

        verify(buffer, times(1)).write(NodeVariant.LEAF.bits ^ NodeVariant.LEAF.mask);
        verify(buffer, times(1)).write(0b1111_1111);
        verify(buffer, times(1)).write(1);
    }

    @Test
    void testEncodeHeaderLeafWithKeyLengthOver3BytesAndLastByte0() throws TrieEncoderException, IOException {
        var node = new Node() {{
            this.setPartialKey(new byte[NodeVariant.LEAF.mask + 0b1111_1111]);
        }};
        OutputStream buffer = mock(OutputStream.class);

        TrieEncoder.encodeHeader(node, buffer);

        verify(buffer, times(1)).write(NodeVariant.LEAF.bits ^ NodeVariant.LEAF.mask);
        verify(buffer, times(1)).write(0b1111_1111);
        verify(buffer, times(1)).write(0);
    }

    @Test
    public void testEncodeHeaderAtMaximum() throws TrieEncoderException {
        int variant = NodeVariant.LEAF.bits;
        final int partialKeyLengthHeaderMask = 0b0011_1111;
        double extraKeyBytesNeeded = Math.ceil((double) (MAX_PARTIAL_KEY_LENGTH - partialKeyLengthHeaderMask) / 255.0);
        int expectedEncodingLength = 1 + (int) extraKeyBytesNeeded;

        int lengthLeft = MAX_PARTIAL_KEY_LENGTH;
        byte[] expectedBytes = new byte[expectedEncodingLength];
        expectedBytes[0] = (byte) (variant | partialKeyLengthHeaderMask);
        lengthLeft -= partialKeyLengthHeaderMask;
        for (int i = 1; i < expectedBytes.length - 1; i++) {
            expectedBytes[i] = (byte) 255;
            lengthLeft -= 255;
        }
        expectedBytes[expectedBytes.length - 1] = (byte) lengthLeft;

        ByteArrayOutputStream buffer = new ByteArrayOutputStream(expectedEncodingLength);
        buffer.reset();

        Node node = new Node();
        node.setPartialKey(new byte[MAX_PARTIAL_KEY_LENGTH]);

        TrieEncoder.encodeHeader(node, buffer);

        assertArrayEquals(expectedBytes, buffer.toByteArray());
    }

    @Test
    public void testEncodeChildrenNode() throws IOException, TrieEncoderException {
        Node node = new Node() {{
            this.setPartialKey(new byte[]{1, 2, 3});
            this.setStorageValue(new byte[]{100});
            this.setChildren(new Node[]{
                            null, null, null,
                            new Node() {{
                                this.setPartialKey(new byte[]{9});
                                this.setStorageValue(new byte[]{1});
                            }},
                            null, null, null,
                            new Node() {{
                                this.setPartialKey(new byte[]{11});
                                this.setStorageValue(new byte[]{1});
                            }},
                    }

            );
        }};

        ByteArrayOutputStream buffer = mock(ByteArrayOutputStream.class);
        TrieEncoder.encode(node, buffer);

        // Header
        verify(buffer, times(1)).write(NodeVariant.BRANCH_WITH_VALUE.bits | 3);
        // LE key
        verify(buffer, times(1)).write(new byte[]{0x01, 0x23});
        // Children bitmap
        verify(buffer, times(1)).write(136);
        verify(buffer, atLeast(1)).write(0);
    }

    // Should be in a separate test class
    @Test
    public void testEncodeChildrenNoChildren() throws TrieEncoderException, IOException {
        ByteArrayOutputStream buffer = mock(ByteArrayOutputStream.class);

        TrieEncoder.encodeChildren(new Node[]{}, buffer);

        verify(buffer, times(0)).write(any());
    }

    @Test
    public void testEncodeChildrenFirstChildNotNull() throws TrieEncoderException {
        ByteArrayOutputStream buffer = mock(ByteArrayOutputStream.class);

        TrieEncoder.encodeChildren(new Node[]{
                new Node() {{
                    this.setPartialKey(new byte[]{1});
                    this.setStorageValue(new byte[]{2});
                }},
        }, buffer);

        verify(buffer, times(1)).write(16);
        // SCALE specific invocation... Maybe there's a workaround for this
        verify(buffer, times(1)).write(new byte[]{65, 1, 4, 2}, 0, 4);
    }

    @Test
    public void testEncodeChildrenLastChildNotNull() throws TrieEncoderException {
        ByteArrayOutputStream buffer = mock(ByteArrayOutputStream.class);

        TrieEncoder.encodeChildren(new Node[]{
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null,
                new Node() {{
                    this.setPartialKey(new byte[]{1});
                    this.setStorageValue(new byte[]{2});
                }},
        }, buffer);

        verify(buffer, times(1)).write(16);
        // SCALE specific invocation... Maybe there's a workaround for this
        verify(buffer, times(1)).write(new byte[]{65, 1, 4, 2}, 0, 4);
    }

    @Test
    public void testEncodeChildrenFirstTwoChildrenNotNull() throws TrieEncoderException {
        ByteArrayOutputStream buffer = mock(ByteArrayOutputStream.class);

        TrieEncoder.encodeChildren(new Node[]{
                new Node() {{
                    this.setPartialKey(new byte[]{1});
                    this.setStorageValue(new byte[]{2});
                }},
                new Node() {{
                    this.setPartialKey(new byte[]{3});
                    this.setStorageValue(new byte[]{4});
                }},
        }, buffer);

        verify(buffer, times(2)).write(16);
        // SCALE specific invocation... Maybe there's a workaround for this
        verify(buffer, times(1)).write(new byte[]{65, 1, 4, 2}, 0, 4);
        // SCALE specific invocation... Maybe there's a workaround for this
        verify(buffer, times(1)).write(new byte[]{65, 3, 4, 4}, 0, 4);
    }

    @Test
    public void testEncodeEmptyBranchChild() throws TrieEncoderException {
        ByteArrayOutputStream buffer = mock(ByteArrayOutputStream.class);

        TrieEncoder.encodeChild(new Node() {{
            this.setChildren(new Node[Node.CHILDREN_CAPACITY]);
        }}, buffer);

        verify(buffer, times(1)).write(12);
        // SCALE specific invocation... Maybe there's a workaround for this
        verify(buffer, times(1)).write(new byte[]{(byte) 128, 0, 0}, 0, 3);
    }

    @Test
    public void testEncodeLeafChild() throws TrieEncoderException {
        ByteArrayOutputStream buffer = mock(ByteArrayOutputStream.class);

        TrieEncoder.encodeChild(new Node() {{
            this.setPartialKey(new byte[]{1});
            this.setStorageValue(new byte[]{2});
        }}, buffer);

        verify(buffer, times(1)).write(16);
        // SCALE specific invocation... Maybe there's a workaround for this
        verify(buffer, times(1)).write(new byte[]{65, 1, 4, 2}, 0, 4);
    }

    @Test
    public void testEncodeBranchChild() throws TrieEncoderException {
        ByteArrayOutputStream buffer = mock(ByteArrayOutputStream.class);

        TrieEncoder.encodeChild(new Node() {{
            this.setPartialKey(new byte[]{1});
            this.setStorageValue(new byte[]{2});
            this.setChildren(new Node[]{
                    null, null,
                    new Node() {{
                        this.setPartialKey(new byte[]{5});
                        this.setStorageValue(new byte[]{6});
                    }},
            });
        }}, buffer);

        verify(buffer, times(1)).write(44);
        // SCALE specific invocation... Maybe there's a workaround for this
        verify(buffer, times(1)).write(
                new byte[]{(byte) 193, 1, 4, 0, 4, 2, 16, 65, 5, 4, 6}, 0, 11);
    }
}