package com.limechain.internal;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import static com.limechain.internal.TrieVerifier.MAX_PARTIAL_KEY_LENGTH;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.*;

class TreeEncoderTest {
    @Test
    void testEncodeHeaderBranchWithNoKey() throws Exception {
        var node = new Node() {{
            this.setChildren(new Node[Node.CHILDREN_CAPACITY]);
        }};
        OutputStream buffer = mock(OutputStream.class);

        TreeEncoder.encodeHeader(node, buffer);
        verify(buffer, times(1)).write(Variant.BRANCH.bits);
    }

    @Test
    void testEncodeHeaderBranchWithValue() throws Exception {
        var node = new Node() {{
            this.setChildren(new Node[Node.CHILDREN_CAPACITY]);
            this.setStorageValue(new byte[0]);
        }};
        OutputStream buffer = mock(OutputStream.class);

        TreeEncoder.encodeHeader(node, buffer);
        verify(buffer, times(1)).write(Variant.BRANCH_WITH_VALUE.bits);
    }

    @Test
    void testEncodeHeaderBranchWithKeyOfLength30() throws Exception {
        var node = new Node() {{
            this.setChildren(new Node[Node.CHILDREN_CAPACITY]);
            this.setPartialKey(new byte[30]);
        }};
        OutputStream buffer = mock(OutputStream.class);

        TreeEncoder.encodeHeader(node, buffer);
        verify(buffer, times(1)).write(Variant.BRANCH.bits | 30);
    }

    @Test
    void testEncodeHeaderBranchWithKeyOfLength62() throws Exception {
        var node = new Node() {{
            this.setChildren(new Node[Node.CHILDREN_CAPACITY]);
            this.setPartialKey(new byte[62]);
        }};
        OutputStream buffer = mock(OutputStream.class);

        TreeEncoder.encodeHeader(node, buffer);
        verify(buffer, times(1)).write(Variant.BRANCH.bits | 62);
    }

    @Test
    void testEncodeHeaderBranchWithKeyOfLength63() throws Exception {
        var node = new Node() {{
            this.setChildren(new Node[Node.CHILDREN_CAPACITY]);
            this.setPartialKey(new byte[63]);
        }};
        OutputStream buffer = mock(OutputStream.class);

        TreeEncoder.encodeHeader(node, buffer);
        verify(buffer, times(1)).write(Variant.BRANCH.bits | 63);
    }

    @Test
    void testEncodeHeaderBranchWithKeyOfLength64() throws Exception {
        var node = new Node() {{
            this.setChildren(new Node[Node.CHILDREN_CAPACITY]);
            this.setPartialKey(new byte[64]);
        }};
        OutputStream buffer = mock(OutputStream.class);

        TreeEncoder.encodeHeader(node, buffer);
        verify(buffer, times(1)).write(Variant.BRANCH.bits | 63);
        verify(buffer, times(1)).write(0x01);
    }

    @Test
    void testEncodeHeaderLeafWithNoKey() throws Exception {
        var node = new Node() {{
            this.setStorageValue(new byte[]{1});
        }};
        OutputStream buffer = mock(OutputStream.class);

        TreeEncoder.encodeHeader(node, buffer);

        verify(buffer, times(1)).write(Variant.LEAF.bits);
    }

    @Test
    void testEncodeHeaderLeafWithKeyOfLength30() throws Exception {
        var node = new Node() {{
            this.setPartialKey(new byte[30]);
        }};
        OutputStream buffer = mock(OutputStream.class);

        TreeEncoder.encodeHeader(node, buffer);

        verify(buffer, times(1)).write(Variant.LEAF.bits | 30);
    }

    @Test
    void testEncodeHeaderLeafWithKeyOfLength62() throws Exception {
        var node = new Node() {{
            this.setPartialKey(new byte[62]);
        }};
        OutputStream buffer = mock(OutputStream.class);

        TreeEncoder.encodeHeader(node, buffer);

        verify(buffer, times(1)).write(Variant.LEAF.bits | 62);
    }

    @Test
    void testEncodeHeaderLeafWithKeyOfLength63() throws Exception {
        var node = new Node() {{
            this.setPartialKey(new byte[63]);
        }};
        OutputStream buffer = mock(OutputStream.class);

        TreeEncoder.encodeHeader(node, buffer);

        verify(buffer, times(1)).write(Variant.LEAF.bits | 63);
    }

    @Test
    void testEncodeHeaderLeafWithKeyOfLength64() throws Exception {
        var node = new Node() {{
            this.setPartialKey(new byte[64]);
        }};
        OutputStream buffer = mock(OutputStream.class);

        TreeEncoder.encodeHeader(node, buffer);

        verify(buffer, times(1)).write(Variant.LEAF.bits | 63);
        verify(buffer, times(1)).write(0x01);
    }

