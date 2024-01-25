package com.limechain.trie.structure.database;

import com.google.common.collect.Lists;
import com.google.common.primitives.Bytes;
import com.limechain.trie.structure.NodeHandle;
import com.limechain.trie.structure.TrieNodeIndex;
import com.limechain.trie.structure.TrieStructure;
import com.limechain.trie.structure.decoded.node.DecodedNode;
import com.limechain.trie.structure.decoded.node.StorageValue;
import com.limechain.trie.structure.nibble.Nibble;
import com.limechain.trie.structure.nibble.Nibbles;
import com.limechain.trie.structure.node.InsertTrieNode;
import com.limechain.utils.HashUtils;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class InsertTrieBuilder {
    // TODO: Figure out where we'll fetch this state version from
    public static final int STATE_VERSION = 1;

    @Getter
    private TrieStructure<NodeData> trieStructure;

    public InsertTrieBuilder initializeTrieStructure(Map<String, String> mainStorage) {
        TrieStructure<NodeData> trie = buildTrieStructure(mainStorage);

        List<TrieNodeIndex> nodeIndices = trie.streamOrdered().toList();

        for (TrieNodeIndex index : Lists.reverse(nodeIndices)) {
            NodeHandle<NodeData> nodeHandle = trie.nodeHandleAtIndex(index);
            if (nodeHandle == null) {
                throw new TrieBuildException("Could not initialize trie");
            }
            updateMerkleValue(nodeHandle);
        }

        this.trieStructure = trie;
        return this;
    }

    private TrieStructure<NodeData> buildTrieStructure(Map<String, String> mainStorage) {
        TrieStructure<NodeData> trie = new TrieStructure<>();

        for (Map.Entry<String, String> entry : mainStorage.entrySet()) {
            Nibbles key = Nibbles.fromBytes(entry.getKey().getBytes());
            byte[] value = entry.getValue().getBytes();
            trie.insertNode(key, new NodeData(value));
        }

        return trie;
    }

    private void updateMerkleValue(NodeHandle<NodeData> nodeHandle) {
        NodeData userData = nodeHandle.getUserData();
//        if (userData == null) {
//            return;
//        }

        if (userData == null) {
            userData = new NodeData(null);
        }

        StorageValue storageValue = userData.getValue() != null ? getStorageValue(userData.getValue()) : null;
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

    private StorageValue getStorageValue(@NotNull byte[] value) {
        if (STATE_VERSION == 1 && value.length >= 33) {
            return new StorageValue(HashUtils.hashWithBlake2b(value), true);
        }

        return new StorageValue(value, false);
    }

    private List<List<Byte>> getChildrenValues(NodeHandle<NodeData> nodeHandle) {
        return IntStream.range(0, 16)
                .mapToObj(i -> nodeHandle.getChild(Nibble.fromInt(i)))
                .map(child -> child
                        .map(NodeHandle::getUserData)
                        .map(NodeData::getMerkleValue)
                        .map(Bytes::asList)
                        .orElse(null)
                )
                .toList();
    }

    /**
     * Builds a list of {@link InsertTrieNode} objects representing the nodes in a trie structure.
     * Each trie node is constructed with its storage value, merkle value, children's merkle values,
     * and partial key nibbles.
     *
     * @return A list of {@link InsertTrieNode} objects representing the nodes in the given trie structure.
     * @throws IllegalStateException if the user data in the trie structure is empty or null, which
     *                               indicates an invalid state for the trie nodes.
     */
    public List<InsertTrieNode> build() {
        return trieStructure.streamUnordered().map(this::prepareForInsert).toList();
    }

    private InsertTrieNode prepareForInsert(TrieNodeIndex nodeIndex) {
        NodeData userData = trieStructure.getUserDataAtIndex(nodeIndex);
        if (userData == null || userData.getMerkleValue() == null) {
            throw new IllegalStateException("Merkle value should not be empty!");
        }

        NodeHandle<NodeData> nodeHandle = trieStructure.nodeHandleAtIndex(nodeIndex);
        if (nodeHandle == null) {
            throw new TrieBuildException("Failed to build trie.");
        }

        return new InsertTrieNode(
                userData.getValue(),
                userData.getMerkleValue(),
                childrenMerkleValues(nodeHandle),
                nodeHandle.getPartialKey().asUnmodifiableList(),
                false);
    }

    private List<byte[]> childrenMerkleValues(NodeHandle<NodeData> nodeHandle) {
        return Nibbles.ALL.stream()
                .map(nodeHandle::getChild)
                .map(child -> child
                        .map(NodeHandle::getUserData)
                        .map(NodeData::getMerkleValue)
                        .orElse(null))
                .toList();
    }
}
