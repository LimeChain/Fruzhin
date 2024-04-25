package com.limechain.trie;

import com.google.common.collect.Lists;
import com.google.common.primitives.Bytes;
import com.google.protobuf.ByteString;
import com.limechain.exception.trie.TrieBuildException;
import com.limechain.runtime.version.StateVersion;
import com.limechain.trie.dto.node.DecodedNode;
import com.limechain.trie.dto.node.StorageValue;
import com.limechain.trie.structure.NodeHandle;
import com.limechain.trie.structure.TrieNodeIndex;
import com.limechain.trie.structure.TrieStructure;
import com.limechain.trie.structure.database.NodeData;
import com.limechain.trie.structure.nibble.Nibbles;
import com.limechain.utils.HashUtils;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

@UtilityClass
public class TrieStructureFactory {

    /**
     * Build the trie structure from the provided key-value pairs, then calculates the merkle values and sets them.
     * @param entries - the key-value pairs that make up the actual data being stored
     * @param stateVersion - for now, all entries are presumed to have the same state version
     * @return - a TrieStructure with calculated merkle values
     */
    public TrieStructure<NodeData> buildFromKVPs(Map<ByteString, ByteString> entries, StateVersion stateVersion) {
        TrieStructure<NodeData> trie = buildTrieStructure(entries);
        calculateMerkleValues(trie, stateVersion, HashUtils::hashWithBlake2b);
        return trie;
    }

    /**
     * Builds a TrieStructure from the provided key-value pairs.
     *
     * @param mainStorage The key-value pairs to be inserted into the trie.
     * @return            A TrieStructure containing the inserted key-value pairs.
     */
    public TrieStructure<NodeData> buildTrieStructure(Map<ByteString, ByteString> mainStorage) {
        TrieStructure<NodeData> trie = new TrieStructure<>();

        for (var entry : mainStorage.entrySet()) {
            Nibbles key = Nibbles.fromBytes(entry.getKey().toByteArray());
            byte[] value = entry.getValue().toByteArray();
            trie.insertNode(key, new NodeData(value));
        }

        return trie;
    }

    /**
     * Calculates the Merkle values for all nodes in the trie and sets them.
     *
     * @param trie          The TrieStructure for which Merkle values are calculated.
     * @param stateVersion  The state version used for constructing the storage values.
     * @param hashFunction  The hash function used for calculating Merkle values.
     */
    public void calculateMerkleValues(TrieStructure<NodeData> trie, StateVersion stateVersion, UnaryOperator<byte[]> hashFunction) {
        List<TrieNodeIndex> nodeIndices = trie.streamOrdered().toList();

        for (TrieNodeIndex index : Lists.reverse(nodeIndices)) {
            NodeHandle<NodeData> nodeHandle = trie.nodeHandleAtIndex(index);
            if (nodeHandle == null) {
                throw new TrieBuildException("Could not initialize trie");
            }
            calculateAndSetMerkleValue(nodeHandle, stateVersion, hashFunction);
        }
    }

    /**
     * Recalculates the Merkle values for all nodes in the trie and returns a list of indices for nodes whose Merkle values have been updated.
     *
     * @param trie          The TrieStructure to recalculate Merkle values for.
     * @param stateVersion  The state version used for constructing the storage values.
     * @param hashFunction  The hash function used for calculating Merkle values.
     * @return              A list of indices for nodes whose Merkle values have been updated.
     */
    public List<TrieNodeIndex> recalculateMerkleValues(TrieStructure<NodeData> trie, StateVersion stateVersion,
                                                       UnaryOperator<byte[]> hashFunction) {
        List<TrieNodeIndex> nodeIndices = trie.streamOrdered().toList();
        List<TrieNodeIndex> updatedNodes = new ArrayList<>();

        for (TrieNodeIndex index : Lists.reverse(nodeIndices)) {
            NodeHandle<NodeData> nodeHandle = trie.nodeHandleAtIndex(index);
            if (nodeHandle == null) {
                throw new TrieBuildException("Could not initialize trie");
            }
            boolean updated = recalculateAndSetMerkleValue(nodeHandle, stateVersion, hashFunction);
            if (updated) {
                updatedNodes.add(index);

            }
        }
        return updatedNodes;
    }

