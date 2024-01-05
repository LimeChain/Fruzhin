package com.limechain.trie.structure.decoded.node;

import com.limechain.trie.NodeVariant;
import com.limechain.trie.structure.decoded.node.exceptions.NodeDecodingException;
import com.limechain.trie.structure.decoded.node.exceptions.NodeEncodingException;
import com.limechain.trie.structure.nibble.BytesToNibbles;
import com.limechain.trie.structure.nibble.Nibble;
import com.limechain.trie.structure.nibble.Nibbles;
import com.limechain.trie.structure.nibble.NibblesToBytes;
import com.limechain.utils.scale.ScaleUtils;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

// BIG TODO
// TODO: implement `decode()` like: https://github.com/smol-dot/smoldot/blob/200214a571af30b5fa3997aea988451adc235ed0/lib/src/trie/trie_node.rs#L317

// NOTE:
//  Those `extends` restrictions on the generic types are only used for `.size()` methods
//  These needn't be restrictions on the node itself, but rather only for the `encode` method
//  Which could alternatively be implemented as a generic static methods only for nodes obeying these constraints
//  But we don't need that much overcomplicating now.
@AllArgsConstructor
public class DecodedNode<I extends Collection<Nibble>, C extends Collection<Byte>> {
    public final static int CHILDREN_COUNT = 16;

    private static final int HASHED_STORAGE_VALUE_LENGTH = 32;
    private static final int CHILD_BYTES_SIZE_LIMIT = 32;

    //NOTE: This will never be constructed internally, only assigned from outside.
    @Getter
    private List<C> children;

    @Getter
    private I partialKey;

    @Getter
    @Nullable
    private StorageValue storageValue;

    public boolean hasChildren() {
        // NOTE: Maybe more inefficient than a for loop, but more readable :)
        return Arrays.stream(this.children.toArray()).anyMatch(Objects::nonNull);
    }

    public int getChildrenBitmap() {
        return IntStream.range(0, CHILDREN_COUNT)
                .filter(i -> Objects.nonNull(this.children.get(i)))
                .reduce(0, (bitmap, i) -> bitmap | (1 << i));
    }

    /**
     * Calculates the NodeVariant of this node depending on whether:
     * - it has children (i.e. branch / leaf node)
     * - it has a storage value
     * - this storage value (if present) is hashed
     *
     * @return the NodeVariant of this DecodedNode
     */
    public NodeVariant calculateNodeVariant() {
        boolean hasChildren = this.hasChildren();
        boolean hasStorageValue = this.storageValue != null;
        boolean valueHashed = hasStorageValue && this.storageValue.isHashed();

        NodeVariant result;
        if (!hasChildren && hasStorageValue && !valueHashed) {
            result = NodeVariant.LEAF;
        } else if (hasChildren && !hasStorageValue) {
            result = NodeVariant.BRANCH;
        } else if (hasChildren && hasStorageValue && !valueHashed) {
            result = NodeVariant.BRANCH_WITH_VALUE;
        } else if (!hasChildren && hasStorageValue && valueHashed) {
            result = NodeVariant.LEAF_WITH_HASHED_VALUE;
        } else if (hasChildren && hasStorageValue && valueHashed) {
            result = NodeVariant.BRANCH_WITH_HASHED_VALUE;
        } else { // NOTE: if (!hasChildren && !hasStorageValue)
            if (!this.partialKey.isEmpty()) {
                throw new NodeEncodingException("Trie node has a partial key, but no children and no storage value.");
            }

            result = NodeVariant.EMPTY;
        }

        return result;
    }

    private List<Byte> encodeNodeHeader() {
        List<Byte> beforeStorageValue = new ArrayList<>(2 + (this.partialKey.size() / 255));
        var decoded = this;

        // Calculate the first byte
        NodeVariant variant = decoded.calculateNodeVariant();
        int variantPartialKeyLenBitsFirstByte = variant.getPartialKeyBitLengthInFirstByte();
        int maxRepresentableInFirstByte = (1 << variantPartialKeyLenBitsFirstByte) - 1;
        byte firstByte = (byte) (variant.bits | Math.min(decoded.partialKey.size(), maxRepresentableInFirstByte)); // a byte cast effectively trims to the last 8 bits, exactly what we want

        beforeStorageValue.add(firstByte);

        // Append as many "private key length" bytes as necessary
        int pkLen = decoded.partialKey.size();
        if (pkLen > maxRepresentableInFirstByte) {
            int remainingPkLen = pkLen - maxRepresentableInFirstByte;
            int numberOfFullBytes = remainingPkLen / 255;
            int lastByte = remainingPkLen % 255;

            for (int __ = 0; __ < numberOfFullBytes; ++__) {
                beforeStorageValue.add((byte) 255);
            }
            beforeStorageValue.add((byte) lastByte);
        }

        return beforeStorageValue;
    }

