package com.limechain.runtime.hostapi;

import com.limechain.runtime.SharedMemory;
import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import com.limechain.runtime.version.StateVersion;
import com.limechain.storage.DeleteByPrefixResult;
import com.limechain.trie.DiskChildTrieAccessor;
import com.limechain.trie.TrieAccessor;
import com.limechain.trie.structure.nibble.Nibbles;
import com.limechain.trie.structure.nibble.NibblesUtils;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;
import org.wasmer.ImportObject;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.limechain.runtime.hostapi.PartialHostApi.newImportObjectPair;
import static com.limechain.runtime.hostapi.StorageHostFunctions.scaleEncodedOption;

/**
 * Implementations of the Child storage HostAPI functions
 * For more info check
 * {<a href="https://spec.polkadot.network/chap-host-api#sect-child-storage-api">Child Storage API</a>}
 */
@Log
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class ChildStorageHostFunctions implements PartialHostApi {
    private final SharedMemory sharedMemory;
    private final TrieAccessor trieAccessor;

    @Override
    public Map<Endpoint, ImportObject.FuncImport> getFunctionImports() {
        return Map.ofEntries(
            newImportObjectPair(Endpoint.ext_default_child_storage_set_version_1, argv -> {
                extDefaultChildStorageSetVersion1(
                    new RuntimePointerSize(argv.get(0)),
                    new RuntimePointerSize(argv.get(1)),
                    new RuntimePointerSize(argv.get(2)));
            }),
            newImportObjectPair(Endpoint.ext_default_child_storage_get_version_1, argv -> {
                return extDefaultChildStorageGetVersion1(
                    new RuntimePointerSize(argv.get(0)),
                    new RuntimePointerSize(argv.get(1))).pointerSize();
            }),
            newImportObjectPair(Endpoint.ext_default_child_storage_read_version_1, argv -> {
                return extDefaultChildStorageReadVersion1(
                    new RuntimePointerSize(argv.get(0)),
                    new RuntimePointerSize(argv.get(1)),
                    new RuntimePointerSize(argv.get(2)),
                    argv.get(2).intValue()
                ).pointerSize();
            }),
            newImportObjectPair(Endpoint.ext_default_child_storage_clear_version_1, argv -> {
                extDefaultChildStorageClearVersion1(
                    new RuntimePointerSize(argv.get(0)),
                    new RuntimePointerSize(argv.get(1)));
            }),
            newImportObjectPair(Endpoint.ext_default_child_storage_storage_kill_version_1, argv -> {
                extDefaultChildStorageKillVersion1(new RuntimePointerSize(argv.get(0)));
            }),
            newImportObjectPair(Endpoint.ext_default_child_storage_storage_kill_version_2, argv -> {
                return extDefaultChildStorageKillVersion2(
                    new RuntimePointerSize(argv.get(0)),
                    new RuntimePointerSize(argv.get(1))).size();
            }),
            newImportObjectPair(Endpoint.ext_default_child_storage_storage_kill_version_3, argv -> {
                return extDefaultChildStorageKillVersion3(
                    new RuntimePointerSize(argv.get(0)),
                    new RuntimePointerSize(argv.get(1))).pointerSize();
            }),
            newImportObjectPair(Endpoint.ext_default_child_storage_exists_version_1, argv -> {
                return extDefaultChildStorageExistsVersion1(
                    new RuntimePointerSize(argv.get(0)),
                    new RuntimePointerSize(argv.get(1)));
            }),
            newImportObjectPair(Endpoint.ext_default_child_storage_clear_prefix_version_1, argv -> {
                extDefaultChildStorageClearPrefixVersion1(
                    new RuntimePointerSize(argv.get(0)),
                    new RuntimePointerSize(argv.get(1)));
            }),
            newImportObjectPair(Endpoint.ext_default_child_storage_clear_prefix_version_2, argv -> {
                return extDefaultChildStorageClearPrefixVersion2(
                    new RuntimePointerSize(argv.get(0)),
                    new RuntimePointerSize(argv.get(1)),
                    new RuntimePointerSize(argv.get(2))
                ).pointerSize();
            }),
            newImportObjectPair(Endpoint.ext_default_child_storage_root_version_1, argv -> {
                return extDefaultChildStorageRoot(
                    new RuntimePointerSize(argv.get(0)),
                    null
                ).pointerSize();
            }),
            newImportObjectPair(Endpoint.ext_default_child_storage_root_version_2, argv -> {
                return extDefaultChildStorageRoot(
                    new RuntimePointerSize(argv.get(0)),
                    StateVersion.fromInt(argv.get(1).intValue())
                ).pointerSize();
            }),
            newImportObjectPair(Endpoint.ext_default_child_storage_next_key_version_1, argv -> {
                return extDefaultChildStorageStorageNextKeyVersion1(
                    new RuntimePointerSize(argv.get(0)),
                    new RuntimePointerSize(argv.get(1))
                ).pointerSize();
            })
        );
    }

    /**
     * Gets the given key from child storage, placing the value into a buffer and returning the number of bytes
     * that the entry in child storage has beyond the offset.
     *
     * @param childStorageKeyPointer a pointer-size containing the child storage key.
     * @param keyPointer             a pointer-size containing the key.
     * @param valueOutPointer        a pointer-size containing the buffer to which the value will be written to.
     *                               This function will never write more than the length of the buffer,
     *                               even if the value’s length is bigger.
     * @param offset                 the offset beyond the value that should be read from.
     * @return a pointer-size pointing to a SCALE encoded Option value containing an unsigned 32-bit integer
     * representing the number of bytes left at supplied offset. Returns None if the entry does not exist.
     */
    public RuntimePointerSize extDefaultChildStorageReadVersion1(RuntimePointerSize childStorageKeyPointer,
                                                                 RuntimePointerSize keyPointer,
                                                                 RuntimePointerSize valueOutPointer,
                                                                 int offset) {
        log.fine("extDefaultChildStorageReadVersion1");
        Nibbles childStorageKey = Nibbles.fromBytes(sharedMemory.readData(childStorageKeyPointer));
        Nibbles key = Nibbles.fromBytes(sharedMemory.readData(keyPointer));

        TrieAccessor childTrie = trieAccessor.getChildTrie(childStorageKey);

        byte[] value = childTrie.findStorageValue(key).orElse(null);

        if (value == null) {
            return sharedMemory.writeData(scaleEncodedOption(null));
        }

        int size = 0;
        if (offset <= value.length) {
            size = value.length - offset;
            sharedMemory.writeData(Arrays.copyOfRange(value, offset, value.length), valueOutPointer);
        }

        return sharedMemory.writeData(scaleEncodedOption(size));
    }

    /**
     * Sets the value under a given key into child storage.
     *
     * @param childStorageKeyPointer a pointer-size containing the child storage key.
     * @param keyPointer             a pointer-size containing the key.
     * @param valuePointer           a pointer-size containing the value.
     */
    public void extDefaultChildStorageSetVersion1(RuntimePointerSize childStorageKeyPointer,
                                                  RuntimePointerSize keyPointer,
                                                  RuntimePointerSize valuePointer) {
        log.fine("extDefaultChildStorageSetVersion1");

        Nibbles childStorageKey = Nibbles.fromBytes(sharedMemory.readData(childStorageKeyPointer));
        Nibbles key = Nibbles.fromBytes(sharedMemory.readData(keyPointer));
        byte[] value = sharedMemory.readData(valuePointer);

        TrieAccessor childTrie = trieAccessor.getChildTrie(childStorageKey);
        childTrie.upsertNode(key, value);
    }

    /**
     * Clears the child storage of the given key and its value. Non-existent entries are silently ignored.
     *
     * @param childStorageKeyPointer a pointer-size containing the child storage key.
     * @param keyPointer             a pointer-size containing the key.
     */
    public void extDefaultChildStorageClearVersion1(RuntimePointerSize childStorageKeyPointer,
                                                    RuntimePointerSize keyPointer) {
        log.fine("extDefaultChildStorageClearVersion1");

        Nibbles childStorageKey = Nibbles.fromBytes(sharedMemory.readData(childStorageKeyPointer));
        Nibbles key = Nibbles.fromBytes(sharedMemory.readData(keyPointer));

        TrieAccessor childTrie = trieAccessor.getChildTrie(childStorageKey);
        childTrie.deleteNode(key);
    }

    /**
     * Clear the child storage of each key/value pair where the key starts with the given prefix.
     *
     * @param childStorageKeyPointer a pointer-size containing the child storage key.
     * @param prefixPointer          a pointer-size containing the prefix.
     */
    public void extDefaultChildStorageClearPrefixVersion1(RuntimePointerSize childStorageKeyPointer,
                                                          RuntimePointerSize prefixPointer) {
        log.fine("extDefaultChildStorageClearPrefixVersion1");

        Nibbles childStorageKey = Nibbles.fromBytes(sharedMemory.readData(childStorageKeyPointer));
        Nibbles prefix = Nibbles.fromBytes(sharedMemory.readData(prefixPointer));

        TrieAccessor childTrie = trieAccessor.getChildTrie(childStorageKey);
        childTrie.deleteMultipleNodesByPrefix(prefix, null);
    }

    /**
     * Clear the child storage of each key/value pair where the key starts with the given prefix.
     *
     * @param childStorageKeyPointer a pointer-size containing the child storage key.
     * @param prefixPointer          a pointer-size containing the prefix.
     * @param limitPointer           a pointer-size to an Option type containing an unsigned 32-bit integer indicating the limit
     *                               on how many keys should be deleted. No limit is applied if this is None.
     *                               Any keys created during the current block execution do not count toward the limit.
     * @return a pointer-size to the following variant, k = 0 -> c | k = 1 -> c
     * where 0 indicates that all keys of the child storage have been removed, followed by the number of removed keys.
     * The variant 1 indicates that there are remaining keys, followed by the number of removed keys.
     */
    public RuntimePointerSize extDefaultChildStorageClearPrefixVersion2(RuntimePointerSize childStorageKeyPointer,
                                                                        RuntimePointerSize prefixPointer,
                                                                        RuntimePointerSize limitPointer) {
        log.fine("extDefaultChildStorageClearPrefixVersion2");

        Nibbles childStorageKey = Nibbles.fromBytes(sharedMemory.readData(childStorageKeyPointer));
        Nibbles prefix = Nibbles.fromBytes(sharedMemory.readData(prefixPointer));

        byte[] limitBytes = sharedMemory.readData(limitPointer);
        Long limit = new ScaleCodecReader(limitBytes).readOptional(ScaleCodecReader.UINT32).orElse(null);

        TrieAccessor childTrie = trieAccessor.getChildTrie(childStorageKey);
        DeleteByPrefixResult result = childTrie.deleteMultipleNodesByPrefix(prefix, limit);

        return sharedMemory.writeData(result.scaleEncoded());
    }

    /**
     * Checks whether the given key exists in child storage.
     *
     * @param childStorageKeyPointer a pointer-size containing the child storage key.
     * @param keyPointer             a pointer-size containing the key.
     * @return integer value equal to 1 if the key exists or a value equal to 0 if otherwise.
     */
    public int extDefaultChildStorageExistsVersion1(RuntimePointerSize childStorageKeyPointer,
                                                    RuntimePointerSize keyPointer) {
        log.fine("extDefaultChildStorageExistsVersion1");

        Nibbles childStorageKey = Nibbles.fromBytes(sharedMemory.readData(childStorageKeyPointer));
        Nibbles key = Nibbles.fromBytes(sharedMemory.readData(keyPointer));

        TrieAccessor childTrie = trieAccessor.getChildTrie(childStorageKey);
        return childTrie.findStorageValue(key).isPresent() ? 1 : 0;
    }

    /**
     * Retrieves the value associated with the given key from child storage.
     *
     * @param childStorageKeyPointer a pointer-size containing the child storage key.
     * @param keyPointer             a pointer-size containing the key
     * @return a pointer-size returning the SCALE encoded Option value containing the value.
     */
    public RuntimePointerSize extDefaultChildStorageGetVersion1(RuntimePointerSize childStorageKeyPointer,
                                                                RuntimePointerSize keyPointer) {
        log.fine("extDefaultChildStorageGetVersion1");

        Nibbles childStorageKey = Nibbles.fromBytes(sharedMemory.readData(childStorageKeyPointer));
        Nibbles key = Nibbles.fromBytes(sharedMemory.readData(keyPointer));

        TrieAccessor childTrie = trieAccessor.getChildTrie(childStorageKey);
        byte[] value = childTrie.findStorageValue(key).orElse(null);

        return sharedMemory.writeData(scaleEncodedOption(value));
    }

    /**
     * Get the next key in child storage after the given one in lexicographic order.
     * The key provided to this function may or may not exist in child storage.
     *
     * @param childStorageKeyPointer a pointer-size containing the child storage key.
     * @param keyPointer             a pointer-size to the key.
     * @return a pointer-size to the SCALE encoded Option value containing the next key in lexicographic order.
     */
    public RuntimePointerSize extDefaultChildStorageStorageNextKeyVersion1(RuntimePointerSize childStorageKeyPointer,
                                                                           RuntimePointerSize keyPointer) {
        log.fine("extDefaultChildStorageStorageNextKeyVersion1");

        Nibbles childStorageKey = Nibbles.fromBytes(sharedMemory.readData(childStorageKeyPointer));
        Nibbles key = Nibbles.fromBytes(sharedMemory.readData(keyPointer));

        TrieAccessor childTrie = trieAccessor.getChildTrie(childStorageKey);

        byte[] nextKey = childTrie.getNextKey(key)
            .map(NibblesUtils::toBytesAppending)
            .map(this::asByteArray)
            .orElse(null);

        return sharedMemory.writeData(scaleEncodedOption(nextKey));
    }


    private byte[] asByteArray(List<Byte> bytes) {
        byte[] result = new byte[bytes.size()];
        for (int i = 0; i < bytes.size(); i++) {
            result[i] = bytes.get(i);
        }
        return result;
    }

    /**
     * Compute the child storage root.
     *
     * @return a pointer-size to a buffer containing the 256-bit Blake2 child storage root.
     */
    public RuntimePointerSize extDefaultChildStorageRoot(RuntimePointerSize childStorageKeyPointer, StateVersion v0) {
        log.fine("extDefaultChildStorageRootVersion1");

        Nibbles childStorageKey = Nibbles.fromBytes(sharedMemory.readData(childStorageKeyPointer));
        TrieAccessor childTrie = trieAccessor.getChildTrie(childStorageKey);

        byte[] rootHash = childTrie.getMerkleRoot(v0);

        return sharedMemory.writeData(rootHash);
    }

    /**
     * Deletes the child storage
     *
     * @param childStorageKeyPointer a pointer-size containing the child storage key.
     */
    public void extDefaultChildStorageKillVersion1(RuntimePointerSize childStorageKeyPointer) {
        log.fine("extDefaultChildStorageKillVersion1");

        Nibbles childStorageKey = Nibbles.fromBytes(sharedMemory.readData(childStorageKeyPointer));
        DiskChildTrieAccessor childTrie = (DiskChildTrieAccessor) trieAccessor.getChildTrie(childStorageKey);
        trieAccessor.deleteNode(childTrie.getChildTrieKey());
    }

    /**
     * Deletes a limited amount of entries in the child storage
     *
     * @param childStorageKeyPointer a pointer-size containing the child storage key.
     * @param limitPointer           a pointer-size containing the limit.
     */
    public RuntimePointerSize extDefaultChildStorageKillVersion2(RuntimePointerSize childStorageKeyPointer,
                                                                 RuntimePointerSize limitPointer) {
        log.fine("extDefaultChildStorageKillVersion2");

        Nibbles childStorageKey = Nibbles.fromBytes(sharedMemory.readData(childStorageKeyPointer));

        byte[] limitBytes = sharedMemory.readData(limitPointer);
        Long limit = new ScaleCodecReader(limitBytes).readOptional(ScaleCodecReader.UINT32).orElse(null);

        TrieAccessor childTrie = trieAccessor.getChildTrie(childStorageKey);
        DeleteByPrefixResult result = childTrie.deleteMultipleNodesByPrefix(Nibbles.EMPTY, limit);

        return sharedMemory.writeData(result.scaleEncoded());
    }

    /**
     * Deletes a limited amount of entries in the child storage
     *
     * @param childStorageKeyPointer a pointer-size containing the child storage key.
     */
    public RuntimePointerSize extDefaultChildStorageKillVersion3(RuntimePointerSize childStorageKeyPointer,
                                                                 RuntimePointerSize limitPointer) {
        log.fine("extDefaultChildStorageKillVersion3");

        return extDefaultChildStorageKillVersion2(childStorageKeyPointer, limitPointer);
    }
}
