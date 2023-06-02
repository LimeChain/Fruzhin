package com.limechain.internal;

import org.junit.jupiter.api.Test;

import java.io.OutputStream;

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

}