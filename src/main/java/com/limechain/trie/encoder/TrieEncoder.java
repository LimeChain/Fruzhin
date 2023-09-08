package com.limechain.trie.encoder;

import com.limechain.trie.Nibbles;
import com.limechain.trie.Node;
import com.limechain.trie.NodeKind;
import com.limechain.trie.NodeVariant;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.OutputStream;

import static com.limechain.trie.TrieVerifier.MAX_PARTIAL_KEY_LENGTH;

/**
 * Encodes a {@link Node} to a {@link OutputStream} buffer.
 */
@UtilityClass
public class TrieEncoder {

    /**
     * Encodes a node and it's children recursively. Writes the encoded node to the buffer.
     *
     * @param node   The node to encode
     * @param buffer The buffer to write the encoded node to
     * @throws TrieEncoderException If any exception occurs during encoding
     */
    public static void encode(Node node, OutputStream buffer) {
        try {
            encodeHeader(node, buffer);
//            if (node.getStorageValue() == null) {
//                // Only encode the header of empty node variant
//                return;
//            }
            byte[] keyLE = Nibbles.nibblesToKeyLE(node.getPartialKey());
            buffer.write(keyLE);

            if (node.getKind() == NodeKind.Branch) {
                try (ScaleCodecWriter writer = new ScaleCodecWriter(buffer)) {
                    writer.writeUint16(node.getChildrenBitmap());
                } catch (IOException e) {
                    throw new TrieEncoderException(e.getMessage());
                }
            }

            // Only encode node storage value if the node has a storage value,
            // even if it is empty. Do not encode if the branch is without value.
            // Note leaves and branches with value cannot have a `null` storage value.
            if (node.getStorageValue() != null) {
                if (node.isValueHashed()) {
                    if (node.getStorageValue().length != Hash256.SIZE_BYTES) {
                        throw new TrieEncoderException("Hashed value must be 32 bytes");
                    }
                    buffer.write(node.getStorageValue());
                } else {
                    try (ScaleCodecWriter writer = new ScaleCodecWriter(buffer)) {
                        writer.writeAsList(node.getStorageValue());
                    } catch (IOException e) {
                        throw new TrieEncoderException(e.getMessage());
                    }
                }
            }

            if (node.getKind() == NodeKind.Branch) {
                encodeChildren(node.getChildren(), buffer);
            }
        } catch (IOException e) {
            throw new TrieEncoderException(e.getMessage());
        }
    }

    /**
     * Encodes the header of the node and writes it to the buffer.
     *
     * @param node   The node to encode
     * @param buffer The buffer to write the encoded header to
     * @throws TrieEncoderException If any exception occurs during encoding
     */
    public static void encodeHeader(Node node, OutputStream buffer) {
        int partialKeyLength = node.getPartialKey().length;
        if (partialKeyLength > MAX_PARTIAL_KEY_LENGTH) {
            throw new TrieEncoderException("Partial key length is too big: " + partialKeyLength);
        }

        try {
            NodeVariant nodeVariant;
            if (node.getKind() == NodeKind.Leaf) {
                nodeVariant = NodeVariant.LEAF;
            } else if (node.getStorageValue() == null) {
                nodeVariant = NodeVariant.BRANCH;
            } else {
                nodeVariant = NodeVariant.BRANCH_WITH_VALUE;
            }

            int headerByte = nodeVariant.bits;
            int partialKeyLengthMask = nodeVariant.mask;
            if (partialKeyLength < partialKeyLengthMask) {
                // Partial key length fits in header byte
                headerByte |= partialKeyLength;
                buffer.write(headerByte);
                return;
            }

            // Partial key length does not fit in header byte only
            headerByte |= partialKeyLengthMask;
            partialKeyLength -= partialKeyLengthMask;
            buffer.write(headerByte);

            while (true) {
                headerByte = 255;
                if (partialKeyLength < 255) {
                    headerByte = partialKeyLength;
                }

                buffer.write(headerByte);
                partialKeyLength -= headerByte;

                if (headerByte < 255) {
                    break;
                }
            }

        } catch (IOException e) {
            throw new TrieEncoderException(e.getMessage());
        }
    }

    /**
     * Encodes the children of a branch node and writes them to the buffer.
     *
     * @param children The children nodes to encode
     * @param buffer   The buffer to write the encoded children to
     * @throws TrieEncoderException If any exception occurs during encoding
     */
    public static void encodeChildren(Node[] children, OutputStream buffer) {
        for (Node child : children) {
            if (child == null) {
                continue;
            }

            encodeChild(child, buffer);
        }
    }

    /**
     * Encodes a child node and writes it to the buffer.
     *
     * @param child  The child node to encode
     * @param buffer The buffer to write the encoded child to
     * @throws TrieEncoderException If any exception occurs during encoding
     */
    public static void encodeChild(Node child, OutputStream buffer) {
        byte[] merkleValue = child.calculateMerkleValue();
        try (ScaleCodecWriter writer = new ScaleCodecWriter(buffer)) {
            writer.writeAsList(merkleValue);
        } catch (IOException e) {
            throw new TrieEncoderException(e.getMessage());
        }
    }
}