package com.limechain.runtime.research.hybrid.hostapi;

import com.limechain.runtime.hostapi.StorageHostFunctions;
import com.limechain.runtime.hostapi.TrieHostFunctions;
import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import com.limechain.runtime.research.hybrid.context.Context;
import com.limechain.runtime.version.StateVersion;
import com.limechain.trie.structure.nibble.Nibbles;
import com.limechain.utils.HashUtils;
import com.limechain.utils.scale.ScaleUtils;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import lombok.extern.java.Log;
import org.wasmer.ImportObject;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("SequencedCollectionMethodCanBeUsed")
@Log
public class MinimalHostapiImpl extends HostApiImpl {
    public MinimalHostapiImpl(Context context) {
        super(context);
    }

    @Override
    public List<ImportObject> getFunctionImports() {
        return Arrays.stream(Endpoint.values()).map(this::getFunctionImport).toList();
    }

    private ImportObject getFunctionImport(Endpoint endpoint) {
        return switch (endpoint) {
            case ext_allocator_free_version_1 -> Endpoint.ext_allocator_free_version_1.getImportObject(argv -> {
                extAllocatorFreeVersion1(argv.get(0).intValue());
            });
            case ext_allocator_malloc_version_1 -> Endpoint.ext_allocator_malloc_version_1.getImportObject(argv -> {
                return extAllocatorMallocVersion1(argv.get(0).intValue());
            });
            case ext_hashing_blake2_256_version_1 -> Endpoint.ext_hashing_blake2_256_version_1.getImportObject(argv -> {
                return blake2256V1(new RuntimePointerSize(argv.get(0)));
            });
            case ext_hashing_twox_128_version_1 -> Endpoint.ext_hashing_twox_128_version_1.getImportObject(argv -> {
                return twox128V1(new RuntimePointerSize(argv.get(0)));
            });
            case ext_storage_changes_root_version_1 ->
                Endpoint.ext_storage_changes_root_version_1.getImportObject(argv -> {
                    return extStorageChangesRootVersion1(new RuntimePointerSize(argv.get(0))).pointerSize();
                });
            case ext_storage_clear_prefix_version_1 ->
                Endpoint.ext_storage_clear_prefix_version_1.getImportObject(argv -> {
                    extStorageClearPrefixVersion1(new RuntimePointerSize(argv.get(0)));
                });
            case ext_storage_clear_version_1 -> Endpoint.ext_storage_clear_version_1.getImportObject(argv -> {
                extStorageClearVersion1(new RuntimePointerSize(argv.get(0)));
            });
            case ext_storage_get_version_1 -> Endpoint.ext_storage_get_version_1.getImportObject(argv -> {
                return extStorageGetVersion1(new RuntimePointerSize(argv.get(0))).pointerSize();
            });
            case ext_storage_read_version_1 -> Endpoint.ext_storage_read_version_1.getImportObject(argv -> {
                return extStorageReadVersion1(
                    new RuntimePointerSize(argv.get(0)), new RuntimePointerSize(argv.get(1)),
                    argv.get(2).intValue()).pointerSize();
            });
            case ext_storage_root_version_1 -> Endpoint.ext_storage_root_version_1.getImportObject(argv -> {
                return extStorageRootVersion1().pointerSize();
            });
            case ext_storage_set_version_1 -> Endpoint.ext_storage_set_version_1.getImportObject(argv -> {
                extStorageSetVersion1(
                    new RuntimePointerSize(argv.get(0)),
                    new RuntimePointerSize(argv.get(1)));
            });
            case ext_trie_blake2_256_ordered_root_version_1 ->
                Endpoint.ext_trie_blake2_256_ordered_root_version_1.getImportObject(argv -> {
                    var encodedVals = sharedMemory.readData(new RuntimePointerSize(argv.get(0)));
                    List<byte[]> values = ScaleUtils.Decode.decodeList(encodedVals, ScaleCodecReader::readByteArray);

                    byte[] trieRoot = new TrieHostFunctions.TrieRootCalculator(TrieHostFunctions.HashFunction.BLAKE2B,
                        StateVersion.V0).orderedTrieRoot(values);

                    return sharedMemory.writeData(trieRoot).pointer();
                });
            default -> endpoint.getImportObjectNotImplemented();
        };
    }