    private List<Byte> encodePartialKey() {
        return new NibblesToBytes(new Nibbles(this.partialKey)).paddingPrepend();
    }

    // TODO: Optimize, a lot of unnecessary copying is going on.
    private List<Byte> encodeSubvalue() {
        List<Byte> subvalue = new LinkedList<>(); //NOTE: Maybe Arraylist?

        // First, push the children bitmap (if a branch node)
        if (this.hasChildren()) {
            //TODO:
            // This doesn't conform to the spec exactly, but that's how smoldot and gossamer do it
            // Why do we reverse the bitmap to LittleEndian when the spec explicitly defines it as a sequence of BITs?
            // https://spec.polkadot.network/chap-state#defn-node-subvalue
            int childrenBitmap = this.getChildrenBitmap();
            subvalue.add((byte) (childrenBitmap & 0x00FF));
            subvalue.add((byte) ((childrenBitmap >> 8) & 0x00FF));
        }

        // Then, encode the storage value
        List<Byte> storageValue;
        if (this.storageValue == null) {
            storageValue = List.of();
        } else {
            // If the storage value is not hashed, we must also include its byte length in the scale encoding
            // TODO:
            //  This seems odd, figure out why we encode the length of the storagevalue only if it's not hashed...?
            //  Perhaps because if its hashed, we know it's 32 bytes only and don't need the length information?
            //  (we know whether it's hashed from the header, so perhaps that's the idea?)
            if (!this.storageValue.isHashed()) {
                // TODO: Do this better, this is awful
                subvalue.addAll(List.of(ArrayUtils.toObject(ScaleUtils.Encode.encodeCompactUInt(this.storageValue.value().length))));
            }

            storageValue = List.of(ArrayUtils.toObject(this.storageValue.value()));
        }
        subvalue.addAll(storageValue);

        // And finally, the children node values
        /*
        // Other implementations are not including the length of children encodings, so I guess we'll skip it, too
        // If needed, this entire block could be shortened to:
        byte[] childrenNodeValues = ScaleUtils.Encode.encodeAsListOfListsOfBytes(Arrays.asList(children));
        // But for now, we encode each individual children as a separate List<Byte> and simply concat them
        */
        List<Byte> childrenNodeValues =
                Stream.of(this.children.toArray())
                        .filter(Objects::nonNull) //NOTE: Suspected trouble with null objects...
                        .flatMap(childValue -> {
                            byte[] scaleEncodedChildValue =
                                    ScaleUtils.Encode.encodeAsListOfBytes((Collection<Byte>) childValue);
                            return Stream.of(ArrayUtils.toObject(scaleEncodedChildValue));
                        })
                        .toList();
        subvalue.addAll(childrenNodeValues);

        // Return everything accumulated thus far
        return subvalue;
    }


    // TODO: Test exhaustively (fine bit twiddling, must be really sure it's accurate)

    /**
     * Encodes the components of a node value into the node value itself.
     * <br>
     * This function returns an iterator of buffers. The actual node value is the concatenation of
     * these buffers put together.
     * <br>
     * > <b>Note</b>:
     * The returned iterator might contain a reference to the storage value and children
     * values in the [`DecodedNode`]. By returning an iterator of buffers, we avoid copying
     * these storage value and children values.
     * <br>
     * This encoding is independent of the trie version.
     *
     * @return The return value is composed of three parts:<br>
     * - node header,<br>
     * - the partial key,<br>
     * - the node subvalue.
     * @throws NodeEncodingException if the node represents invalid state;
     *                               for now only if it has a partial key, but no children and no storage value
     */
    // NOTE:
    //  This return type is quite arbitrary (mainly influenced by Smoldot),
    //  feel free to change accordingly if it becomes too messy
    public Stream<List<Byte>> encode() {
        return Stream.of(
                this.encodeNodeHeader(),
                this.encodePartialKey(),
                this.encodeSubvalue()
        );
    }


