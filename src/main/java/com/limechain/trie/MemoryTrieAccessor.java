package com.limechain.trie;

import com.limechain.runtime.version.StateVersion;
import com.limechain.storage.DeleteByPrefixResult;
import com.limechain.storage.trie.TrieStorage;
import com.limechain.trie.structure.NodeHandle;
import com.limechain.trie.structure.TrieNodeIndex;
import com.limechain.trie.structure.TrieStructure;
import com.limechain.trie.structure.database.NodeData;
import com.limechain.trie.structure.nibble.Nibble;
import com.limechain.trie.structure.nibble.Nibbles;
import com.limechain.utils.HashUtils;
import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@AllArgsConstructor
public abstract sealed class MemoryTrieAccessor implements TrieAccessor
    permits BlockTrieAccessor, ChildTrieAccessor {

    protected final TrieStorage trieStorage;
    protected byte[] mainTrieRoot;

    private final Map<Nibbles, ChildTrieAccessor> loadedChildTries;
    private TrieStructure<NodeData> initialTrie;
    private List<TrieNodeIndex> updates;

    MemoryTrieAccessor(TrieStorage trieStorage, byte[] mainTrieRoot) {
        this.trieStorage = trieStorage;
        this.mainTrieRoot = mainTrieRoot;

        this.loadedChildTries = new HashMap<>();
        this.initialTrie = trieStorage.loadTrieStructure(mainTrieRoot);
    }

    /**
     * Retrieves the child trie accessor for the given key.
     *
     * @param key The key corresponding to the child trie accessor.
     * @return The ChildTrieAccessor for the specified key.
     */
    public ChildTrieAccessor getChildTrie(Nibbles key) {
        Nibbles trieKey = Nibbles.fromBytes(":child_storage:default:".getBytes()).addAll(key);
        byte[] merkleRoot = findStorageValue(trieKey).orElse(null);
        return loadedChildTries.computeIfAbsent(trieKey, k -> new ChildTrieAccessor(this.trieStorage, this, trieKey, merkleRoot));
    }

    @Override
    public void upsertNode(Nibbles key, byte[] value) {
        NodeData nodeData = new NodeData(value);
        initialTrie.insertNode(key, nodeData);
    }

    @Override
    public void deleteNode(Nibbles key) {
        initialTrie.deleteStorageNodeAt(key);
    }

    @Override
    public Optional<byte[]> findStorageValue(Nibbles key) {
        return initialTrie.existingNode(key)
            .map(NodeHandle::getUserData)
            .map(NodeData::getValue);
    }

    @Override
    public DeleteByPrefixResult deleteMultipleNodesByPrefix(Nibbles prefix, Long limit) {
        Optional<NodeHandle<NodeData>> optionalNodeHandle = initialTrie.existingNode(prefix);

        if (optionalNodeHandle.isEmpty()) {
            return new DeleteByPrefixResult(0, true);
        }

        NodeHandle<NodeData> nodeHandle = optionalNodeHandle.get();
        AtomicInteger deleted = new AtomicInteger(0);

        for (Nibble nibble : Nibbles.ALL) {
            nodeHandle.getChild(nibble)
                .map(NodeHandle::getNodeIndex)
                .ifPresent(childIndex -> initialTrie.deleteNodesRecursively(childIndex, limit, deleted));
        }

        if (limit != null && deleted.get() >= limit) {
            return new DeleteByPrefixResult(deleted.get(), false);
        }

        if (nodeHandle.hasStorageValue()) {
            initialTrie.deleteStorageNodeAt(nodeHandle.getFullKey());
            deleted.incrementAndGet();
        } else {
            initialTrie.deleteInternalNodeAt(nodeHandle.getFullKey());
        }

        return new DeleteByPrefixResult(deleted.get(), true);
    }

    @Override
    public Optional<Nibbles> getNextKey(Nibbles key) {
        NodeHandle<NodeData> rootHandle = initialTrie.getRootNode().orElse(null);
        return findNextKey(rootHandle, key, Nibbles.EMPTY);
    }

    private Optional<Nibbles> findNextKey(NodeHandle<NodeData> node, Nibbles prefix, Nibbles currentPath) {
        if (node == null) {
            return Optional.empty();
        }

        // If the current node is a leaf and the fullPath is greater than the prefix, it's a candidate.
        if (node.getUserData() != null && node.getUserData().getValue() != null && currentPath.compareTo(prefix) > 0) {
            return Optional.of(currentPath);
        }

        for (Nibble nibble : Nibbles.ALL) {
            NodeHandle<NodeData> childNode = node.getChild(nibble).orElse(null);
            if (childNode == null) continue;
            Nibbles nextPath = currentPath.add(nibble).addAll(childNode.getPartialKey());
            Optional<Nibbles> result = findNextKey(childNode, prefix, nextPath);
            if (result.isPresent()) {
                return result;
            }
        }

        return Optional.empty();
    }

    @Override
    public void persistChanges() {
        for (ChildTrieAccessor value : loadedChildTries.values()) value.persistChanges();
        loadedChildTries.clear();

        trieStorage.updateTrieStorage(initialTrie, updates, StateVersion.V0);
    }

    /**
     * Retrieves the Merkle root hash of the trie with the specified state version.
     *
     * @param version The state version.
     * @return The Merkle root hash.
     */
    public byte[] getMerkleRoot(StateVersion version) {
        this.updates = TrieStructureFactory.recalculateMerkleValues(initialTrie, version, HashUtils::hashWithBlake2b);

        this.mainTrieRoot = initialTrie.getRootNode()
            .map(NodeHandle::getUserData)
            .map(NodeData::getMerkleValue)
            .orElseThrow();

        return this.mainTrieRoot;
    }
}
