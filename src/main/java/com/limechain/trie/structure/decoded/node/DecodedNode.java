package com.limechain.trie.structure.decoded.node;

import com.limechain.trie.NodeVariant;
import com.limechain.trie.structure.decoded.node.exceptions.NodeEncodingException;
import com.limechain.trie.structure.nibble.Nibble;
import com.limechain.trie.structure.nibble.Nibbles;
import com.limechain.trie.structure.nibble.NibblesToBytes;
import com.limechain.utils.scale.ScaleUtils;
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

    //NOTE: This will never be constructed internally, only assigned from outside.
    private C[] children;

    private I partialKey;

    @Getter
    @Nullable
    private StorageValue storageValue;

    public boolean hasChildren() {
        // NOTE: Maybe more inefficient than a for loop, but more readable :)
        return Arrays.stream(this.children).anyMatch(Objects::nonNull);
    }

    public int getChildrenBitmap() {
        return IntStream.range(0, CHILDREN_COUNT)
            .filter(i -> Objects.nonNull(this.children[i]))
            .reduce(0, (bitmap, i) -> bitmap | (1 << i));
    }

    /**
     * Calculates the NodeVariant of this node depending on whether:
     * - it has children (i.e. branch / leaf node)
     * - it has a storage value
     * - this storage value (if present) is hashed
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
        int maxRepresentableInFirstByte = variant.getPartialKeyLengthHeaderMask();
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
        // But for now, we encode each inidividual children as a separate List<Byte> and simply concat them
        */
        List<Byte> childrenNodeValues =
            Stream.of(this.children)
                .filter(Objects::nonNull) //NOTE: Suspected trouble with null objects...
                .flatMap(childValue -> {
                    byte[] scaleEncodedChildValue = ScaleUtils.Encode.encodeAsListOfBytes(childValue);
                    return Stream.of(ArrayUtils.toObject(scaleEncodedChildValue));
                })
                .toList();
        subvalue.addAll(childrenNodeValues);

        // Return everything accumulated thus far
        return subvalue;
    }


    // TODO: Test exhaustively (fine bit twiddling, must be really sure it's accurate)
    /**
     *    Encodes the components of a node value into the node value itself.
     *  <br>
     *     This function returns an iterator of buffers. The actual node value is the concatenation of
     *     these buffers put together.
     *  <br>
     *     > <b>Note</b>:
     *      The returned iterator might contain a reference to the storage value and children
     *      values in the [`DecodedNode`]. By returning an iterator of buffers, we avoid copying
     *      these storage value and children values.
     *  <br>
     *     This encoding is independent of the trie version.
     * @return The return value is composed of three parts:<br>
     *          - node header,<br>
     *          - the partial key,<br>
     *          - the node subvalue.
     * @throws NodeEncodingException if the node represents invalid state;
     *                           for now only if it has a partial key, but no children and no storage value
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
}
