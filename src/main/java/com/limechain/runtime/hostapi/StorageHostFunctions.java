package com.limechain.runtime.hostapi;

import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import com.limechain.storage.DBConstants;
import com.limechain.storage.DeleteByPrefixResult;
import com.limechain.storage.KVRepository;
import com.limechain.sync.warpsync.SyncedState;
import io.emeraldpay.polkaj.scale.CompactMode;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
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
@AllArgsConstructor
public class StorageHostFunctions {
    private final HostApi hostApi;
    private final KVRepository<String, Object> repository;

    public StorageHostFunctions() {
        this.hostApi = HostApi.getInstance();
        this.repository = SyncedState.getInstance().getRepository();
    }

    public static List<ImportObject> getFunctions() {
        return new StorageHostFunctions().buildFunctions();
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
        byte[] key = hostApi.getDataFromMemory(keyPointer);
        byte[] value = hostApi.getDataFromMemory(valuePointer);

        repository.save(new String(key), value);
    }

    /**
     * Retrieves the value associated with the given key from storage.
     *
     * @param keyPointer a pointer-size containing the key
     * @return a pointer-size returning the SCALE encoded Option value containing the value.
     */
    public RuntimePointerSize extStorageGetVersion1(RuntimePointerSize keyPointer) {
        byte[] key = hostApi.getDataFromMemory(keyPointer);
        byte[] value = (byte[]) repository.find(new String(key)).orElse(null);

        return hostApi.writeDataToMemory(scaleEncodedOption(value));
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
        byte[] key = hostApi.getDataFromMemory(keyPointer);
        byte[] value = (byte[]) repository.find(new String(key)).orElse(null);

        if (value == null) {
            return hostApi.writeDataToMemory(scaleEncodedOption(null));
        }

        int size = 0;
        if (offset <= value.length) {
            size = value.length - offset;
            hostApi.writeDataToMemory(Arrays.copyOfRange(value, offset, value.length), valueOutPointer);
        }

        return hostApi.writeDataToMemory(scaleEncodedOption(size));
    }

    /**
     * Clears the storage of the given key and its value. Non-existent entries are silently ignored.
     *
     * @param keyPointer a pointer-size containing the key.
     */
    public void extStorageClearVersion1(RuntimePointerSize keyPointer) {
        byte[] key = hostApi.getDataFromMemory(keyPointer);
        repository.delete(new String(key));
    }

    /**
     * Checks whether the given key exists in storage.
     *
     * @param keyPointer a pointer-size containing the key.
     * @return integer value equal to 1 if the key exists or a value equal to 0 if otherwise.
     */
    public int extStorageExistsVersion1(RuntimePointerSize keyPointer) {
        byte[] key = hostApi.getDataFromMemory(keyPointer);
        return repository.find(new String(key)).isPresent() ? 1 : 0;
    }

    /**
     * Clear the storage of each key/value pair where the key starts with the given prefix.
     *
     * @param prefixPointer a pointer-size containing the prefix.
     */
    public void extStorageClearPrefixVersion1(RuntimePointerSize prefixPointer) {
        byte[] prefix = hostApi.getDataFromMemory(prefixPointer);
        repository.deleteByPrefix(new String(prefix), null);
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
        String prefix = new String(hostApi.getDataFromMemory(prefixPointer));

        byte[] limitBytes = hostApi.getDataFromMemory(limitPointer);
        Long limit = new ScaleCodecReader(limitBytes).readOptional(ScaleCodecReader.UINT32).orElse(null);

        DeleteByPrefixResult result = repository.deleteByPrefix(prefix, limit);

        return hostApi.writeDataToMemory(result.scaleEncoded());
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
        String key = new String(hostApi.getDataFromMemory(keyPointer));
        byte[] sequence = (byte[]) repository.find(key).orElse(null);
        byte[] valueToAppend = hostApi.getDataFromMemory(valuePointer);

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
            throw new RuntimeException(e);
        }
        repository.save(key, buf.toByteArray());
    }

    /**
     * Compute the storage root.
     *
     * @return a pointer-size to a buffer containing the 256-bit Blake2 storage root.
     */
    public RuntimePointerSize extStorageRootVersion1() {
        //TODO: compute from Trie
        byte[] rootHash = (byte[]) repository.find(DBConstants.STATE_TRIE_ROOT_HASH).orElseThrow();

        return hostApi.writeDataToMemory(rootHash);
    }

    /**
     * Compute the storage root.
     *
     * @param version the state version
     * @return a pointer-size to a buffer containing the 256-bit Blake2 storage root.
     */
    public RuntimePointerSize extStorageRootVersion2(int version) {
        // TODO: update to use state trie versions
        return extStorageRootVersion1();
    }

    /**
     * This function is no longer used and only exists for compatibility reasons.
     *
     * @param parentHashPointer a pointer-size to the SCALE encoded block hash.
     * @return a pointer-size to an Option type (Definition 185) that’s always None.
     */
    public RuntimePointerSize extStorageChangesRootVersion1(RuntimePointerSize parentHashPointer) {
        return hostApi.writeDataToMemory(scaleEncodedOption(null));
    }

    /**
     * Get the next key in storage after the given one in lexicographic order.
     * The key provided to this function may or may not exist in storage.
     *
     * @param keyPointer a pointer-size to the key.
     * @return a pointer-size to the SCALE encoded Option value containing the next key in lexicographic order.
     */
    public RuntimePointerSize extStorageNextKeyVersion1(RuntimePointerSize keyPointer) {
        byte[] key = hostApi.getDataFromMemory(keyPointer);
        byte[] nextKey = repository.getNextKey(new String(key)).map(String::getBytes).orElse(null);

        return hostApi.writeDataToMemory(scaleEncodedOption(nextKey));
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
            throw new RuntimeException(e);
        }
        return buf.toByteArray();
    }

    public static byte[] scaleEncodedOption(@Nullable byte[] data) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (ScaleCodecWriter writer = new ScaleCodecWriter(buf)) {
            writer.writeOptional(ScaleCodecWriter::writeByteArray, data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return buf.toByteArray();
    }
}
