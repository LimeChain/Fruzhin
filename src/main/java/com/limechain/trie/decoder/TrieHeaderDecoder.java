package com.limechain.trie.decoder;

import com.limechain.trie.NodeVariant;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;

import java.util.Arrays;
import java.util.List;

import static com.limechain.trie.TrieVerifier.MAX_PARTIAL_KEY_LENGTH;

public class TrieHeaderDecoder {

    /**
     * Decodes a node header from a ScaleCodecReader input stream.
     *
     * @param reader the ScaleCodecReader input stream used to read the encoded node data
     * @return The variant, partial key length and mask read from the stream
     * @throws TrieDecoderException if an error occurs while reading the children bitmap or the storage value.
     */
    public static TrieHeaderDecoderResult decodeHeader(ScaleCodecReader reader) {
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
                    throw new TrieDecoderException("Partial key overflow");
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

    /**
     * Decodes the header byte of a Trie node to identify its variant and the partial key length.
     *
     * @param headerByte the byte from the header of the Trie node to be decoded
     * @return a TrieHeaderDecoderResult object containing the variant bits, partial key length, and the variant mask
     * @throws TrieDecoderException if no matching NodeVariant is found for the given header byte
     */
    static TrieHeaderDecoderResult decodeHeaderByte(byte headerByte) {
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