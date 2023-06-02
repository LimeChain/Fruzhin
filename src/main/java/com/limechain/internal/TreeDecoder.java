package com.limechain.internal;

import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.extern.java.Log;

import static com.limechain.internal.TrieVerifier.MAX_PARTIAL_KEY_LENGTH;

@Log
public class TreeDecoder {

    public static DecodeHeaderResult decodeHeader(ScaleCodecReader reader) throws TrieDecoderException {
        try {
            byte currentByte = reader.readByte();
            DecodeHeaderResult header = DecodeHeaderResult.decodeHeaderByte(currentByte);
            int partialKeyLengthHeader = header.getPartialKeyLengthHeader();
            int partialKeyLengthHeaderMask = header.getPartialKeyLengthHeaderMask();

            if (partialKeyLengthHeader < partialKeyLengthHeaderMask) {
                return new DecodeHeaderResult(header.getVariantBits(), header.getPartialKeyLengthHeader(), (byte) 0);
            }

            while (true) {
                int nextByte = reader.readUByte();
                partialKeyLengthHeader += nextByte;
                if (partialKeyLengthHeader > MAX_PARTIAL_KEY_LENGTH) {
                    throw new IllegalStateException("Partial key overflow");
                }

                //check if current byte is max byte value
                if (nextByte < 255) {
                    return new DecodeHeaderResult(header.getVariantBits(), partialKeyLengthHeader, (byte) 0);
                }
            }
        } catch (IndexOutOfBoundsException e) {
            throw new TrieDecoderException("Could not decode header: " + e.getMessage());
        }
    }

    public static Node decodeLeaf(ScaleCodecReader reader, int partialKeyLength) throws TrieDecoderException {
        Node node = new Node();
        node.setPartialKey(DecodeLeaf.decodeKey(reader, partialKeyLength));

        // Decode storage:
        // https://spec.polkadot.network/sect-metadata#defn-rtm-storage-entry-type
        try {
            node.setStorageValue(reader.readByteArray());
        } catch (IndexOutOfBoundsException e) {
            throw new TrieDecoderException("Could not decode storage value: " + e.getMessage());
        }
        return node;
    }

    public static Node decodeBranch(ScaleCodecReader reader, byte variantByte, int partialKeyLength) throws TrieDecoderException {
        Node node = new Node();
        node.setChildren(new Node[Node.CHILDREN_CAPACITY]);

        node.setPartialKey(DecodeLeaf.decodeKey(reader, partialKeyLength));

        byte[] childrenBitmap;

        try {
            childrenBitmap = reader.readByteArray(2);
        } catch (IndexOutOfBoundsException error) {
            throw new TrieDecoderException("Could not decode children bitmap: " + error.getMessage());
        }
        int variant = variantByte & 0xff;
        if (variant == DecodeHeaderResult.variantsOrderedByBitMask[2][1]) {
            try {
                node.setStorageValue(reader.readByteArray());
            } catch (IndexOutOfBoundsException e) {
                throw new TrieDecoderException("Could not decode storage value: " + e.getMessage());
            }
        }

        for (int i = 0; i < Node.CHILDREN_CAPACITY; i++) {
            if (((childrenBitmap[i / 8] >> (i % 8)) & 1) != 1) {
                continue;
            }
            byte[] hash;
            try {
                hash = reader.readByteArray();
            } catch (IndexOutOfBoundsException e) {
                throw new TrieDecoderException("Could not decode child hash: " + e.getMessage());
            }
            Node child = new Node();
            child.setMerkleValue(Node.getMerkleValueRoot(hash));
            if (hash.length < Hash256.SIZE_BYTES) {
                ScaleCodecReader inlinedChildReader = new ScaleCodecReader(hash);
                Node childNode = decode(inlinedChildReader);
                node.setDescendants(node.getDescendants() + childNode.getDescendants());
            }
            node.setDescendants(node.getDescendants() + 1);
            node.setChildrenAt(child, i);
        }
        return node;
    }

    public static Node decode(ScaleCodecReader reader) throws TrieDecoderException {
        DecodeHeaderResult decodeHeaderResult = decodeHeader(reader);
        int variant = decodeHeaderResult.getVariantBits() & 0xff;
        int partialKeyLength = decodeHeaderResult.partialKeyLengthHeader;
        if (variant == DecodeHeaderResult.variantsOrderedByBitMask[0][0]) {
            return decodeLeaf(reader, partialKeyLength);
        }
        if (variant == DecodeHeaderResult.variantsOrderedByBitMask[1][0] ||
                variant == DecodeHeaderResult.variantsOrderedByBitMask[2][0]) {
            return decodeBranch(reader, (byte) variant, partialKeyLength);
        }

        throw new IllegalStateException("Unknown variant: " + variant);
    }
}
