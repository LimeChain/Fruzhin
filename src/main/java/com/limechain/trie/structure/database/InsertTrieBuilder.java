package com.limechain.trie.structure.database;

import com.limechain.exception.trie.TrieBuildException;
import com.limechain.trie.structure.NodeHandle;
import com.limechain.trie.structure.TrieNodeIndex;
import com.limechain.trie.structure.TrieStructure;
import com.limechain.trie.structure.nibble.Nibbles;
import com.limechain.trie.structure.node.InsertTrieNode;

import java.util.List;

public class InsertTrieBuilder {
    private final TrieStructure<NodeData> trieStructure;

    public InsertTrieBuilder(TrieStructure<NodeData> trieStructure) {
        this.trieStructure = trieStructure;
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

        boolean isReferenceValue = nodeHandle
                .getFullKey()
                .startsWith(Nibbles.fromBytes(":child_storage:".getBytes()));

        return new InsertTrieNode(
                userData.getValue(),
                userData.getMerkleValue(),
                childrenMerkleValues(nodeHandle),
                nodeHandle.getPartialKey().asUnmodifiableList(),
                isReferenceValue);
    }

    private static List<byte[]> childrenMerkleValues(NodeHandle<NodeData> nodeHandle) {
        return Nibbles.ALL.stream()
                .map(nodeHandle::getChild)
                .map(child -> child
                        .map(NodeHandle::getUserData)
                        .map(NodeData::getMerkleValue)
                        .orElse(null))
                .toList();
    }
}
