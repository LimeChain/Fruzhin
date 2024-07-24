package com.limechain.trie;

import com.limechain.runtime.version.StateVersion;
import com.limechain.storage.DeleteByPrefixResult;
import com.limechain.storage.trie.TrieStorage;
import com.limechain.trie.cache.node.PendingInsertUpdate;
import com.limechain.trie.structure.NodeHandle;
import com.limechain.trie.structure.TrieNodeIndex;
import com.limechain.trie.structure.TrieStructure;
import com.limechain.trie.structure.database.NodeData;
import com.limechain.trie.structure.nibble.Nibble;
import com.limechain.trie.structure.nibble.Nibbles;
import com.limechain.utils.HashUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public abstract sealed class MemoryTrieAccessor extends TrieAccessor
    permits BlockTrieAccessor, MemoryChildTrieAccessor {

    private final TrieStructure<NodeData> initialTrie;
    private List<TrieNodeIndex> updates;
    private DiskTrieService diskTrieService;
    byte[] testRoot;

    MemoryTrieAccessor(TrieStorage trieStorage, byte[] mainTrieRoot) {
        super(trieStorage, mainTrieRoot);

        this.updates = new ArrayList<>();
        this.initialTrie = trieStorage.loadTrieStructure(mainTrieRoot);
        diskTrieService = new DiskTrieService(trieStorage);
        testRoot = mainTrieRoot;
    }

    MemoryTrieAccessor(TrieStorage trieStorage, byte[] mainTrieRoot, TrieStructure<NodeData> trieStructure) {
        super(trieStorage, mainTrieRoot);

        this.initialTrie = trieStructure;
        diskTrieService = new DiskTrieService(trieStorage);
        testRoot = mainTrieRoot;
    }

    @Override
    public void upsertNode(Nibbles key, byte[] value) {
        NodeData nodeData = new NodeData(value);
        initialTrie.insertNode(key, nodeData, currentStateVersion);
        diskTrieService.upsertNode(testRoot, key, value, currentStateVersion);
    }

    @Override
    public void deleteNode(Nibbles key) {
        initialTrie.deleteStorageNodeAt(key);
        diskTrieService.deleteStorageNode(testRoot, key);
    }

    @Override
    public Optional<byte[]> findStorageValue(Nibbles key) {
        var test = diskTrieService.findStorageValue(mainTrieRoot, key);
        return initialTrie.existingNode(key)
            .map(NodeHandle::getUserData)
            .map(NodeData::getValue);
    }

    @Override
    public DeleteByPrefixResult deleteMultipleNodesByPrefix(Nibbles prefix, Long limit) {
        var result = diskTrieService.deleteMultipleNodesByPrefix(mainTrieRoot, prefix, limit);
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
        var test = diskTrieService.getNextKey(mainTrieRoot, key);
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
        super.persistChanges();
        trieStorage.updateTrieStorage(initialTrie, updates);
        testRoot = mainTrieRoot;
        diskTrieService.trieChanges.clear();
    }

    @Override
    public MemoryChildTrieAccessor getChildTrie(Nibbles key) {
        return (MemoryChildTrieAccessor) super.getChildTrie(key);
    }

    @Override
    public MemoryChildTrieAccessor createChildTrie(Nibbles trieKey, byte[] merkleRoot) {
        return new MemoryChildTrieAccessor(trieStorage, this, trieKey, merkleRoot);
    }

    /**
     * Retrieves the Merkle root hash of the trie with the specified state version.
     *
     * @param version The state version.
     * @return The Merkle root hash.
     */
    public byte[] getMerkleRoot(StateVersion version) {
        updates = TrieStructureFactory.recalculateMerkleValues(initialTrie, version, HashUtils::hashWithBlake2b);

        mainTrieRoot = initialTrie.getRootNode()
            .map(NodeHandle::getUserData)
            .map(NodeData::getMerkleValue)
            .orElseThrow();

        Boolean test = null;
        if (this.diskTrieService.trieChanges.changes.firstEntry() != null) {
            test = Arrays.equals(mainTrieRoot, ((PendingInsertUpdate) this.diskTrieService.trieChanges.changes.firstEntry().getValue()).newMerkleValue());
        }

        return mainTrieRoot;
    }
}