    @Test
    void testEncodeHeaderLeafWithKeyLengthOver3Bytes() throws Exception {
        var node = new Node() {{
            this.setPartialKey(new byte[Variant.LEAF.mask + 0b1111_1111 + 0b0000_0001]);
        }};
        OutputStream buffer = mock(OutputStream.class);

        TreeEncoder.encodeHeader(node, buffer);

        verify(buffer, times(1)).write(Variant.LEAF.bits ^ Variant.LEAF.mask);
        verify(buffer, times(1)).write(0b1111_1111);
        verify(buffer, times(1)).write(1);
    }

    @Test
    void testEncodeHeaderLeafWithKeyLengthOver3BytesAndLastByte0() throws Exception {
        var node = new Node() {{
            this.setPartialKey(new byte[Variant.LEAF.mask + 0b1111_1111]);
        }};
        OutputStream buffer = mock(OutputStream.class);

        TreeEncoder.encodeHeader(node, buffer);

        verify(buffer, times(1)).write(Variant.LEAF.bits ^ Variant.LEAF.mask);
        verify(buffer, times(1)).write(0b1111_1111);
        verify(buffer, times(1)).write(0);
    }

    @Test
    public void testEncodeHeaderAtMaximum() throws Exception {
        int variant = Variant.LEAF.bits;
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

        TreeEncoder.encodeHeader(node, buffer);

        assertArrayEquals(expectedBytes, buffer.toByteArray());
    }

    @Test
    public void testEncodeChildrenNode() throws Exception {
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
//        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        TreeEncoder.encode(node, buffer);

        // Header
        verify(buffer, times(1)).write(Variant.BRANCH_WITH_VALUE.bits | 3);
        // LE key
        verify(buffer, times(1)).write(new byte[]{0x01, 0x23});
        // Children bitmap
        verify(buffer, times(1)).write(136);
        verify(buffer, atLeast(1)).write(0);
        // Storage value
        verify(buffer, times(1)).write(4);
        // Storage value - scale specifc invocation
        verify(buffer, times(1)).write(new byte[]{100}, 0, 1);
        // First child
        verify(buffer, times(1)).write(new byte[]{16, 95, 9, 4, 1});
        verify(buffer, times(1)).write(new byte[]{16, 65, 11, 4, 1});
    }


    // Should be in a separate test class
    @Test
    public void testEncodeChildrenNoChildren() throws Exception {
        ByteArrayOutputStream buffer = mock(ByteArrayOutputStream.class);

        TreeEncoder.encodeChildren(new Node[]{}, buffer);

        verify(buffer, times(0)).write(any());
    }

    @Test
    public void testEncodeChildrenFirstChildNotNull() throws Exception {
        ByteArrayOutputStream buffer = mock(ByteArrayOutputStream.class);

        TreeEncoder.encodeChildren(new Node[]{
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
    public void testEncodeChildrenLastChildNotNull() throws Exception {
        ByteArrayOutputStream buffer = mock(ByteArrayOutputStream.class);

        TreeEncoder.encodeChildren(new Node[]{
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
    public void testEncodeChildrenFirstTwoChildrenNotNull() throws Exception {
        ByteArrayOutputStream buffer = mock(ByteArrayOutputStream.class);

        TreeEncoder.encodeChildren(new Node[]{
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
    public void testEncodeEmptyBranchChild() throws Exception {
        ByteArrayOutputStream buffer = mock(ByteArrayOutputStream.class);

        TreeEncoder.encodeChild(new Node() {{
            this.setChildren(new Node[Node.CHILDREN_CAPACITY]);
        }}, buffer);

        verify(buffer, times(1)).write(16);
        // SCALE specific invocation... Maybe there's a workaround for this
        verify(buffer, times(1)).write(new byte[]{65, 1, 4, 2}, 0, 4);
    }

    @Test
    public void testEncodeLeafChild() throws Exception {
        ByteArrayOutputStream buffer = mock(ByteArrayOutputStream.class);

        TreeEncoder.encodeChild(new Node() {{
            this.setPartialKey(new byte[]{1});
            this.setStorageValue(new byte[]{2});
        }}, buffer);

        verify(buffer, times(1)).write(16);
        // SCALE specific invocation... Maybe there's a workaround for this
        verify(buffer, times(1)).write(new byte[]{65, 1, 4, 2}, 0, 4);
    }

    @Test
    public void testEncodeBranchChild() throws Exception {
        ByteArrayOutputStream buffer = mock(ByteArrayOutputStream.class);

        TreeEncoder.encodeChild(new Node() {{
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
        verify(buffer, times(1)).write(new byte[]{(byte) 193, 1, 4, 0, 4, 2, 16, 65, 5, 4, 6}, 0, 11);
    }
}