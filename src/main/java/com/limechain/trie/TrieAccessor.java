package com.limechain.trie;

import com.limechain.runtime.version.StateVersion;
import com.limechain.storage.DeleteByPrefixResult;
import com.limechain.storage.KVRepository;
import com.limechain.storage.trie.TrieStorage;
import com.limechain.trie.structure.Entry;
import com.limechain.trie.structure.NodeHandle;
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
    protected final TrieStructure<NodeData> partialTrie;
    protected final TrieStorage trieStorage;
    protected byte[] lastRoot;

    public TrieAccessor(byte[] trieRoot) {
        this.lastRoot = trieRoot;
        this.loadedChildTries = new HashMap<>();
        this.trieStorage = TrieStorage.getInstance();
        this.partialTrie = new TrieStructure<>();
    }

    public ChildTrieAccessor getChildTrie(Nibbles key) {
        Nibbles trieKey = Nibbles.fromBytes(":child_storage:".getBytes()).addAll(key);
        byte[] merkleRoot = find(trieKey).orElse(null);
        return loadedChildTries.computeIfAbsent(trieKey, k -> new ChildTrieAccessor(this, trieKey, merkleRoot));
    }

    @Override
    public boolean save(Nibbles key, byte[] value) {
        loadPathToKey(key);
        loadChildren(key);
        NodeData nodeData = new NodeData(value);
        partialTrie.insertNode(key, nodeData);
        markForRecalculation(key);

        return true;
    }

    private void markForRecalculation(Nibbles key) {
        NodeHandle<NodeData> createdNode = partialTrie.node(key).asNodeHandle();
        Optional.ofNullable(createdNode.getParent())
                .map(NodeHandle::getUserData)
                .ifPresent(data -> data.setMerkleValue(null));
        for (Nibble nibble : Nibbles.ALL) {
            createdNode.getChild(nibble)
                    .map(NodeHandle::getUserData)
                    .ifPresent(data -> data.setMerkleValue(null));
        }
    }

    /**
     * Load the path up to the given key, from the TrieStorage into our partial trie.
     *
     * @param key key
     */
    private void loadPathToKey(Nibbles key) {
        Entry<NodeData> closestNode = partialTrie.node(key);
        if (closestNode instanceof Vacant<NodeData> vacantNode) {
            Optional.of(vacantNode)
                    .map(Vacant::getClosestAncestorIndex)
                    .map(partialTrie::nodeHandleAtIndex)
                    .map(this::closestAncestorMerkleValue)
                    .map(closestMerkle -> trieStorage.entriesBetween(closestMerkle, key.toString()))
                    .ifPresent(entries -> entries.forEach(
                            storageNode -> partialTrie.insertNode(storageNode.key(), storageNode.nodeData()))
                    );
        }
    }

    private byte[] closestAncestorMerkleValue(NodeHandle<NodeData> nodeHandle) {
        if (nodeHandle == null) {
            return lastRoot;
        }

        return Optional.of(nodeHandle)
                .map(NodeHandle::getUserData)
                .map(NodeData::getMerkleValue)
                .orElseGet(() -> closestAncestorMerkleValue(nodeHandle.getParent()));
    }

    private void loadChildren(Nibbles key) {
        Entry<NodeData> entry = partialTrie.node(key);
        if (entry instanceof NodeHandle<NodeData> nodeHandle) {
            Optional.of(nodeHandle)
                    .map(NodeHandle::getUserData)
                    .map(NodeData::getMerkleValue)
                    .map(merkle -> trieStorage.loadChildren(key, merkle))
                    .ifPresent(entries -> entries.forEach(e -> partialTrie.insertNode(e.key(), e.nodeData())));
        }
    }

    @Override
    public Optional<byte[]> find(Nibbles key) {
        loadPathToKey(key);
        return partialTrie.existingNode(key)
                .map(NodeHandle::getUserData)
                .map(NodeData::getValue);
    }

    @Override
    public boolean delete(Nibbles key) {
        loadPathToKey(key);
        loadChildren(key);
        markForRecalculation(key);
        return partialTrie.removeValueAtKey(key);
    }

    @Override
    public List<byte[]> findKeysByPrefix(Nibbles prefixSeek, int limit) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public DeleteByPrefixResult deleteByPrefix(Nibbles prefix, Long limit) {
        loadPathToKey(prefix);

        NodeHandle<NodeData> parent = switch (partialTrie.node(prefix)) {
            case Vacant<NodeData> vacant -> Optional.ofNullable(vacant.getClosestAncestorIndex())
                    .map(partialTrie::nodeHandleAtIndex)
                    .orElse(null);
            case NodeHandle<NodeData> handle -> handle.getParent();
        };

        return Optional.ofNullable(parent)
                .map(NodeHandle::getFullKey)
                .map(parentKey -> prefixIndexInParent(parentKey, prefix))
                .flatMap(parent::getChild)
                .map(c -> lexicographicDelete(c, limit))
                .orElse(new DeleteByPrefixResult(0, true));

    }

    private Nibble prefixIndexInParent(Nibbles parent, Nibbles prefix) {
        for (int i = 0; i < prefix.size(); i++) {
            if(prefix.get(i).equals(parent.get(i))) {
                return parent.get(i);
            }
        }
        return null;
    }

    private DeleteByPrefixResult lexicographicDelete(NodeHandle<NodeData> node, Long limit) {
        loadChildren(node.getFullKey());
        int deleted = 0;
        for (Nibble nibble : Nibbles.ALL) {
            if (limit != null && limit <= deleted) {
                return new DeleteByPrefixResult(deleted, false);
            }
            NodeHandle<NodeData> child = node.getChild(nibble).orElse(null);
            if (child != null) {
                DeleteByPrefixResult childResult = lexicographicDelete(child, limit == null ? null : limit - deleted);
                deleted += childResult.deleted();
                if (!childResult.all()) {
                    return new DeleteByPrefixResult(deleted, false);
                }
            }
        }
        if (partialTrie.clearNodeValue(node.getNodeIndex())) {
            deleted++;
        }
        return new DeleteByPrefixResult(deleted, true);
    }

    public void persistAll() {
        for (ChildTrieAccessor value : loadedChildTries.values()) value.persistAll();
        loadedChildTries.clear();

        if (partialTrie.isEmpty()) return;
        lastRoot = getMerkleRoot(StateVersion.V1);
        trieStorage.insertTrieStorage(partialTrie, StateVersion.V1);
    }

    @Override
    public Optional<Nibbles> getNextKey(Nibbles key) {
        loadPathToKey(key);
        //TODO: optimize using partial trie
        return Optional.ofNullable(trieStorage.getNextKeyByMerkleValue(lastRoot, key.toString()))
                .map(k -> Nibbles.fromBytes(k.getBytes()));
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

    public byte[] getMerkleRoot(StateVersion version) {
        TrieStructureFactory.recalculateMerkleValues(partialTrie, version, HashUtils::hashWithBlake2b);
        return partialTrie.getRootNode()
                .map(NodeHandle::getUserData)
                .map(NodeData::getMerkleValue)
                .orElseThrow();
    }
}