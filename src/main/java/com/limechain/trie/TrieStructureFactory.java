package com.limechain.trie;

import com.google.common.collect.Lists;
import com.google.common.primitives.Bytes;
import com.google.protobuf.ByteString;
import com.limechain.runtime.StateVersion;
import com.limechain.trie.structure.NodeHandle;
import com.limechain.trie.structure.TrieNodeIndex;
import com.limechain.trie.structure.TrieStructure;
import com.limechain.trie.structure.database.NodeData;
import com.limechain.trie.structure.database.TrieBuildException;
import com.limechain.trie.dto.node.DecodedNode;
import com.limechain.trie.dto.node.StorageValue;
import com.limechain.trie.structure.nibble.Nibbles;
import com.limechain.utils.HashUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

// TODO: Consider splitting the two steps (building the trie + calculating the merkle values) as part of the public API
public class TrieStructureFactory {

    /**
     * Build the trie structure from the provided key-value pairs, then calculates the merkle values and sets them.
     * @param entries - the key-value pairs that make up the actual data being stored
     * @param stateVersion - for now, all entries are presumed to have the same state version
     * @return - a TrieStructure with calculated merkle values
     */
    public static TrieStructure<NodeData> buildFromKVPs(Map<ByteString, ByteString> entries, StateVersion stateVersion) {
        TrieStructure<NodeData> trie = buildTrieStructure(entries);
        calculateMerkleValues(trie, stateVersion);
        return trie;
    }

    private static TrieStructure<NodeData> buildTrieStructure(Map<ByteString, ByteString> mainStorage) {
        TrieStructure<NodeData> trie = new TrieStructure<>();

        for (var entry : mainStorage.entrySet()) {
            Nibbles key = Nibbles.fromBytes(entry.getKey().toByteArray());
            byte[] value = entry.getValue().toByteArray();
            trie.insertNode(key, new NodeData(value));
        }

        return trie;
    }

    private static void calculateMerkleValues(TrieStructure<NodeData> trie, StateVersion stateVersion) {
        List<TrieNodeIndex> nodeIndices = trie.streamOrdered().toList();

        for (TrieNodeIndex index : Lists.reverse(nodeIndices)) {
            NodeHandle<NodeData> nodeHandle = trie.nodeHandleAtIndex(index);
            if (nodeHandle == null) {
                throw new TrieBuildException("Could not initialize trie");
            }
            calculateAndSetMerkleValue(nodeHandle, stateVersion);
        }
    }

    private static void calculateAndSetMerkleValue(NodeHandle<NodeData> nodeHandle, StateVersion stateVersion) {
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
            HashUtils::hashWithBlake2b,
            nodeHandle.isRootNode());

        userData.setMerkleValue(merkleValue);
        nodeHandle.setUserData(userData);
    }

    private static StorageValue constructStorageValue(@Nullable byte[] value, StateVersion stateVersion) {
        if (value == null) {
            return null;
        }

        if (stateVersion == StateVersion.V1 && value.length >= 33) {
            return new StorageValue(HashUtils.hashWithBlake2b(value), true);
        }

        return new StorageValue(value, false);
    }

    private static List<List<Byte>> getChildrenValues(NodeHandle<NodeData> nodeHandle) {
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