    /**
     * Calculates the Merkle value of the given node.
     * `isRootNode` must be `true` if the encoded node is the root node of the trie.
     * Ultimately, almost the same as `encode`, except that the encoding is then optionally hashed.
     * Hashing is performed if the encoded value is 32 bytes or more, or if `is_root_node` is `true`.
     * This is the reason why `is_root_node` must be provided.
     */
    // NOTE:
    //  Passing the hashFunction as a lambda might be insufficient for future use cases, but it's enough for now
    //  Feel free to refactor if needed.
    public byte[] calculateMerkleValue(Function<byte[], byte[]> hashFunction, boolean isRootNode) {
        byte[] nodeValue = ArrayUtils.toPrimitive(this.encode().flatMap(Collection::stream).toArray(Byte[]::new));

        // The node value must be hashed if we're the root or otherwise, if it exceeds 31 bytes of length
        if (isRootNode || nodeValue.length >= 32) {
            nodeValue = hashFunction.apply(nodeValue);
        }

        return nodeValue;
    }

    /***
     * Decodes a node value found in a proof into its components.
     * This can decode nodes no matter their version or hash algorithm.
     *
     * @param encoded The byte array representing the encoded node.
     * @return {@code DecodedNode<Nibbles, List <Byte>>} The decoded node
     * @throws NodeDecodingException If the encoded array is null, empty, or invalid.
     * @throws IllegalArgumentException If 'encoded' is null.
     */
    public static DecodedNode<Nibbles, List<Byte>> decode(byte[] encoded) {
        ScaleCodecReader reader = new ScaleCodecReader(encoded);
        if (encoded == null || encoded.length == 0) {
            throw new NodeDecodingException("Invalid node value: it's empty");
        }

        int firstByte = reader.readByte() & 0xFF;
        boolean hasChildren;
        Boolean storageValueHashed;
        int pkLenFirstByteBits;

        // https://spec.polkadot.network/#defn-node-header
        switch (firstByte >> 6) {
            case 0b00:
                if ((firstByte >> 5) == 0b001) {
                    hasChildren = false;
                    storageValueHashed = true;
                    pkLenFirstByteBits = 5;
                } else if ((firstByte >> 4) == 0b0001) {
                    hasChildren = true;
                    storageValueHashed = true;
                    pkLenFirstByteBits = 4;
                } else if (firstByte == 0) {
                    hasChildren = false;
                    storageValueHashed = null;
                    pkLenFirstByteBits = 6;
                } else {
                    throw new NodeDecodingException("Invalid header bits");
                }
                break;
            case 0b10:
                hasChildren = true;
                storageValueHashed = null;
                pkLenFirstByteBits = 6;
                break;
            case 0b01:
                hasChildren = false;
                storageValueHashed = false;
                pkLenFirstByteBits = 6;
                break;
            case 0b11:
                hasChildren = true;
                storageValueHashed = false;
                pkLenFirstByteBits = 6;
                break;
            default:
                throw new NodeDecodingException("Unknown variant");
        }


        int pkLen = getPkLen(reader, firstByte, pkLenFirstByteBits);

        if (pkLen != 0 && !hasChildren && storageValueHashed == null) {
            throw new NodeDecodingException("Empty trie with partial key ");
        }

        byte[] partialKeyLEBytes = decodePartialKey(reader, pkLen);

        int childrenBitmap = decodeChildrenBitmap(reader, hasChildren);

        byte[] storageValue = decodeStorageValue(reader, storageValueHashed);

        List<List<Byte>> children = decodeChildren(reader, childrenBitmap);
        Nibbles partialKey = new Nibbles(new BytesToNibbles(partialKeyLEBytes));
        if (pkLen % 2 == 1) {
            // This is for situations where the partial key contains a `0` prefix that exists for
            // alignment but doesn't actually represent a nibble.
            partialKey.remove(0);
        }
        return new DecodedNode<>(children, partialKey, new StorageValue(storageValue, storageValueHashed));
    }

    /**
     * Calculates the length of the partial key in nibbles
     *
     * @param reader             ScaleCodecReader used to read the encoded node data.
     * @param firstByte          The first byte of the encoded node data.
     * @param pkLenFirstByteBits The number of bits in the first byte dedicated to encoding the partial key length.
     * @return int The length of the partial key.
     * @throws NodeDecodingException If there's an error in decoding, such as if the partial key length is too short,
     *                               or if there's an overflow in calculating the partial key length.
     */
    private static int getPkLen(ScaleCodecReader reader, int firstByte, int pkLenFirstByteBits) {
        int pkLen = firstByte & ((1 << pkLenFirstByteBits) - 1);

        boolean continueIter = pkLen == ((1 << pkLenFirstByteBits) - 1);

        while (continueIter) {
            if (!reader.hasNext()) {
                throw new NodeDecodingException("PartialKeyLenTooShort");
            }
            byte currentByte = reader.readByte();
            continueIter = (currentByte & 0xFF) == 255;
            int addedValue = (currentByte & 0xFF);

            if (pkLen > Integer.MAX_VALUE - addedValue) {
                throw new NodeDecodingException("Partial key length overflow");
            }

            pkLen += addedValue;
        }
        return pkLen;
    }

