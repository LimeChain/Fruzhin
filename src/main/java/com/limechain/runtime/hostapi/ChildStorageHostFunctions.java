package com.limechain.runtime.hostapi;

import com.google.common.primitives.Bytes;
import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import com.limechain.storage.DBConstants;
import com.limechain.storage.DeleteByPrefixResult;
import com.limechain.storage.KVRepository;
import com.limechain.sync.warpsync.SyncedState;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import lombok.AllArgsConstructor;
import org.wasmer.ImportObject;
import org.wasmer.Type;

import java.util.Arrays;
import java.util.List;

import static com.limechain.runtime.hostapi.StorageHostFunctions.scaleEncodedOption;

/**
 * Implementations of the Child storage HostAPI functions
 * For more info check
 * {<a href="https://spec.polkadot.network/chap-host-api#sect-child-storage-api">Child Storage API</a>}
 */
@AllArgsConstructor
public class ChildStorageHostFunctions {
    private final HostApi hostApi;
    private final KVRepository<String, Object> repository;

    public ChildStorageHostFunctions() {
        this.hostApi = HostApi.getInstance();
        this.repository = SyncedState.getInstance().getRepository();
    }

    public static List<ImportObject> getFunctions() {
        return new ChildStorageHostFunctions().buildFunctions();
    }

    public List<ImportObject> buildFunctions() {
        return Arrays.asList(
                HostApi.getImportObject("ext_default_child_storage_set_version_1", argv ->
                        extDefaultChildStorageSetVersion1(
                                new RuntimePointerSize(argv.get(0)),
                                new RuntimePointerSize(argv.get(1)),
                                new RuntimePointerSize(argv.get(2))), List.of(Type.I64, Type.I64, Type.I64)),
                HostApi.getImportObject(
                        "ext_default_child_storage_get_version_1", argv ->
                                extDefaultChildStorageSetVersion1(
                                        new RuntimePointerSize(argv.get(0)),
                                        new RuntimePointerSize(argv.get(1)),
                                        new RuntimePointerSize(argv.get(2))), List.of(Type.I64, Type.I64, Type.I64)),
                HostApi.getImportObject("ext_default_child_storage_read_version_1", argv ->
                                extDefaultChildStorageReadVersion1(
                                        new RuntimePointerSize(argv.get(0)),
                                        new RuntimePointerSize(argv.get(1)),
                                        new RuntimePointerSize(argv.get(2)),
                                        argv.get(2).intValue()).pointerSize()
                        , List.of(Type.I64, Type.I64, Type.I64, Type.I32), Type.I64),
                HostApi.getImportObject("ext_default_child_storage_clear_version_1", argv ->
                                extDefaultChildStorageClearVersion1(
                                        new RuntimePointerSize(argv.get(0)),
                                        new RuntimePointerSize(argv.get(1)))
                        , List.of(Type.I64, Type.I64)),
                HostApi.getImportObject("ext_default_child_storage_storage_kill_version_1", argv ->
                                extDefaultChildStorageKillVersion1(
                                        new RuntimePointerSize(argv.get(0)))
                        , List.of(Type.I64)),
                HostApi.getImportObject("ext_default_child_storage_storage_kill_version_2", argv ->
                                extDefaultChildStorageKillVersion2(
                                        new RuntimePointerSize(argv.get(0)),
                                        new RuntimePointerSize(argv.get(1))).size()
                        , List.of(Type.I64, Type.I64), Type.I32),
                HostApi.getImportObject("ext_default_child_storage_storage_kill_version_3", argv ->
                                extDefaultChildStorageKillVersion3(
                                        new RuntimePointerSize(argv.get(0)),
                                        new RuntimePointerSize(argv.get(1))).pointerSize()
                        , List.of(Type.I64, Type.I64), Type.I64),
                HostApi.getImportObject("ext_default_child_storage_exists_version_1", argv ->
                                extDefaultChildStorageExistsVersion1(
                                        new RuntimePointerSize(argv.get(0)),
                                        new RuntimePointerSize(argv.get(1))),
                        List.of(Type.I64, Type.I64), Type.I32),
                HostApi.getImportObject("ext_default_child_storage_clear_prefix_version_1", argv ->
                        extDefaultChildStorageClearPrefixVersion1(
                                new RuntimePointerSize(argv.get(0)),
                                new RuntimePointerSize(argv.get(1))), List.of(Type.I64, Type.I64)),
                HostApi.getImportObject("ext_default_child_storage_clear_prefix_version_2", argv ->
                                extDefaultChildStorageClearPrefixVersion2(
                                        new RuntimePointerSize(argv.get(0)),
                                        new RuntimePointerSize(argv.get(1)),
                                        new RuntimePointerSize(argv.get(2))).pointerSize()
                        , List.of(Type.I64, Type.I64, Type.I64), Type.I64),
                HostApi.getImportObject("ext_default_child_storage_root_version_1", argv ->
                    extDefaultChildStorageRootVersion1().pointerSize(),
                            List.of(Type.I64), Type.I64),
                HostApi.getImportObject("ext_default_child_storage_root_version_2", argv ->
                        extDefaultChildStorageRootVersion1().pointerSize(), List.of(Type.I64, Type.I32), Type.I64),
                HostApi.getImportObject("ext_default_child_storage_next_key_version_1", argv ->
                        extDefaultChildStorageStorageNextKeyVersion1(
                                new RuntimePointerSize(argv.get(0)),
                                new RuntimePointerSize(argv.get(1))
                        ).pointerSize()
                , List.of(Type.I64, Type.I64), Type.I64));
    }

