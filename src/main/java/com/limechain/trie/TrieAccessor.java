package com.limechain.trie;

import com.limechain.constants.GenesisBlockHash;
import com.limechain.rpc.server.AppBean;
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
import java.util.Objects;
import java.util.Optional;

@AllArgsConstructor
public class TrieAccessor implements KVRepository<Nibbles, byte[]> {

    public static final String TRANSACTIONS_NOT_SUPPORTED = "Block Trie Accessor does not support transactions.";
    private final Map<Nibbles, ChildTrieAccessor> loadedChildTries;
    protected final TrieStructure<NodeData> partialTrie;
    protected final TrieStorage trieStorage;
    private final TrieDiff trieDiff;
    protected byte[] lastRoot;

    public TrieAccessor(byte[] trieRoot) {
        this.lastRoot = trieRoot;
        this.loadedChildTries = new HashMap<>();
        this.trieStorage = TrieStorage.getInstance();
        this.partialTrie = new TrieStructure<>();
        this.trieDiff = new TrieDiff();
    }

    public ChildTrieAccessor getChildTrie(Nibbles key) {
        Nibbles trieKey = Nibbles.fromBytes(":child_storage:default:".getBytes()).addAll(key);
        byte[] merkleRoot = find(trieKey).orElse(null);
        return loadedChildTries.computeIfAbsent(trieKey, k -> new ChildTrieAccessor(this, trieKey, merkleRoot));
    }

    @Override
    public boolean save(Nibbles key, byte[] value) {
        loadPathToKey(key);
        loadChildren(key);
        NodeData nodeData = new NodeData(value);
        trieDiff.diffInsert(key, nodeData);
        return true;
    }

    //    /**
//     * Load the path up to the given key, from the TrieStorage into our partial trie.
//     *
//     * @param key key
//     */
    private void loadPathToKey(Nibbles key) {
        Entry<NodeData> closestNode = partialTrie.node(key);
        if (closestNode instanceof Vacant<NodeData>) {
            Optional.of(lastRoot)
                    .map(closestMerkle -> trieStorage.entriesBetween(closestMerkle, key))
                    .ifPresent(entries ->
                            entries.forEach(
                                    storageNode -> {
                                        if (Boolean.TRUE.equals(!trieDiff.isDeleted(storageNode.key()))) {
                                            partialTrie.insertNode(storageNode.key(), storageNode.nodeData());
                                        }
                                    })
                    );
        }
    }

    private void loadChildren(Nibbles key) {
        findMerkleValue(key)
                .map(merkle -> trieStorage.loadChildren(key, merkle))
                .ifPresent(entries ->
                        entries.stream()
                                .filter(Objects::nonNull)
                                .forEach(storageNode -> {
                                    if (Boolean.TRUE.equals(!trieDiff.isDeleted(storageNode.key())))
                                        partialTrie.insertNode(storageNode.key(), storageNode.nodeData());
                                }));
    }

    @Override
    public Optional<byte[]> find(Nibbles key) {
        if (trieDiff.isAvailable(key)) {
            return Optional.ofNullable(trieDiff.get(key).getValue());
        }
        loadPathToKey(key);
        loadChildren(key);
        return partialTrie.existingNode(key)
                .map(NodeHandle::getUserData)
                .map(NodeData::getValue);
    }

    public Optional<byte[]> findMerkleValue(Nibbles key) {
        return trieStorage.getByKeyFromMerkle(lastRoot, key)
                .map(NodeData::getMerkleValue);
    }

    @Override
    public boolean delete(Nibbles key) {
        trieDiff.diffInsertErase(key);
        return true;
    }

    @Override
    public List<byte[]> findKeysByPrefix(Nibbles prefixSeek, int limit) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public DeleteByPrefixResult deleteByPrefix(Nibbles prefix, Long limit) {
//        loadPathToKey(prefix);

//        NodeHandle<NodeData> parent = switch (partialTrie.node(prefix)) {
//            case Vacant<NodeData> vacant -> Optional.ofNullable(vacant.getClosestAncestorIndex())
//                    .map(partialTrie::nodeHandleAtIndex)
//                    .orElse(null);
//            case NodeHandle<NodeData> handle -> handle.getParent();
//        };

        DeleteByPrefixResult other = new DeleteByPrefixResult(0, true);
        return other;
//        return Optional.ofNullable(parent)
//                .map(NodeHandle::getFullKey)
//                .map(parentKey -> prefixIndexInParent(parentKey, prefix))
//                .flatMap(parent::getChild)
//                .map(c -> lexicographicDelete(c, limit))
//                .orElse(other);
    }

    private Nibble prefixIndexInParent(Nibbles parent, Nibbles prefix) {
        for (int i = 0; i < prefix.size(); i++) {
            if (prefix.get(i).equals(parent.get(i))) {
                return parent.get(i);
            }
        }
        return null;
    }

    private DeleteByPrefixResult lexicographicDelete(NodeHandle<NodeData> node, Long limit) {
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

        trieDiff.diffInsertErase(node.getFullKey());
        return new DeleteByPrefixResult(deleted, true);
    }

    public void persistAll() {
        for (ChildTrieAccessor value : loadedChildTries.values()) value.persistAll();
        loadedChildTries.clear();

        if (partialTrie.isEmpty()) return;
        lastRoot = getMerkleRoot(StateVersion.V0);
        trieStorage.insertTrieStorage(partialTrie, StateVersion.V0);
    }

    @Override
    public Optional<Nibbles> getNextKey(Nibbles key) {
        loadPathToKey(key);
        return Optional.ofNullable(trieStorage.getNextKeyByMerkleValue(lastRoot, key));
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
        TrieStructure<NodeData> testTrie = AppBean.getBean(GenesisBlockHash.class).getTestTrie();

        for (Map.Entry<Nibbles, NodeData> entry : trieDiff.diffIterUnordered().entrySet()) {
            Nibbles key = entry.getKey();
            if (trieDiff.isDeleted(key)) {
                Entry<NodeData> node = testTrie.node(key);
                if (!(node instanceof Vacant<NodeData>)) {
                    NodeHandle<NodeData> nodeHandle = node.asNodeHandle();
                    testTrie.clearNodeValue(nodeHandle.getNodeIndex());
                }
            } else {
                NodeData nodeData = new NodeData(entry.getValue().getValue());
                testTrie.insertNode(key, nodeData);
            }
        }

        clearMerkleValues(testTrie);
        TrieStructureFactory.recalculateMerkleValues(testTrie, version, HashUtils::hashWithBlake2b);

        byte[] bytes = testTrie.getRootNode()
                .map(NodeHandle::getUserData)
                .map(NodeData::getMerkleValue)
                .orElseThrow();
        return bytes;
    }

    private void clearMerkleValues(TrieStructure<NodeData> testTrie) {
        testTrie.iteratorUnordered().forEachRemaining(nodeIndex -> {
            NodeData userData = testTrie.nodeHandleAtIndex(nodeIndex).getUserData();
            if (userData != null) {
                userData.setMerkleValue(null);
            }
        });
    }

}
