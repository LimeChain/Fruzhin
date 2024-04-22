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
import java.util.Optional;

@AllArgsConstructor
public class TrieAccessor implements KVRepository<Nibbles, byte[]> {

    public static final String TRANSACTIONS_NOT_SUPPORTED = "Block Trie Accessor does not support transactions.";
    private final Map<Nibbles, ChildTrieAccessor> loadedChildTries;
    protected final TrieStorage trieStorage;
    protected byte[] lastRoot;
    private TrieStructure<NodeData> initialTrie;

    public TrieAccessor(byte[] trieRoot) {
        this.lastRoot = trieRoot;
        this.loadedChildTries = new HashMap<>();
        this.trieStorage = TrieStorage.getInstance();
        this.initialTrie = AppBean.getBean(GenesisBlockHash.class).getInitialSyncTrie();
    }

    public ChildTrieAccessor getChildTrie(Nibbles key) {
        Nibbles trieKey = Nibbles.fromBytes(":child_storage:default:".getBytes()).addAll(key);
        byte[] merkleRoot = find(trieKey).orElse(null);
        return loadedChildTries.computeIfAbsent(trieKey, k -> new ChildTrieAccessor(this, trieKey, merkleRoot));
    }

    @Override
    public boolean save(Nibbles key, byte[] value) {
        NodeData nodeData = new NodeData(value);
        initialTrie.insertNode(key, nodeData);
        return true;
    }


    @Override
    public Optional<byte[]> find(Nibbles key) {
        return initialTrie.existingNode(key)
                .map(NodeHandle::getUserData)
                .map(NodeData::getValue);
    }

    public Optional<byte[]> findMerkleValue(Nibbles key) {
        return trieStorage.getByKeyFromMerkle(lastRoot, key)
                .map(NodeData::getMerkleValue);
    }

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

//        trieDiff.diffInsertErase(node.getFullKey());
        return new DeleteByPrefixResult(deleted, true);
    }

    @Override
    public Optional<Nibbles> getNextKey(Nibbles key) {
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
        clearMerkleValues(initialTrie);
        TrieStructureFactory.recalculateMerkleValues(initialTrie, version, HashUtils::hashWithBlake2b);

        return initialTrie.getRootNode()
                .map(NodeHandle::getUserData)
                .map(NodeData::getMerkleValue)
                .orElseThrow();
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
