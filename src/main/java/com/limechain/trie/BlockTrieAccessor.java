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
import io.emeraldpay.polkaj.types.Hash256;
import org.apache.commons.lang3.NotImplementedException;

import java.util.List;
import java.util.Optional;

// TODO: change key type to Nibbles
public class BlockTrieAccessor implements KVRepository<String, byte[]> {
    public static final String TRANSACTIONS_NOT_SUPPORTED = "Block Trie Accessor does not support transactions.";
    private final Hash256 blockHash;
    private final TrieStructure<NodeData> partialTrie = new TrieStructure<>();
    private final TrieStorage trieStorage = TrieStorage.getInstance();

    public BlockTrieAccessor(Hash256 blockHash) {
        this.blockHash = blockHash;
    }

    public byte[] getMerkleRoot(StateVersion version) {
        //TODO: don't recalculate calculate child leaf values
        TrieStructureFactory.calculateMerkleValues(partialTrie, version);
        return partialTrie.getRootNode()
                .map(NodeHandle::getUserData)
                .map(NodeData::getMerkleValue)
                .orElseThrow();
    }
    @Override
    public boolean save(String key, byte[] value) {
        loadPathToKey(key);
        loadChildren(key);
        NodeData nodeData = new NodeData(value);
        partialTrie.insertNode(Nibbles.fromBytes(key.getBytes()), nodeData);
        return true;
    }

    /**
     * Load the path up to the given key, from the TrieStorage into our partial trie.
     *
     * @param key key
     */
    private void loadPathToKey(String key) {
        Entry<NodeData> closestNode = partialTrie.node(Nibbles.fromBytes(key.getBytes()));
        if (closestNode instanceof Vacant<NodeData> vacantNode) {
            Optional.of(vacantNode)
                    .map(Vacant::getClosestAncestorIndex)
                    .map(partialTrie::nodeHandleAtIndex)
                    .map(NodeHandle::getUserData)
                    .map(NodeData::getMerkleValue)
                    .map(closestMerkle -> trieStorage.entriesBetween(closestMerkle, key))
                    .ifPresent(entries -> entries.forEach(e -> partialTrie.insertNode(e.key(), e.nodeData())));
        }
    }

    private void loadChildren(String key) {
        Entry<NodeData> entry = partialTrie.node(Nibbles.fromBytes(key.getBytes()));
        if (entry instanceof NodeHandle<NodeData> nodeHandle) {
            Optional.of(nodeHandle)
                    .map(NodeHandle::getUserData)
                    .map(NodeData::getMerkleValue)
                    .map(trieStorage::loadChildren)
                    .ifPresent(entries -> entries.forEach(e -> partialTrie.insertNode(e.key(), e.nodeData())));
        }
    }

    @Override
    public Optional<byte[]> find(String key) {
        loadPathToKey(key);
        return partialTrie.existingNode(Nibbles.fromBytes(key.getBytes()))
                .map(NodeHandle::getUserData)
                .map(NodeData::getValue);
    }

    @Override
    public boolean delete(String key) {
        loadPathToKey(key);
        loadChildren(key);
        return partialTrie.removeNode(Nibbles.fromBytes(key.getBytes()));
    }

    @Override
    public List<byte[]> findKeysByPrefix(String prefixSeek, int limit) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public DeleteByPrefixResult deleteByPrefix(String prefix, Long limit) {
        loadPathToKey(prefix);

        Nibbles prefixKey = Nibbles.fromBytes(prefix.getBytes());
        NodeHandle<NodeData> parent = switch (partialTrie.node(prefixKey)) {
            case Vacant<NodeData> vacant -> Optional.ofNullable(vacant.getClosestAncestorIndex())
                    .map(partialTrie::nodeHandleAtIndex)
                    .orElse(null);
            case NodeHandle<NodeData> handle -> handle.getParent();
        };

        if (limit == null) {
            //TODO: detach prefix from tree
            return null;
        }

        return Optional.ofNullable(parent)
                .map(NodeHandle::getFullKey)
                .map(parentKey -> prefixIndexInParent(parentKey, prefixKey))
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

    private DeleteByPrefixResult lexicographicDelete(NodeHandle<NodeData> node, long limit) {
        loadChildren(node.getFullKey().toString());
        int deleted = 0;
        for (Nibble nibble : Nibbles.ALL) {
            if (limit <= deleted) {
                return new DeleteByPrefixResult(deleted, false);
            }
            NodeHandle<NodeData> child = node.getChild(nibble).orElse(null);
            if (child != null) {
                DeleteByPrefixResult childResult = lexicographicDelete(child, limit - deleted);
                deleted += childResult.deleted();
                if(!childResult.all()) {
                    return new DeleteByPrefixResult(deleted, false);
                }
            }
        }
        if (partialTrie.removeNode(node.getNodeIndex().getValue())) {
            deleted++;
        }
        return new DeleteByPrefixResult(deleted, true);
    }


    @Override
    public Optional<String> getNextKey(String key) {
        loadPathToKey(key);
        //TODO: optimize using partial trie
        return Optional.ofNullable(trieStorage.getNextKey(blockHash, key));
    }

    @Override
    public void startTransaction() {
        throw new NotImplementedException(TRANSACTIONS_NOT_SUPPORTED);

    }

    @Override
    public void rollbackTransaction() {
        throw new NotImplementedException(TRANSACTIONS_NOT_SUPPORTED);

    }

    @Override
    public void commitTransaction() {
        throw new NotImplementedException(TRANSACTIONS_NOT_SUPPORTED);

    }

    @Override
    public void closeConnection() {
        throw new NotImplementedException(TRANSACTIONS_NOT_SUPPORTED);
    }

    public void persist() {
    }
}
