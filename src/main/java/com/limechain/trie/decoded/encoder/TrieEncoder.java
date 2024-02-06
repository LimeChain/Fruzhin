package com.limechain.trie.decoded.encoder;

import com.limechain.trie.decoded.Nibbles;
import com.limechain.trie.decoded.Node;
import com.limechain.trie.decoded.NodeKind;
import com.limechain.trie.decoded.NodeVariant;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.OutputStream;

import static com.limechain.trie.decoded.TrieVerifier.MAX_PARTIAL_KEY_LENGTH;

/**
 * Encodes a {@link Node} to a {@link OutputStream} buffer.
 * <p>
 * Inspired by Gossamer’s implementation approach
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

            // Only encode the header of empty node variant
            if (node == null) {
                return;
            }

            byte[] keyLE = Nibbles.nibblesToKeyLE(node.getPartialKey());
            buffer.write(keyLE);

            if (node.getKind() == NodeKind.BRANCH) {
                writeChildrenBitmap(node, buffer);
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
                    writeHashedValue(buffer, node.getStorageValue());
                }
            }

            if (node.getKind() == NodeKind.BRANCH) {
                encodeChildren(node.getChildren(), buffer);
            }
        } catch (IOException e) {
            throw new TrieEncoderException(e.getMessage());
        }
    }

    private void writeHashedValue(OutputStream buffer, byte[] storageValue) {
        try (ScaleCodecWriter writer = new ScaleCodecWriter(buffer)) {
            writer.writeAsList(storageValue);
        } catch (IOException e) {
            throw new TrieEncoderException(e.getMessage());
        }
    }

    private void writeChildrenBitmap(Node node, OutputStream buffer) {
        try (ScaleCodecWriter writer = new ScaleCodecWriter(buffer)) {
            writer.writeUint16(node.getChildrenBitmap());
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
        try {
            if (node == null) {
                buffer.write(NodeVariant.EMPTY.bits);
                return;
            }

            int partialKeyLength = node.getPartialKey().length;
            if (partialKeyLength > MAX_PARTIAL_KEY_LENGTH) {
                throw new TrieEncoderException("Partial key length is too big: " + partialKeyLength);
            }

            NodeVariant nodeVariant;
            if (node.getKind() == NodeKind.LEAF) {
                if (node.isValueHashed()) {
                    nodeVariant = NodeVariant.LEAF_WITH_HASHED_VALUE;
                } else {
                    nodeVariant = NodeVariant.LEAF;
                }
            } else if (node.getStorageValue() == null) {
                nodeVariant = NodeVariant.BRANCH;
            } else if (node.isValueHashed()) {
                nodeVariant = NodeVariant.BRANCH_WITH_HASHED_VALUE;
            } else {
                nodeVariant = NodeVariant.BRANCH_WITH_VALUE;
            }

            int headerByte = nodeVariant.bits;
            int partialKeyLengthMask = nodeVariant.getPartialKeyLengthHeaderMask();

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
                headerByte = Math.min(partialKeyLength, 255);

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
        writeHashedValue(buffer, merkleValue);
    }
}