    /**
     * Gets the given key from child storage, placing the value into a buffer and returning the number of bytes
     * that the entry in child storage has beyond the offset.
     *
     * @param childStorageKeyPointer a pointer-size containing the child storage key.
     * @param keyPointer a pointer-size containing the key.
     * @param valueOutPointer a pointer-size containing the buffer to which the value will be written to.
     *                        This function will never write more than the length of the buffer,
     *                        even if the value’s length is bigger.
     * @param offset the offset beyond the value that should be read from.
     * @return a pointer-size pointing to a SCALE encoded Option value containing an unsigned 32-bit integer
     * representing the number of bytes left at supplied offset. Returns None if the entry does not exist.
     */
    public RuntimePointerSize extDefaultChildStorageReadVersion1(RuntimePointerSize childStorageKeyPointer,
                                                                 RuntimePointerSize keyPointer,
                                                                 RuntimePointerSize valueOutPointer,
                                                                 int offset) {
        byte[] childStorageKey = hostApi.getDataFromMemory(childStorageKeyPointer);
        byte[] key = hostApi.getDataFromMemory(keyPointer);

        byte[] value = (byte[]) repository.find(new String(Bytes.concat(childStorageKey, key))).orElse(null);

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
     * Sets the value under a given key into child storage.
     *
     * @param childStorageKeyPointer a pointer-size containing the child storage key.
     * @param keyPointer             a pointer-size containing the key.
     * @param valuePointer           a pointer-size containing the value.
     */
    public void extDefaultChildStorageSetVersion1(RuntimePointerSize childStorageKeyPointer,
                                                  RuntimePointerSize keyPointer,
                                                  RuntimePointerSize valuePointer) {
        byte[] childStorageKey = hostApi.getDataFromMemory(childStorageKeyPointer);
        byte[] key = hostApi.getDataFromMemory(keyPointer);
        byte[] value = hostApi.getDataFromMemory(valuePointer);

        repository.save(new String(Bytes.concat(childStorageKey, key)), value);
    }

    /**
     * Clears the child storage of the given key and its value. Non-existent entries are silently ignored.
     *
     * @param childStorageKeyPointer a pointer-size containing the child storage key.
     * @param keyPointer a pointer-size containing the key.
     */
    public void extDefaultChildStorageClearVersion1(RuntimePointerSize childStorageKeyPointer,
                                                    RuntimePointerSize keyPointer) {
        byte[] childStorageKey = hostApi.getDataFromMemory(childStorageKeyPointer);
        byte[] key = hostApi.getDataFromMemory(keyPointer);

        repository.delete(new String(Bytes.concat(childStorageKey, key)));
    }

    /**
     * Clear the child storage of each key/value pair where the key starts with the given prefix.
     *
     * @param childStorageKeyPointer a pointer-size containing the child storage key.
     * @param prefixPointer a pointer-size containing the prefix.
     */
    public void extDefaultChildStorageClearPrefixVersion1(RuntimePointerSize childStorageKeyPointer,
                                                          RuntimePointerSize prefixPointer) {
        byte[] childStorageKey = hostApi.getDataFromMemory(childStorageKeyPointer);
        byte[] prefix = hostApi.getDataFromMemory(prefixPointer);

        repository.deleteByPrefix(new String(Bytes.concat(childStorageKey, prefix)), null);
    }

    /**
     * Clear the child storage of each key/value pair where the key starts with the given prefix.
     *
     * @param childStorageKeyPointer a pointer-size containing the child storage key.
     * @param prefixPointer a pointer-size containing the prefix.
     * @param limitPointer  a pointer-size to an Option type containing an unsigned 32-bit integer indicating the limit
     *                      on how many keys should be deleted. No limit is applied if this is None.
     *                      Any keys created during the current block execution do not count toward the limit.
     * @return a pointer-size to the following variant, k = 0 -> c | k = 1 -> c
     * where 0 indicates that all keys of the child storage have been removed, followed by the number of removed keys.
     * The variant 1 indicates that there are remaining keys, followed by the number of removed keys.
     */
    public RuntimePointerSize extDefaultChildStorageClearPrefixVersion2(RuntimePointerSize childStorageKeyPointer,
                                                                        RuntimePointerSize prefixPointer,
                                                                        RuntimePointerSize limitPointer) {
        byte[] childStorageKey = hostApi.getDataFromMemory(childStorageKeyPointer);
        byte[] prefix = hostApi.getDataFromMemory(prefixPointer);

        byte[] limitBytes = hostApi.getDataFromMemory(limitPointer);
        Long limit = new ScaleCodecReader(limitBytes).readOptional(ScaleCodecReader.UINT32).orElse(null);

        DeleteByPrefixResult result =
                repository.deleteByPrefix(new String(Bytes.concat(childStorageKey, prefix)), limit);

        return hostApi.writeDataToMemory(result.scaleEncoded());
    }

    /**
     * Checks whether the given key exists in child storage.
     *
     * @param childStorageKeyPointer a pointer-size containing the child storage key.
     * @param keyPointer a pointer-size containing the key.
     * @return integer value equal to 1 if the key exists or a value equal to 0 if otherwise.
     */
    public int extDefaultChildStorageExistsVersion1(RuntimePointerSize childStorageKeyPointer,
                                                    RuntimePointerSize keyPointer) {
        byte[] childStorageKey = hostApi.getDataFromMemory(childStorageKeyPointer);
        byte[] key = hostApi.getDataFromMemory(keyPointer);
        return repository.find(new String(Bytes.concat(childStorageKey, key))).isPresent() ? 1 : 0;
    }

    /**
     * Retrieves the value associated with the given key from child storage.
     *
     * @param childStorageKeyPointer a pointer-size containing the child storage key.
     * @param keyPointer a pointer-size containing the key
     * @return a pointer-size returning the SCALE encoded Option value containing the value.
     */
    public RuntimePointerSize extDefaultChildStorageGetVersion1(RuntimePointerSize childStorageKeyPointer,
                                                                RuntimePointerSize keyPointer) {
        byte[] childStorageKey = hostApi.getDataFromMemory(childStorageKeyPointer);
        byte[] key = hostApi.getDataFromMemory(keyPointer);
        byte[] value = (byte[]) repository.find(new String(Bytes.concat(childStorageKey, key))).orElse(null);

        return hostApi.writeDataToMemory(scaleEncodedOption(value));
    }

    /**
     * Get the next key in child storage after the given one in lexicographic order.
     * The key provided to this function may or may not exist in child storage.
     *
     * @param childStorageKeyPointer a pointer-size containing the child storage key.
     * @param keyPointer a pointer-size to the key.
     * @return a pointer-size to the SCALE encoded Option value containing the next key in lexicographic order.
     */
    public RuntimePointerSize extDefaultChildStorageStorageNextKeyVersion1(RuntimePointerSize childStorageKeyPointer,
                                                                           RuntimePointerSize keyPointer) {
        byte[] childStorageKey = hostApi.getDataFromMemory(childStorageKeyPointer);
        byte[] key = hostApi.getDataFromMemory(keyPointer);
        String combinedKey = new String(Bytes.concat(childStorageKey, key));

        byte[] nextKey = repository.getNextKey(combinedKey).map(String::getBytes).orElse(null);

        return hostApi.writeDataToMemory(scaleEncodedOption(nextKey));
    }


    /**
     * Compute the child storage root.
     *
     * @return a pointer-size to a buffer containing the 256-bit Blake2 child storage root.
     */
    public RuntimePointerSize extDefaultChildStorageRootVersion1() {
        //TODO: compute from Trie
        byte[] rootHash = (byte[]) repository.find(DBConstants.STATE_TRIE_ROOT_HASH).orElseThrow();

        return hostApi.writeDataToMemory(rootHash);
    }

    /**
     * Compute the child storage root.
     *
     * @param version the state version
     * @return a pointer-size to a buffer containing the 256-bit Blake2 child storage root.
     */
    public RuntimePointerSize extDefaultChildStorageRootVersion2(int version) {
        // TODO: update to use state trie versions
        return extDefaultChildStorageRootVersion1();
    }

    /**
     * Deletes the child storage
     *
     * @param childStorageKeyPointer a pointer-size containing the child storage key.
     */
    public void extDefaultChildStorageKillVersion1(RuntimePointerSize childStorageKeyPointer) {
        byte[] childStorageKey = hostApi.getDataFromMemory(childStorageKeyPointer);
        repository.deleteByPrefix(new String(childStorageKey), null);
    }

    /**
     * Deletes a limited amount of entries in the child storage
     *
     * @param childStorageKeyPointer a pointer-size containing the child storage key.
     * @param limitPointer a pointer-size containing the limit.
     */
    public RuntimePointerSize extDefaultChildStorageKillVersion2(RuntimePointerSize childStorageKeyPointer,
                                                                 RuntimePointerSize limitPointer) {
        byte[] childStorageKey = hostApi.getDataFromMemory(childStorageKeyPointer);

        byte[] limitBytes = hostApi.getDataFromMemory(limitPointer);
        Long limit = new ScaleCodecReader(limitBytes).readOptional(ScaleCodecReader.UINT32).orElse(null);

        DeleteByPrefixResult result =
                repository.deleteByPrefix(new String(childStorageKey), limit);

        return hostApi.writeDataToMemory(result.scaleEncoded());
    }

    /**
     * Deletes a limited amount of entries in the child storage
     *
     * @param childStorageKeyPointer a pointer-size containing the child storage key.
     */
    public RuntimePointerSize extDefaultChildStorageKillVersion3(RuntimePointerSize childStorageKeyPointer,
                                                                 RuntimePointerSize limitPointer) {
        return extDefaultChildStorageKillVersion2(childStorageKeyPointer, limitPointer);
    }
}
