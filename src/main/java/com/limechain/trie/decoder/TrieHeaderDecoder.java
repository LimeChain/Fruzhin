package com.limechain.trie.decoder;

import com.limechain.trie.NodeVariant;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.List;

import static com.limechain.trie.TrieVerifier.MAX_PARTIAL_KEY_LENGTH;

@UtilityClass
public class TrieHeaderDecoder {

    /**
     * Decodes a node header from a ScaleCodecReader input stream.
     * See <a href="https://spec.polkadot.network/chap-state#defn-node-header">here</a> for more information
     *
     * @param reader the ScaleCodecReader input stream used to read the encoded node data
     * @return The variant, partial key length and mask read from the stream
     * @throws TrieDecoderException if an error occurs while reading the node type or storage value.
     */
    public static TrieHeaderDecoderResult decodeHeader(ScaleCodecReader reader) {
        byte currentByte = reader.readByte();
        try {
            TrieHeaderDecoderResult header = TrieHeaderDecoder.decodeHeaderByte(currentByte);
            int partialKeyLengthHeader = header.partialKeyLengthHeader();
            int partialKeyLengthHeaderMask = header.nodeVariant().getPartialKeyLengthHeaderMask();

            // Empty node
            if (partialKeyLengthHeaderMask == NodeVariant.EMPTY.bits) {
                return new TrieHeaderDecoderResult(header.nodeVariant(), 0);
            }

            // Key length is contained in the first byte
            if (partialKeyLengthHeader < partialKeyLengthHeaderMask) {
                return new TrieHeaderDecoderResult(header.nodeVariant(), partialKeyLengthHeader);
            }

            // If the value is greater than the maximum possible value the bits can hold (63 for leafs),
            // then the value of the next 8 bits are added to the length.
            // This process is repeated until the next value less than < 255
            // See https://spec.polkadot.network/chap-state#defn-node-header
            while (true) {
                int nextByte = reader.readUByte();
                partialKeyLengthHeader += nextByte;
                if (partialKeyLengthHeader > MAX_PARTIAL_KEY_LENGTH) {
                    throw new TrieDecoderException("Partial key overflow");
                }

                // Check if current byte is less than max byte value
                if (nextByte < 255) {
                    return new TrieHeaderDecoderResult(header.nodeVariant(), partialKeyLengthHeader);
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
            int variantBits = headerByte & nodeVariant.mask;
            if (variantBits != nodeVariant.bits) {
                continue;
            }

            byte partialKeyLengthHeader = (byte) (headerByte & nodeVariant.getPartialKeyLengthHeaderMask());
            return new TrieHeaderDecoderResult(nodeVariant, partialKeyLengthHeader);
        }
        throw new TrieDecoderException("Node header is unknown: " +
                String.format("%08d", Integer.parseInt(Integer.toBinaryString(headerByte))));
    }
}