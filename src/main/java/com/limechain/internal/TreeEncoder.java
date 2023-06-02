package com.limechain.internal;

import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.writer.UByteWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static com.limechain.internal.TrieVerifier.MAX_PARTIAL_KEY_LENGTH;

public class TreeEncoder {
    public static void encode(Node node, OutputStream buffer) throws Exception {
        encodeHeader(node, buffer);

        byte[] keyLE = Nibbles.nibblesToKeyLE(node.getPartialKey());
        buffer.write(keyLE);

        if (node.getKind() == NodeKind.Branch) {
            try (ScaleCodecWriter writer = new ScaleCodecWriter(buffer)) {
                writer.writeUint16(node.getChildrenBitmap());
            }
        }

        // Only encode node storage value if the node has a storage value,
        // even if it is empty. Do not encode if the branch is without value.
        // Note leaves and branches with value cannot have a `null` storage value.
        if (node.getStorageValue() != null) {
            try (ScaleCodecWriter writer = new ScaleCodecWriter(buffer)) {
                writer.writeAsList(node.getStorageValue());
            }
        }

        if (node.getKind() == NodeKind.Branch) {
            encodeChildren(node.getChildren(), buffer);
        }

    }

    public static void encodeHeader(Node node, OutputStream writer) throws Exception {
        int partialKeyLength = node.getPartialKey().length;
        if (partialKeyLength > MAX_PARTIAL_KEY_LENGTH) {
            throw new IllegalStateException("Partial key length is too big: " + partialKeyLength);
        }

        Variant variant;
        if (node.getKind() == NodeKind.Leaf) {
            variant = Variant.LEAF;
        } else if (node.getStorageValue() == null) {
            variant = Variant.BRANCH;
        } else {
            variant = Variant.BRANCH_WITH_VALUE;
        }

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        int headerByte = variant.bits;
        new UByteWriter().write(new ScaleCodecWriter(outStream), variant.bits);

        int partialKeyLengthMask = variant.mask;
        if (partialKeyLength < partialKeyLengthMask) {
            // Partial key length fits in header byte
            headerByte |= partialKeyLength;
            writer.write(headerByte);
            return;
        }


        // Partial key length does not fit in header byte only
        headerByte |= partialKeyLengthMask;
        partialKeyLength -= partialKeyLengthMask;
        writer.write(headerByte);

        while (true) {
            headerByte = 255;
            if (partialKeyLength < 255) {
                headerByte = partialKeyLength;
            }

            writer.write(headerByte);
            partialKeyLength -= headerByte;

            if (headerByte < 255) {
                break;
            }
        }
    }

    private static void encodeChildren(Node[] children, OutputStream buffer) throws Exception {
        for (Node child : children) {
            if (child == null) {
                continue;
            }

            encodeChild(child, buffer);
        }
    }

    private static void encodeChild(Node child, OutputStream buffer) throws IOException {
        byte[] merkleValue = child.calculateMerkleValue();
        try (ScaleCodecWriter writer = new ScaleCodecWriter(buffer)) {
            writer.writeAsList(merkleValue);
        }

    }
}
