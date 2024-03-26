package com.limechain.rpc.methods.childstate;

import com.limechain.storage.block.BlockState;
import com.limechain.storage.trie.TrieStorage;
import com.limechain.trie.structure.database.NodeData;
import com.limechain.utils.StringUtils;
import io.emeraldpay.polkaj.types.Hash256;
import org.springframework.stereotype.Service;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.List;

@Service
public class ChildStateRPCImpl {

    private final TrieStorage trieStorage = TrieStorage.getInstance();
    private final BlockState blockState = BlockState.getInstance();

    /**
     * Retrieves a list of storage keys that match a given prefix within a specific child storage and block.
     *
     * @param prefixHex    The prefix (in hexadecimal format) to match the keys against.
     * @param childKeyHex  The hexadecimal representation of the child storage key.
     * @param blockHashHex The block hash (in hexadecimal format) to scope the search.
     * @return A list of matching keys in hexadecimal format. Returns an empty list if the block state is not initialized.
     */
    public List<String> childStateGetKeys(String prefixHex, String childKeyHex, String blockHashHex) {
        if (!this.blockState.isInitialized()) {
            return Collections.emptyList();
        }

        byte[] prefix = StringUtils.hexToBytes(prefixHex);
        byte[] childStorageMerkle = getChildStorageMerkle(childKeyHex, blockHashHex);

        return trieStorage
                .getKeysWithPrefix(childStorageMerkle, prefix)
                .stream().map(StringUtils::toHexWithPrefix)
                .toList();
    }

    /**
     * Retrieves the storage value for a specific key within a child storage and block.
     *
     * @param childKeyHex  The hexadecimal representation of the child storage key.
     * @param keyHex       The key (in hexadecimal format) for which to retrieve the storage value.
     * @param blockHashHex The block hash (in hexadecimal format) to scope the search.
     * @return The storage value in hexadecimal format,
     * or {@code null} if the block state is not initialized or the value does not exist.
     */
    public String stateGetStorage(String childKeyHex, String keyHex, String blockHashHex) {
        if (!this.blockState.isInitialized()) {
            return null;
        }
        byte[] key = StringUtils.hexToBytes(keyHex);
        byte[] childStorageMerkle = getChildStorageMerkle(childKeyHex, blockHashHex);

        return trieStorage.getByKeyFromMerkle(childStorageMerkle, new String(key))
                .map(NodeData::getValue)
                .map(StringUtils::toHexWithPrefix)
                .orElse(null);
    }

    /**
     * Retrieves the storage hash for a specific key within a child storage and block.
     *
     * @param childKeyHex The hexadecimal representation of the child storage key.
     * @param keyHex The key (in hexadecimal format) for which to retrieve the storage hash.
     * @param blockHashHex The block hash (in hexadecimal format) to scope the search.
     * @return The storage hash in hexadecimal format, or {@code null} if the block state is not initialized or the hash does not exist.
     */
    public String stateGetStorageHash(String childKeyHex, String keyHex, String blockHashHex) {
        if (!this.blockState.isInitialized()) {
            return null;
        }

        byte[] key = StringUtils.hexToBytes(keyHex);
        byte[] childStorageMerkle = getChildStorageMerkle(childKeyHex, blockHashHex);

        return trieStorage.getByKeyFromMerkle(childStorageMerkle, new String(key))
                .map(NodeData::getMerkleValue)
                .map(StringUtils::toHexWithPrefix)
                .orElse(null);
    }

    /**
     * Retrieves the storage size for a specific key within a child storage and block.
     *
     * @param childKeyHex The hexadecimal representation of the child storage key.
     * @param keyHex The key (in hexadecimal format) for which to retrieve the storage size.
     * @param blockHashHex The block hash (in hexadecimal format) to scope the search.
     * @return The storage size as a string, or {@code null} if the block state is not initialized or the key does not exist.
     */
    public String stateGetStorageSize(String childKeyHex, String keyHex, String blockHashHex) {
        if (!this.blockState.isInitialized()) {
            return null;
        }

        byte[] key = StringUtils.hexToBytes(keyHex);
        byte[] childStorageMerkle = getChildStorageMerkle(childKeyHex, blockHashHex);

        return trieStorage
                .getByKeyFromMerkle(childStorageMerkle, new String(key))
                .map(NodeData::getValue)
                .map(Array::getLength)
                .map(String::valueOf)
                .orElse(null);
    }

    private Hash256 getHash256FromHex(String blockHashHex) {
        return blockHashHex != null ? Hash256.from(blockHashHex) : blockState.getHighestFinalizedHash();
    }

    private byte[] getChildStorageMerkle(String childKeyHex, String blockHashHex) {
        byte[] childKey = StringUtils.hexToBytes(childKeyHex);
        final Hash256 blockHash = getHash256FromHex(blockHashHex);

        return getChildMerkle(blockHash, childKey);
    }

    private byte[] getChildMerkle(Hash256 blockHash, byte[] childKey) {
        return trieStorage
                .getByKeyFromBlock(blockHash, ":child_storage:default:" + new String(childKey))
                .map(NodeData::getValue)
                .orElse(null);
    }
}