    /**
     * Allocates the given number of bytes and returns the pointer to that memory location.
     *
     * @param size the size of the buffer to be allocated.
     * @return a pointer to the allocated buffer.
     */
    public int extAllocatorMallocVersion1(int size) {
        log.finest("extAllocatorMallocVersion1");
        return sharedMemory.allocate(size).pointer();

    }

    /**
     * Free the given pointer.
     *
     * @param pointer a pointer to the memory buffer to be freed.
     */
    public void extAllocatorFreeVersion1(int pointer) {
        log.finest("extAllocatorFreeVersion1");
        sharedMemory.deallocate(pointer);
    }

    /**
     * Conducts a 256-bit Blake2 hash.
     *
     * @param data a pointer-size to the data to be hashed.
     * @return a pointer to the buffer containing the 256-bit hash result.
     */
    public int blake2256V1(RuntimePointerSize data) {
        log.fine("blake2256V1");

        byte[] dataToHash = sharedMemory.readData(data);

        byte[] hash = HashUtils.hashWithBlake2b(dataToHash);

        return sharedMemory.writeData(hash).pointer();
    }

    /**
     * Conducts a 128-bit xxHash hash.
     *
     * @param data a pointer-size to the data to be hashed.
     * @return a pointer to the buffer containing the 128-bit hash result.
     */
    public int twox128V1(final RuntimePointerSize data) {
        log.fine("twox128V1");

        byte[] dataToHash = sharedMemory.readData(data);
        log.fine("with data to hash: " + new String(dataToHash));

        byte[] hash = HashUtils.hashXx128(0, dataToHash);

        return sharedMemory.writeData(hash).pointer();
    }

    public RuntimePointerSize extStorageChangesRootVersion1(RuntimePointerSize parentHashPointer) {
        log.fine("extStorageChangesRootVersion1");

        return sharedMemory.writeData(StorageHostFunctions.scaleEncodedOption(null));
    }

    public void extStorageClearPrefixVersion1(RuntimePointerSize prefixPointer) {
        log.fine("extStorageClearPrefixVersion1");
        Nibbles prefix = Nibbles.fromBytes(sharedMemory.readData(prefixPointer));
        context.getTrieAccessor().deleteByPrefix(prefix, null);
    }

    public void extStorageClearVersion1(RuntimePointerSize keyPointer) {
        log.fine("extStorageClearVersion1");
        Nibbles key = Nibbles.fromBytes(sharedMemory.readData(keyPointer));
        context.getTrieAccessor().delete(key);
    }

    public RuntimePointerSize extStorageGetVersion1(RuntimePointerSize keyPointer) {
        Nibbles key = Nibbles.fromBytes(sharedMemory.readData(keyPointer));
        byte[] value = context.getTrieAccessor().find(key).orElse(null);

        log.fine("");
        log.fine("extStorageGetVersion1");
        log.fine("key: " + key);
        log.fine("");

        return sharedMemory.writeData(StorageHostFunctions.scaleEncodedOption(value));
    }

    public RuntimePointerSize extStorageReadVersion1(RuntimePointerSize keyPointer, RuntimePointerSize valueOutPointer,
                                                     int offset) {
        log.fine("extStorageReadVersion1");
        Nibbles key = Nibbles.fromBytes(sharedMemory.readData(keyPointer));
        byte[] value = context.getTrieAccessor().find(key).orElse(null);

        if (value == null) {
            return sharedMemory.writeData(StorageHostFunctions.scaleEncodedOption(null));
        }

        int size = 0;
        if (offset <= value.length) {
            size = value.length - offset;
            sharedMemory.writeData(Arrays.copyOfRange(value, offset, value.length), valueOutPointer);
        }

        return sharedMemory.writeData(StorageHostFunctions.scaleEncodedOption(size));
    }

    public RuntimePointerSize extStorageRootVersion1() {
        log.fine("extStorageRootVersion1");
        byte[] rootHash = context.getTrieAccessor().getMerkleRoot(StateVersion.V0);

        return sharedMemory.writeData(rootHash);
    }

    public void extStorageSetVersion1(RuntimePointerSize keyPointer, RuntimePointerSize valuePointer) {
        Nibbles key = Nibbles.fromBytes(sharedMemory.readData(keyPointer));
        byte[] value = sharedMemory.readData(valuePointer);

        log.fine("");
        log.fine("extStorageSetVersion1 with ");
        log.fine("key: " + key);
        log.fine("value: " + Arrays.toString(value));
        log.fine("");
        context.getTrieAccessor().save(key, value);
    }
}
