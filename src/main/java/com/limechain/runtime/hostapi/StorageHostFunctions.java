package com.limechain.runtime.hostapi;

import com.limechain.runtime.version.StateVersion;
import com.limechain.trie.AccessorHolder;
import com.limechain.trie.BlockTrieAccessor;
import com.limechain.trie.structure.nibble.Nibbles;
import com.limechain.trie.structure.nibble.NibblesUtils;
import com.limechain.utils.scale.exceptions.ScaleEncodingException;
import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import com.limechain.runtime.Runtime;
import com.limechain.storage.DeleteByPrefixResult;
import io.emeraldpay.polkaj.scale.CompactMode;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.lang.Nullable;
import org.wasmer.ImportObject;
import org.wasmer.Type;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Implementations of the Storage HostAPI functions
 * For more info check
 * {<a href="https://spec.polkadot.network/chap-host-api#sect-storage-api">Storage API</a>}
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class StorageHostFunctions {
    private final Runtime runtime;
    private final BlockTrieAccessor repository;

    private StorageHostFunctions(Runtime runtime) {
        this.runtime = runtime;
        this.repository = AccessorHolder.getInstance().getBlockTrieAccessor();
    }

    public static List<ImportObject> getFunctions(Runtime runtime) {
        return new StorageHostFunctions(runtime).buildFunctions();
    }

    public List<ImportObject> buildFunctions() {
        return Arrays.asList(
                HostApi.getImportObject("ext_storage_set_version_1", argv ->
                                extStorageSetVersion1(
                                        new RuntimePointerSize(argv.get(0)),
                                        new RuntimePointerSize(argv.get(1))),
                        List.of(Type.I64, Type.I64)),
                HostApi.getImportObject("ext_storage_get_version_1", argv ->
                                extStorageGetVersion1(new RuntimePointerSize(argv.get(0))).pointerSize(),
                        List.of(Type.I64), Type.I64),
                HostApi.getImportObject("ext_storage_read_version_1", argv ->
                                extStorageReadVersion1(
                                        new RuntimePointerSize(argv.get(0)), new RuntimePointerSize(argv.get(1)),
                                        argv.get(2).intValue()).pointerSize(),
                        List.of(Type.I64, Type.I64, Type.I32), Type.I64),
                HostApi.getImportObject("ext_storage_clear_version_1", argv ->
                        extStorageClearVersion1(new RuntimePointerSize(argv.get(0))),
                        List.of(Type.I64)),
                HostApi.getImportObject("ext_storage_exists_version_1", argv ->
                        extStorageExistsVersion1(new RuntimePointerSize(argv.get(0))),
                        List.of(Type.I64), Type.I32),
                HostApi.getImportObject("ext_storage_clear_prefix_version_1", argv ->
                        extStorageClearPrefixVersion1(new RuntimePointerSize(argv.get(0))),
                        List.of(Type.I64)),
                HostApi.getImportObject("ext_storage_clear_prefix_version_2", argv ->
                        extStorageClearPrefixVersion2(
                                new RuntimePointerSize(argv.get(0)), new RuntimePointerSize(argv.get(1))).pointerSize()
                        , List.of(Type.I64, Type.I64), Type.I64),
                HostApi.getImportObject("ext_storage_append_version_1", argv ->
                        extStorageAppendVersion1(
                                new RuntimePointerSize(argv.get(0)), new RuntimePointerSize(argv.get(1))
                        ), List.of(Type.I64, Type.I64)),
                HostApi.getImportObject("ext_storage_root_version_1", argv ->
                                extStorageRootVersion1().pointerSize(),
                        HostApi.EMPTY_LIST_OF_TYPES, Type.I64),
                HostApi.getImportObject("ext_storage_root_version_2", argv ->
                                extStorageRootVersion2(argv.get(0).intValue()).pointerSize(),
                        List.of(Type.I32), Type.I64),
                HostApi.getImportObject("ext_storage_changes_root_version_1", argv ->
                                extStorageChangesRootVersion1(new RuntimePointerSize(argv.get(0))).pointerSize()
                        , List.of(Type.I64), Type.I64),
                HostApi.getImportObject("ext_storage_next_key_version_1", argv ->
                                extStorageNextKeyVersion1(new RuntimePointerSize(argv.get(0))).pointerSize(),
                        List.of(Type.I64), Type.I64),
                HostApi.getImportObject("ext_storage_start_transaction_version_1", argv ->
                        extStorageStartTransactionVersion1(), HostApi.EMPTY_LIST_OF_TYPES),
                HostApi.getImportObject("ext_storage_rollback_transaction_version_1", argv ->
                        extStorageRollbackTransactionVersion1(), HostApi.EMPTY_LIST_OF_TYPES),
                HostApi.getImportObject("ext_storage_commit_transaction_version_1", argv ->
                        extStorageCommitTransactionVersion1(), HostApi.EMPTY_LIST_OF_TYPES));
    }

    /**
     * Sets the value under a given key into storage.
     *
     * @param keyPointer a pointer-size containing the key.
     * @param valuePointer a pointer-size containing the key.
     */
    public void extStorageSetVersion1(RuntimePointerSize keyPointer, RuntimePointerSize valuePointer) {
        Nibbles key = Nibbles.fromBytes(runtime.getDataFromMemory(keyPointer));
        byte[] value = runtime.getDataFromMemory(valuePointer);

        repository.save(key, value);
    }

    /**
     * Retrieves the value associated with the given key from storage.
     *
     * @param keyPointer a pointer-size containing the key
     * @return a pointer-size returning the SCALE encoded Option value containing the value.
     */
    public RuntimePointerSize extStorageGetVersion1(RuntimePointerSize keyPointer) {
        Nibbles key = Nibbles.fromBytes(runtime.getDataFromMemory(keyPointer));
        byte[] value = repository.find(key).orElse(null);

        return runtime.writeDataToMemory(scaleEncodedOption(value));
    }

    /**
     * Gets the given key from storage, placing the value into a buffer and returning the number of bytes
     * that the entry in storage has beyond the offset.
     *
     * @param keyPointer a pointer-size containing the key.
     * @param valueOutPointer a pointer-size containing the buffer to which the value will be written to.
     *                        This function will never write more than the length of the buffer,
     *                        even if the value’s length is bigger.
     * @param offset the offset beyond the value that should be read from.
     * @return a pointer-size pointing to a SCALE encoded Option value containing an unsigned 32-bit integer
     * representing the number of bytes left at supplied offset. Returns None if the entry does not exist.
     */
    public RuntimePointerSize extStorageReadVersion1(RuntimePointerSize keyPointer, RuntimePointerSize valueOutPointer,
                                                     int offset) {
        Nibbles key = Nibbles.fromBytes(runtime.getDataFromMemory(keyPointer));
        byte[] value = repository.find(key).orElse(null);

        if (value == null) {
            return runtime.writeDataToMemory(scaleEncodedOption(null));
        }

        int size = 0;
        if (offset <= value.length) {
            size = value.length - offset;
            runtime.writeDataToMemory(Arrays.copyOfRange(value, offset, value.length), valueOutPointer);
        }

        return runtime.writeDataToMemory(scaleEncodedOption(size));
    }

    /**
     * Clears the storage of the given key and its value. Non-existent entries are silently ignored.
     *
     * @param keyPointer a pointer-size containing the key.
     */
    public void extStorageClearVersion1(RuntimePointerSize keyPointer) {
        Nibbles key = Nibbles.fromBytes(runtime.getDataFromMemory(keyPointer));
        repository.delete(key);
    }

    /**
     * Checks whether the given key exists in storage.
     *
     * @param keyPointer a pointer-size containing the key.
     * @return integer value equal to 1 if the key exists or a value equal to 0 if otherwise.
     */
    public int extStorageExistsVersion1(RuntimePointerSize keyPointer) {
        Nibbles key = Nibbles.fromBytes(runtime.getDataFromMemory(keyPointer));
        return repository.find(key).isPresent() ? 1 : 0;
    }

    /**
     * Clear the storage of each key/value pair where the key starts with the given prefix.
     *
     * @param prefixPointer a pointer-size containing the prefix.
     */
    public void extStorageClearPrefixVersion1(RuntimePointerSize prefixPointer) {
        Nibbles prefix = Nibbles.fromBytes(runtime.getDataFromMemory(prefixPointer));
        repository.deleteByPrefix(prefix, null);
    }

    /**
     * Clear the storage of each key/value pair where the key starts with the given prefix.
     *
     * @param prefixPointer a pointer-size containing the prefix.
     * @param limitPointer a pointer-size to an Option type containing an unsigned 32-bit integer indicating the limit
     *                     on how many keys should be deleted. No limit is applied if this is None.
     *                     Any keys created during the current block execution do not count toward the limit.
     * @return a pointer-size to the following variant, k = 0 -> c | k = 1 -> c
     * where 0 indicates that all keys of the child storage have been removed, followed by the number of removed keys.
     * The variant 1 indicates that there are remaining keys, followed by the number of removed keys.
     */
    public RuntimePointerSize extStorageClearPrefixVersion2(RuntimePointerSize prefixPointer,
                                                            RuntimePointerSize limitPointer) {
        Nibbles prefix = Nibbles.fromBytes(runtime.getDataFromMemory(prefixPointer));

        byte[] limitBytes = runtime.getDataFromMemory(limitPointer);
        Long limit = new ScaleCodecReader(limitBytes).readOptional(ScaleCodecReader.UINT32).orElse(null);

        DeleteByPrefixResult result = repository.deleteByPrefix(prefix, limit);

        return runtime.writeDataToMemory(result.scaleEncoded());
    }

    /**
     * Append the SCALE encoded value to a SCALE encoded sequence at the given key.
     * This function assumes that the existing storage item is either empty or a SCALE-encoded sequence and that
     * the value to append is also SCALE encoded and of the same type as the items in the existing sequence.
     *
     * @param keyPointer a pointer-size containing the key.
     * @param valuePointer a pointer-size containing the value to be appended.
     */
    public void extStorageAppendVersion1(RuntimePointerSize keyPointer, RuntimePointerSize valuePointer) {
        Nibbles key = Nibbles.fromBytes(runtime.getDataFromMemory(keyPointer));
        byte[] sequence = repository.find(key).orElse(null);
        byte[] valueToAppend = runtime.getDataFromMemory(valuePointer);

        if (sequence == null) {
            repository.save(key, valueToAppend);
            return;
        }

        int numberOfItems;
        try {
            numberOfItems = new ScaleCodecReader(sequence).readCompactInt();
        } catch (IndexOutOfBoundsException e) {
            repository.save(key, valueToAppend);
            return;
        }

        int numberOfScaleLengthBytes = switch (CompactMode.forNumber(numberOfItems)) {
            case SINGLE -> 1;
            case TWO -> 2;
            default -> 4;
        };

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (ScaleCodecWriter writer = new ScaleCodecWriter(buf)) {
            writer.writeCompact(numberOfItems + 1);
            writer.writeByteArray(Arrays.copyOfRange(sequence, numberOfScaleLengthBytes, sequence.length));
            writer.writeByteArray(valueToAppend);
        } catch (IOException e) {
            throw new ScaleEncodingException(e);
        }
        repository.save(key, buf.toByteArray());
    }

    /**
     * Compute the storage root.
     *
     * @return a pointer-size to a buffer containing the 256-bit Blake2 storage root.
     */
    public RuntimePointerSize extStorageRootVersion1() {
        byte[] rootHash = repository.getMerkleRoot(StateVersion.V0);

        return runtime.writeDataToMemory(rootHash);
    }

    /**
     * Compute the storage root.
     *
     * @param version the state version
     * @return a pointer-size to a buffer containing the 256-bit Blake2 storage root.
     */
    public RuntimePointerSize extStorageRootVersion2(int version) {
        byte[] rootHash = repository.getMerkleRoot(StateVersion.fromInt(version));

        return runtime.writeDataToMemory(rootHash);
    }

    /**
     * This function is no longer used and only exists for compatibility reasons.
     *
     * @param parentHashPointer a pointer-size to the SCALE encoded block hash.
     * @return a pointer-size to an Option type (Definition 185) that’s always None.
     */
    public RuntimePointerSize extStorageChangesRootVersion1(RuntimePointerSize parentHashPointer) {
        return runtime.writeDataToMemory(scaleEncodedOption(null));
    }

    /**
     * Get the next key in storage after the given one in lexicographic order.
     * The key provided to this function may or may not exist in storage.
     *
     * @param keyPointer a pointer-size to the key.
     * @return a pointer-size to the SCALE encoded Option value containing the next key in lexicographic order.
     */
    public RuntimePointerSize extStorageNextKeyVersion1(RuntimePointerSize keyPointer) {
        Nibbles key = Nibbles.fromBytes(runtime.getDataFromMemory(keyPointer));
        byte[] nextKey = repository.getNextKey(key)
                .map(NibblesUtils::toBytesAppending)
                .map(this::asByteArray)
                .orElse(null);

        return runtime.writeDataToMemory(scaleEncodedOption(nextKey));
    }

    private byte[] asByteArray(List<Byte> bytes) {
        byte[] result = new byte[bytes.size()];
        for (int i = 0; i < bytes.size(); i++) {
            result[i] = bytes.get(i);
        }
        return result;
    }

    /**
     * Start a new nested transaction. This allows to either commit or roll back all changes that are made after this
     * call. For every transaction, there must be a matching call to either ext_storage_rollback_transaction or
     * ext_storage_commit_transaction. This is also effective for all values manipulated using the child storage API.
     * It’s legal to call this function multiple times in a row.
     */
    public void extStorageStartTransactionVersion1() {
        repository.startTransaction();
    }

    /**
     * Rollback the last transaction started by ext_storage_start_transaction.
     * Any changes made during that transaction are discarded. It’s legal to call this function multiple times in a row.
     */
    public void extStorageRollbackTransactionVersion1() {
        repository.rollbackTransaction();
    }

    /**
     * Commit the last transaction started by ext_storage_start_transaction.
     * Any changes made during that transaction are committed to the main state.
     * It’s legal to call this function multiple times in a row.
     */
    public void extStorageCommitTransactionVersion1() {
        repository.commitTransaction();
    }

    public static byte[] scaleEncodedOption(@Nullable int data) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (ScaleCodecWriter writer = new ScaleCodecWriter(buf)) {
            writer.writeOptional(ScaleCodecWriter::writeUint32, data);
        } catch (IOException e) {
            throw new ScaleEncodingException(e);
        }
        return buf.toByteArray();
    }

    public static byte[] scaleEncodedOption(@Nullable byte[] data) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (ScaleCodecWriter writer = new ScaleCodecWriter(buf)) {
            writer.writeOptional(ScaleCodecWriter::writeByteArray, data);
        } catch (IOException e) {
            throw new ScaleEncodingException(e);
        }
        return buf.toByteArray();
    }
}
