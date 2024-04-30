package com.limechain.trie;

import com.limechain.runtime.version.StateVersion;
import com.limechain.storage.DeleteByPrefixResult;
import com.limechain.storage.KVRepository;
import com.limechain.storage.trie.TrieStorage;
import com.limechain.trie.structure.Entry;
import com.limechain.trie.structure.NodeHandle;
import com.limechain.trie.structure.TrieNodeIndex;
import com.limechain.trie.structure.TrieStructure;
import com.limechain.trie.structure.Vacant;
import com.limechain.trie.structure.database.NodeData;
import com.limechain.trie.structure.nibble.Nibble;
import com.limechain.trie.structure.nibble.Nibbles;
import com.limechain.utils.HashUtils;
import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@AllArgsConstructor
public class TrieAccessor implements KVRepository<Nibbles, byte[]> {

    public static final String TRANSACTIONS_NOT_SUPPORTED = "Block Trie Accessor does not support transactions.";
    private final Map<Nibbles, ChildTrieAccessor> loadedChildTries;
    protected final TrieStorage trieStorage;
    protected byte[] lastRoot;
    private TrieStructure<NodeData> initialTrie;
    private List<TrieNodeIndex> updates;

    public TrieAccessor(byte[] trieRoot) {
        this.lastRoot = trieRoot;
        this.loadedChildTries = new HashMap<>();
        this.trieStorage = TrieStorage.getInstance();
        this.initialTrie = trieStorage.loadTrieStructure(trieRoot);
    }

    /**
     * Retrieves the child trie accessor for the given key.
     *
     * @param key The key corresponding to the child trie accessor.
     * @return The ChildTrieAccessor for the specified key.
     */
    public ChildTrieAccessor getChildTrie(Nibbles key) {
        Nibbles trieKey = Nibbles.fromBytes(":child_storage:default:".getBytes()).addAll(key);
        byte[] merkleRoot = find(trieKey).orElse(null);
        return loadedChildTries.computeIfAbsent(trieKey, k -> new ChildTrieAccessor(this, trieKey, merkleRoot));
    }

    /**
     * Saves a key-value pair to the trie.
     *
     * @param key   The key to save.
     * @param value The value to save.
     * @return True if the operation was successful, false otherwise.
     */
    @Override
    public boolean save(Nibbles key, byte[] value) {
        NodeData nodeData = new NodeData(value);
        initialTrie.insertNode(key, nodeData);
        return true;
    }

    /**
     * Finds the value associated with the given key in the trie.
     *
     * @param key The key to search for.
     * @return An Optional containing the value if found, or empty otherwise.
     */
    @Override
    public Optional<byte[]> find(Nibbles key) {
        return initialTrie.existingNode(key)
                .map(NodeHandle::getUserData)
                .map(NodeData::getValue);
    }

    /**
     * Finds the Merkle value associated with the given key in the trie.
     *
     * @param key The key to search for.
     * @return An Optional containing the Merkle value if found, or empty otherwise.
     */
    public Optional<byte[]> findMerkleValue(Nibbles key) {
        return trieStorage.getByKeyFromMerkle(lastRoot, key)
                .map(NodeData::getMerkleValue);
    }

    /**
     * Deletes the value associated with the given key from the trie.
     *
     * @param key The key to delete.
     * @return True if the operation was successful, false otherwise.
     */
    @Override
    public boolean delete(Nibbles key) {
        Entry<NodeData> node = initialTrie.node(key);
        if (!(node instanceof Vacant<NodeData>)) {
            NodeHandle<NodeData> nodeHandle = node.asNodeHandle();
            initialTrie.clearNodeValue(nodeHandle.getNodeIndex());
            return true;
        }
        return false;
    }

    @Override
    public List<byte[]> findKeysByPrefix(Nibbles prefixSeek, int limit) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Deletes keys in the trie that match the given prefix.
     *
     * @param prefix The prefix to match for deletion.
     * @param limit  The maximum number of keys to delete.
     * @return A DeleteByPrefixResult indicating the number of keys deleted and whether all keys were deleted.
     */
    @Override
    public DeleteByPrefixResult deleteByPrefix(Nibbles prefix, Long limit) {
        Optional<NodeHandle<NodeData>> optionalNodeHandle = initialTrie.existingNode(prefix);

        if (!optionalNodeHandle.isPresent()) {
            return new DeleteByPrefixResult(0, true);
        }

        NodeHandle<NodeData> nodeHandle = optionalNodeHandle.get();
        int deleted = 0;

        for (Nibble nibble : Nibbles.ALL) {
            deleted += nodeHandle.getChild(nibble)
                    .map(NodeHandle::getNodeIndex)
                    .map(childIndex -> initialTrie.clearNodeValueRecursive(childIndex, limit)).orElse(0);
        }

        if (limit != null && deleted >= limit) {
            return new DeleteByPrefixResult(deleted, false);
        }

        initialTrie.clearNodeValue(nodeHandle.getNodeIndex());
        return new DeleteByPrefixResult(deleted, true);
    }

    public void persistUpdates() {
        for (ChildTrieAccessor value : loadedChildTries.values()) value.persistUpdates();
        loadedChildTries.clear();

        trieStorage.updateTrieStorage(initialTrie, updates, StateVersion.V0);
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
            if(childNode == null) continue;
            Nibbles nextPath = currentPath.add(nibble).addAll(childNode.getPartialKey());
            Optional<Nibbles> result = findNextKey(childNode, prefix, nextPath);
            if (result.isPresent()) {
                return result;
            }
        }

        return Optional.empty();
    }

    @Override
    public void startTransaction() {
        throw new UnsupportedOperationException(TRANSACTIONS_NOT_SUPPORTED);

    }

    @Override
    public void rollbackTransaction() {
        throw new UnsupportedOperationException(TRANSACTIONS_NOT_SUPPORTED);

    }

    @Override
    public void commitTransaction() {
        throw new UnsupportedOperationException(TRANSACTIONS_NOT_SUPPORTED);

    }

    @Override
    public void closeConnection() {
        throw new UnsupportedOperationException(TRANSACTIONS_NOT_SUPPORTED);
    }

    /**
     * Retrieves the Merkle root hash of the trie with the specified state version.
     *
     * @param version The state version.
     * @return The Merkle root hash.
     */
    public byte[] getMerkleRoot(StateVersion version) {
        this.updates = TrieStructureFactory.recalculateMerkleValues(initialTrie, version, HashUtils::hashWithBlake2b);

        this.lastRoot = initialTrie.getRootNode()
                .map(NodeHandle::getUserData)
                .map(NodeData::getMerkleValue)
                .orElseThrow();

        return lastRoot;
    }

}
