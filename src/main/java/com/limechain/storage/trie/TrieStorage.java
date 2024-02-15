package com.limechain.storage.trie;

import com.google.common.primitives.Bytes;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.runtime.version.StateVersion;
import com.limechain.storage.DeleteByPrefixResult;
import com.limechain.storage.KVRepository;
import com.limechain.storage.block.BlockState;
import com.limechain.trie.structure.TrieStructure;
import com.limechain.trie.structure.nibble.Nibble;
import com.limechain.trie.structure.nibble.Nibbles;
import com.limechain.trie.structure.node.InsertTrieNode;
import com.limechain.trie.structure.node.TrieNodeData;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.Getter;
import lombok.extern.java.Log;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

@Log
public class TrieStorage {

    private static final String TRIE_NODE_PREFIX = "tn:";
    @Getter
    private static TrieStorage instance;
    private final KVRepository<String, Object> db;
    private final BlockState blockState;

    public TrieStorage(final KVRepository<String, Object> db) {
        if (instance != null) {
            throw new IllegalStateException("TrieStorage is already initialized");
        }
        instance = this;
        this.db = db;
        this.blockState = BlockState.getInstance();
    }

    @NotNull
    private static byte[] partialKeyFromNibbles(List<Nibble> nibbles) {
        return Bytes.toArray(
                nibbles.stream()
                        .map(Nibble::asByte)
                        .toList());
    }

    /**
     * Inserts trie nodes into the key-value repository.
     *
     * @param insertTrieNodes The list of trie nodes to be inserted.
     * @param stateVersion    The version number of the trie entries.
     */
    public void insertStorage(final List<InsertTrieNode> insertTrieNodes,
                              final StateVersion stateVersion) {
        try {
            for (InsertTrieNode trieNode : insertTrieNodes) {
                insertTrieNodeStorage(db, trieNode, stateVersion);
            }

        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to insert trie structure to db storage", e);
        }
    }

    /**
     * Inserts trie node storage data into the key-value repository.
     * <p>
     * The storage data is represented by a {@link TrieNodeData} object, which is serialized and saved to the repository.
     *
     * @param db           The key-value repository where trie node storage is to be saved.
     * @param trieNode     The trie node whose storage data is to be inserted.
     * @param stateVersion The version of the state, affecting how the storage value is interpreted.
     */
    public void insertTrieNodeStorage(KVRepository<String, Object> db, InsertTrieNode trieNode,
                                      StateVersion stateVersion) {
        String key = TRIE_NODE_PREFIX + new String(trieNode.merkleValue());

        List<Nibble> nibbles = trieNode.partialKeyNibbles();
        byte[] partialKey = partialKeyFromNibbles(nibbles);

        List<byte[]> childrenMerkleValues = trieNode.childrenMerkleValues();

        TrieNodeData storageValue = new TrieNodeData(
                partialKey,
                childrenMerkleValues,
                trieNode.isReferenceValue() ? null : trieNode.storageValue(),
                trieNode.isReferenceValue() ? trieNode.storageValue() : null,
                (byte) stateVersion.asInt());

        db.save(key, storageValue);
    }

    /**
     * Retrieves a value by key from the trie associated with a specific block hash.
     *
     * @param blockHash The hash of the block whose trie is to be searched.
     * @param key       The key for which to retrieve the value.
     * @return An {@link Optional} containing the value associated with the key if found, or empty if not found.
     */
    public Optional<byte[]> getByKeyFromBlock(Hash256 blockHash, byte[] key) {
        BlockHeader header = blockState.getHeader(blockHash);

        if (header == null) {
            return Optional.empty();
        }

        Optional<Object> trieNodeData = db.find(TRIE_NODE_PREFIX + new String(header.getStateRoot().getBytes()));
        if (trieNodeData.isEmpty()) {
            return Optional.empty();
        }

        TrieNodeData trieNode = (TrieNodeData) trieNodeData.get();

        return Optional.ofNullable(
                getFromDb(trieNode, partialKeyFromNibbles(Nibbles.fromBytes(key).asUnmodifiableList())));
    }

    /**
     * Recursively searches for a value by key starting from a given trie node.
     * <p>
     * This method compares the provided key with the trie node's partial key. If they match,
     * it returns the node's value. Otherwise, it iterates through the node's children,
     * recursively searching for the value in both inline and referenced child nodes.
     * If a child node is referenced by a hash, the method fetches and decodes the child node
     * from the database before continuing the search.
     *
     * @param trieNode The trie node from which to start the search.
     * @param key      The key for which to search. (should be nibbles array)
     * @return The value associated with the key, or {@code null} if not found.
     * @throws RuntimeException If a referenced child node cannot be found in the database.
     */
    private byte[] getFromDb(TrieNodeData trieNode, byte[] key) {
        if (Arrays.equals(trieNode.getPartialKey(), key)) {
            return trieNode.getValue();
        }

        int i = 0;
        while (i < key.length && i < trieNode.getPartialKey().length && key[i] == trieNode.getPartialKey()[i]) {
            i++;
        }
        byte[] childMerkleValue = trieNode.getChildrenMerkleValues().get(key[i]);
        key = Arrays.copyOfRange(key, 1 + i, key.length);

        // Node is referenced by hash, fetch and decode
        Optional<Object> encodedChild = db.find(TRIE_NODE_PREFIX + new String(childMerkleValue));
        if (encodedChild.isEmpty()) {
            throw new RuntimeException(
                    "Child node not found in database for hash: " + Arrays.toString(childMerkleValue));
        }
        TrieNodeData childNode = (TrieNodeData) encodedChild.get();

        byte[] potentialValue = getFromDb(childNode, key);
        if (potentialValue != null) {
            return potentialValue;
        }

        return null;
    }

    public List<String> getNextKey(Hash256 blockHash, String prefix, long limit) {
        return null;
    }

    public DeleteByPrefixResult deleteByPrefixFromBlock(Hash256 blockHash, String prefix, long limit) {
        return null;
    }

    public boolean persistAllChanges(TrieStructure<byte[]> trie) {
        return false;
    }

}