    /**
     * Extracts the partial key from a node in a trie structure using a ScaleCodecReader.
     *
     * @param reader The ScaleCodecReader used to read the encoded node data.
     * @param pkLen  The length of the partial key in nibbles.
     * @return byte[] The byte array containing the partial key.
     * @throws NodeDecodingException If there are no more bytes to read, or if the partial key has invalid padding.
     */
    private static byte[] decodePartialKey(ScaleCodecReader reader, int pkLen) {
        if (!reader.hasNext()) {
            throw new NodeDecodingException("Node value cannot be null for partial key");
        }

        // Calculate the length of the partial key in bytes
        int pkLenBytes = pkLen == 0 ? 0 : 1 + ((pkLen - 1) / 2);
        byte[] partialKey = reader.readByteArray(pkLenBytes);

        if ((pkLen % 2 == 1) && ((partialKey[0] & 0xf0) != 0)) {
            throw new NodeDecodingException("Invalid Partial key padding");
        }

        return partialKey;
    }

    /**
     * Extracts the children bitmap from a node in a trie structure.
     *
     * @param reader      The ScaleCodecReader used to read the encoded node data.
     * @param hasChildren A boolean indicating whether the node has children.
     * @return int The children bitmap as an integer. If the node has no children, returns 0.
     * @throws NodeDecodingException If there are no more bytes to read, or if the children bitmap is zero.
     */
    private static int decodeChildrenBitmap(ScaleCodecReader reader, boolean hasChildren) {
        if (!reader.hasNext()) {
            throw new NodeDecodingException("Node value cannot be null for children bitmap");
        }

        if (!hasChildren) {
            return 0;
        }
        byte[] bitmap;
        final int BITMAP_DATA_LENGTH = 2;
        bitmap = reader.readByteArray(BITMAP_DATA_LENGTH);
        int childrenBitmap = ((bitmap[0] & 0xFF) | (bitmap[1] & 0xFF) << 8);

        if (childrenBitmap == 0) {
            throw new NodeDecodingException("Zero children bitmap");
        }
        return childrenBitmap;
    }

    /**
     * Extracts the storage value from a node in a trie structure.
     *
     * @param reader             The ScaleCodecReader used to read the encoded node data.
     * @param storageValueHashed A Boolean indicating whether the storage value is hashed. Can be null, true, or false.
     * @return byte[] The storage value as a byte array, or null if there is no storage value.
     * @throws NodeDecodingException If there are no more bytes to read from the reader.
     */
    private static byte[] decodeStorageValue(ScaleCodecReader reader, @Nullable Boolean storageValueHashed) {
        if (!reader.hasNext()) {
            throw new NodeDecodingException("Node value cannot be null for storage value");
        }

        byte[] storageValue;

        if (storageValueHashed == null) {
            storageValue = null;
        } else if (!storageValueHashed) {
            storageValue = reader.readByteArray();
        } else { // storageValueHashed is true
            storageValue = reader.readByteArray(HASHED_STORAGE_VALUE_LENGTH);
        }

        return storageValue;
    }

    /**
     * Extracts the children of a node in a trie structure.
     *
     * @param reader         The ScaleCodecReader used to read the encoded node data.
     * @param childrenBitmap An integer bitmap representing the presence of children. Each bit corresponds to a child.
     * @return List<List < Byte>> A list of children, where each child is represented as a list of bytes.
     * @throws NodeDecodingException If the length of any child's data exceeds the maximum allowed size.
     */
    private static List<List<Byte>> decodeChildren(ScaleCodecReader reader, int childrenBitmap) {
        List<List<Byte>> children = new ArrayList<List<Byte>>(CHILDREN_COUNT);
        for (int i = 0; i < CHILDREN_COUNT; i++) {
            if ((childrenBitmap & (1 << i)) == 0) {
                children.add(i, null);
                continue;
            }
            byte[] value = reader.readByteArray();
            if (value.length > CHILD_BYTES_SIZE_LIMIT) {
                throw new NodeDecodingException("Child too large");
            }
            children.add(i, Arrays.asList(ArrayUtils.toObject(value)));
        }
        return children;
    }
}