    /**
     * Recalculates the Merkle value for the given node and updates it if necessary.
     *
     * @param nodeHandle    The NodeHandle representing the node for which the Merkle value is recalculated.
     * @param stateVersion  The state version used for constructing the storage values.
     * @param hashFunction  The hash function used for calculating Merkle values.
     * @return              True if the Merkle value was updated, false otherwise.
     */
    private boolean recalculateAndSetMerkleValue(NodeHandle<NodeData> nodeHandle, StateVersion stateVersion,
                                            UnaryOperator<byte[]> hashFunction) {
        NodeData userData = nodeHandle.getUserData();

        // Node didn't have any userData set (hence no storage value), but now we want to calculate its merkle value
        if (userData == null) {
            userData = new NodeData(null);
        }

        StorageValue storageValue = constructStorageValue(userData.getValue(), stateVersion);
        DecodedNode<List<Byte>> decoded = new DecodedNode<>(
            getChildrenValues(nodeHandle),
            nodeHandle.getPartialKey(),
            storageValue);

        byte[] merkleValue = decoded.calculateMerkleValue(
            hashFunction,
                nodeHandle.isRootNode());

        if (userData.getMerkleValue() != null &&
            Bytes.asList(userData.getMerkleValue()).equals(Bytes.asList(merkleValue))) {
            return false;
        } else {
            userData.setMerkleValue(merkleValue);
            nodeHandle.setUserData(userData);
            return true;
        }
    }

    /**
     * Calculates the Merkle value for the given node and sets it.
     *
     * @param nodeHandle    The NodeHandle representing the node for which the Merkle value is calculated.
     * @param stateVersion  The state version used for constructing the storage values.
     * @param hashFunction  The hash function used for calculating Merkle values.
     */
    private void calculateAndSetMerkleValue(NodeHandle<NodeData> nodeHandle, StateVersion stateVersion,
                                            UnaryOperator<byte[]> hashFunction) {
        NodeData userData = nodeHandle.getUserData();

        // Node didn't have any userData set (hence no storage value), but now we want to calculate its merkle value
        if (userData == null) {
            userData = new NodeData(null);
        }

        StorageValue storageValue = constructStorageValue(userData.getValue(), stateVersion);
        DecodedNode<List<Byte>> decoded = new DecodedNode<>(
            getChildrenValues(nodeHandle),
            nodeHandle.getPartialKey(),
            storageValue);

        byte[] merkleValue = decoded.calculateMerkleValue(
            hashFunction,
            nodeHandle.isRootNode());

        userData.setMerkleValue(merkleValue);
        nodeHandle.setUserData(userData);
    }

    /**
     * Constructs a StorageValue object based on the provided value and state version.
     *
     * @param value        The byte array value.
     * @param stateVersion The state version.
     * @return             A StorageValue object constructed based on the provided value and state version.
     */
    private StorageValue constructStorageValue(@Nullable byte[] value, StateVersion stateVersion) {
        if (value == null) {
            return null;
        }

        if (stateVersion == StateVersion.V1 && value.length >= 33) {
            return new StorageValue(HashUtils.hashWithBlake2b(value), true);
        }

        return new StorageValue(value, false);
    }

    private List<List<Byte>> getChildrenValues(NodeHandle<NodeData> nodeHandle) {
        return Nibbles.ALL.stream()
            .map(nodeHandle::getChild)
            .map(child -> child
                .map(NodeHandle::getUserData)
                .map(NodeData::getMerkleValue)
                .map(Bytes::asList)
                .orElse(null)
            )
            .toList();
    }
}
