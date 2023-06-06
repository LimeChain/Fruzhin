package com.limechain.trie.decoder;

import com.limechain.trie.NodeVariant;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;

import java.util.Arrays;
import java.util.List;

import static com.limechain.trie.TrieVerifier.MAX_PARTIAL_KEY_LENGTH;

public class TrieHeaderDecoder {
    public static TrieHeaderDecoderResult decodeHeader(ScaleCodecReader reader) throws TrieDecoderException {
        try {
            byte currentByte = reader.readByte();
            TrieHeaderDecoderResult header = TrieHeaderDecoder.decodeHeaderByte(currentByte);
            int partialKeyLengthHeader = header.getPartialKeyLengthHeader();
            int partialKeyLengthHeaderMask = header.getPartialKeyLengthHeaderMask();
            byte variantBits = header.getVariantBits();

            if (partialKeyLengthHeader < partialKeyLengthHeaderMask) {
                return new TrieHeaderDecoderResult(variantBits, partialKeyLengthHeader, (byte) 0);
            }

            while (true) {
                int nextByte = reader.readUByte();
                partialKeyLengthHeader += nextByte;
                if (partialKeyLengthHeader > MAX_PARTIAL_KEY_LENGTH) {
                    throw new IllegalStateException("Partial key overflow");
                }

                // Check if current byte is max byte value
                if (nextByte < 255) {
                    return new TrieHeaderDecoderResult(variantBits, partialKeyLengthHeader, (byte) 0);
                }
            }
        } catch (IndexOutOfBoundsException e) {
            throw new TrieDecoderException("Could not decode header: " + e.getMessage());
        }
    }

    static TrieHeaderDecoderResult decodeHeaderByte(byte headerByte) throws TrieDecoderException {
        List<NodeVariant> nodeVariantList = Arrays.asList(NodeVariant.values());
        for (int i = nodeVariantList.size() - 1; i >= 0; i--) {
            NodeVariant nodeVariant = nodeVariantList.get(i);
            int variantBits = headerByte & nodeVariant.bits;
            if (variantBits != nodeVariant.bits) {
                continue;
            }

            byte partialKeyLengthHeader = (byte) (headerByte & nodeVariant.mask);
            return new TrieHeaderDecoderResult((byte) variantBits, partialKeyLengthHeader, (byte) nodeVariant.mask);
        }
        throw new TrieDecoderException("Node variant is unknown for header byte " +
                String.format("%08d", Integer.parseInt(Integer.toBinaryString(headerByte & 0xFF))));
    }
}