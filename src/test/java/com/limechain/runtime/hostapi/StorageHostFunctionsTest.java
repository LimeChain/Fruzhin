package com.limechain.runtime.hostapi;

import com.google.common.primitives.Bytes;
import com.limechain.runtime.Runtime;
import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import com.limechain.storage.DeleteByPrefixResult;
import com.limechain.trie.BlockTrieAccessor;
import com.limechain.trie.structure.nibble.Nibbles;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Disabled
class StorageHostFunctionsTest {
    @InjectMocks
    private StorageHostFunctions storageHostFunctions;

    @Mock
    private Runtime runtime;

    @Mock
    private RuntimePointerSize keyPointer;

    @Mock
    private RuntimePointerSize valuePointer;

    @Mock
    private RuntimePointerSize resultPointer;

    @Mock
    private BlockTrieAccessor repository;

    private final byte[] keyBytes = new byte[] { 1, 2, 3 };
    private final Nibbles key = Nibbles.fromBytes(keyBytes);

    private final byte[] valueBytes = new byte[] { 4, 5, 6 };

    private final byte[] emptyOption = new byte[] { 0 };

    @Test
    void extStorageSetVersion1() {
        when(runtime.getDataFromMemory(keyPointer)).thenReturn(keyBytes);
        when(runtime.getDataFromMemory(valuePointer)).thenReturn(valueBytes);

        storageHostFunctions.extStorageSetVersion1(keyPointer, valuePointer);

        verify(repository).save(key, valueBytes);
    }

    @Test
    void extStorageGetVersion1() {
        when(runtime.getDataFromMemory(keyPointer)).thenReturn(keyBytes);
        when(repository.find(key)).thenReturn(Optional.of(valueBytes));
        when(runtime.writeDataToMemory(toOption(valueBytes))).thenReturn(resultPointer);

        RuntimePointerSize result = storageHostFunctions.extStorageGetVersion1(keyPointer);

        assertEquals(resultPointer, result);
    }

    @Test
    void extStorageGetVersion1ShouldReturnNoneOptionWhenNoValue() {
        when(runtime.getDataFromMemory(keyPointer)).thenReturn(keyBytes);
        when(repository.find(key)).thenReturn(Optional.empty());
        when(runtime.writeDataToMemory(emptyOption)).thenReturn(resultPointer);

        RuntimePointerSize result = storageHostFunctions.extStorageGetVersion1(keyPointer);

        assertEquals(resultPointer, result);
    }

    @Test
    void extStorageReadVersion1() {
        byte[] scaleEncodedOptionSize = new byte[] { 1, 2, 0, 0, 0 }; // Option with value 2
        when(runtime.getDataFromMemory(keyPointer)).thenReturn(keyBytes);
        when(repository.find(key)).thenReturn(Optional.of(valueBytes));
        when(runtime.writeDataToMemory(scaleEncodedOptionSize)).thenReturn(resultPointer);
        doNothing().when(runtime).writeDataToMemory(any(), any());

        RuntimePointerSize result = storageHostFunctions.extStorageReadVersion1(keyPointer, valuePointer, 1);

        verify(runtime).writeDataToMemory(Arrays.copyOfRange(valueBytes, 1, 3), valuePointer);
        assertEquals(resultPointer, result);
    }

    @Test
    void extStorageReadVersion1ShouldReturnNoneWhenNoValue() {
        when(runtime.getDataFromMemory(keyPointer)).thenReturn(keyBytes);
        when(repository.find(key)).thenReturn(Optional.empty());
        when(runtime.writeDataToMemory(emptyOption)).thenReturn(resultPointer);

        RuntimePointerSize result = storageHostFunctions.extStorageReadVersion1(keyPointer, valuePointer, 1);

        verifyNoMoreInteractions(runtime);
        assertEquals(resultPointer, result);
    }

    @Test
    void extStorageReadVersion1ShouldReturnPointerTo0WhenOffsetGreaterThanLength() {
        byte[] scaleEncodedOptionSize = new byte[] { 1, 0, 0, 0, 0 }; // Option with value 0
        when(runtime.getDataFromMemory(keyPointer)).thenReturn(keyBytes);
        when(repository.find(key)).thenReturn(Optional.of(valueBytes));
        when(runtime.writeDataToMemory(scaleEncodedOptionSize)).thenReturn(resultPointer);

        RuntimePointerSize result = storageHostFunctions.extStorageReadVersion1(keyPointer, valuePointer, 10);

        verifyNoMoreInteractions(runtime);
        assertEquals(resultPointer, result);
    }

    @Test
    void extStorageClearVersion1() {
        when(runtime.getDataFromMemory(keyPointer)).thenReturn(keyBytes);

        storageHostFunctions.extStorageClearVersion1(keyPointer);

        verify(repository).delete(key);
    }

    @Test
    void extStorageExistsVersion1() {
        when(runtime.getDataFromMemory(keyPointer)).thenReturn(keyBytes);
        when(repository.find(key)).thenReturn(Optional.of(valueBytes));

        int result = storageHostFunctions.extStorageExistsVersion1(keyPointer);

        assertEquals(1, result);
    }

    @Test
    void extStorageExistsVersion1WhenNonExistent() {
        when(runtime.getDataFromMemory(keyPointer)).thenReturn(keyBytes);
        when(repository.find(key)).thenReturn(Optional.empty());

        int result = storageHostFunctions.extStorageExistsVersion1(keyPointer);

        assertEquals(0, result);
    }

    @Test
    void extStorageClearPrefixVersion1() {
        when(runtime.getDataFromMemory(keyPointer)).thenReturn(keyBytes);

        storageHostFunctions.extStorageClearPrefixVersion1(keyPointer);

        verify(repository).deleteByPrefix(key, null);
    }

    @Test
    void extStorageClearPrefixVersion2WhenNotAllDeleted() {
        RuntimePointerSize limitPointer = mock(RuntimePointerSize.class);
        byte[] encodedLimit = new byte[] { 1, 2, 0, 0, 0 }; // Encoded option with value 2
        byte[] encodedResult = new byte[] { 0, 2, 0, 0, 0 }; // Result with remaining items and 2 deleted
        when(runtime.getDataFromMemory(keyPointer)).thenReturn(keyBytes);
        when(runtime.getDataFromMemory(limitPointer)).thenReturn(encodedLimit);
        when(repository.deleteByPrefix(key, 2L)).thenReturn(new DeleteByPrefixResult(2, false));
        when(runtime.writeDataToMemory(encodedResult)).thenReturn(resultPointer);

        RuntimePointerSize result = storageHostFunctions.extStorageClearPrefixVersion2(keyPointer, limitPointer);

        assertEquals(resultPointer, result);
    }

    @Test
    void extStorageClearPrefixVersion2WhenAllDeleted() {
        RuntimePointerSize limitPointer = mock(RuntimePointerSize.class);
        byte[] encodedLimit = new byte[] { 1, 4, 0, 0, 0 }; // Encoded option with value 4
        byte[] encodedResult = new byte[] { 1, 3, 0, 0, 0 }; // Result with no remaining items and 3 deleted
        when(runtime.getDataFromMemory(keyPointer)).thenReturn(keyBytes);
        when(runtime.getDataFromMemory(limitPointer)).thenReturn(encodedLimit);
        when(repository.deleteByPrefix(key, 4L)).thenReturn(new DeleteByPrefixResult(3, true));
        when(runtime.writeDataToMemory(encodedResult)).thenReturn(resultPointer);

        RuntimePointerSize result = storageHostFunctions.extStorageClearPrefixVersion2(keyPointer, limitPointer);

        assertEquals(resultPointer, result);
    }

    @Test
    void extStorageAppendVersion1WhenNoSequenceShouldStoreValueAsByteArray() {
        when(runtime.getDataFromMemory(keyPointer)).thenReturn(keyBytes);
        when(runtime.getDataFromMemory(valuePointer)).thenReturn(valueBytes);
        when(repository.find(key)).thenReturn(Optional.empty());

        storageHostFunctions.extStorageAppendVersion1(keyPointer, valuePointer);

        verify(repository).save(key, valueBytes);
    }

    @Test
    void extStorageAppendVersion1WhenSequenceExistSaveAppendedSequence() {
        byte[] sequence = new byte[] { 8, 3, 4, 5, 6, 7, 8 };
        byte[] newSequence = new byte[] { 12, 3, 4, 5, 6, 7, 8, 4, 5, 6 };
        when(runtime.getDataFromMemory(keyPointer)).thenReturn(keyBytes);
        when(runtime.getDataFromMemory(valuePointer)).thenReturn(valueBytes);
        when(repository.find(key)).thenReturn(Optional.of(sequence));

        storageHostFunctions.extStorageAppendVersion1(keyPointer, valuePointer);

        verify(repository).save(key, newSequence);
    }

    @Test
    void extStorageChangesRootVersion1ShouldReturnPointerToEmptyOption() {
        when(runtime.writeDataToMemory(emptyOption)).thenReturn(resultPointer);

        RuntimePointerSize result = storageHostFunctions.extStorageChangesRootVersion1(keyPointer);

        assertEquals(resultPointer, result);
    }

    @Test
    void extStorageNextKeyVersion1WhenNextKeyExistsShouldReturnNextKeyAsOption() {
        byte[] nextKeyBytes = new byte[]{ 6, 3, 9, 8 };
        Nibbles nextKey = Nibbles.fromBytes(nextKeyBytes);
        when(runtime.getDataFromMemory(keyPointer)).thenReturn(keyBytes);
        when(repository.getNextKey(key)).thenReturn(Optional.of(nextKey));
        when(runtime.writeDataToMemory(toOption(nextKeyBytes))).thenReturn(resultPointer);

        RuntimePointerSize result = storageHostFunctions.extStorageNextKeyVersion1(keyPointer);

        assertEquals(resultPointer, result);
    }

    @Test
    void extStorageNextKeyVersion1WhenNextKeyDoesNotExistsShouldReturnEmptyOption() {
        when(runtime.getDataFromMemory(keyPointer)).thenReturn(keyBytes);
        when(repository.getNextKey(key)).thenReturn(Optional.empty());
        when(runtime.writeDataToMemory(emptyOption)).thenReturn(resultPointer);

        RuntimePointerSize result = storageHostFunctions.extStorageNextKeyVersion1(keyPointer);

        assertEquals(resultPointer, result);
    }

    @Test
    void extStorageStartTransactionVersion1ShouldStartRepositoryTransaction() {
        storageHostFunctions.extStorageStartTransactionVersion1();
        verify(repository).startTransaction();
    }

    @Test
    void extStorageRollbackTransactionVersion1ShouldRollbackRepositoryTransaction() {
        storageHostFunctions.extStorageRollbackTransactionVersion1();
        verify(repository).rollbackTransaction();
    }

    @Test
    void extStorageCommitTransactionVersion1ShouldRollbackRepositoryTransaction() {
        storageHostFunctions.extStorageCommitTransactionVersion1();
        verify(repository).commitTransaction();
    }

    private byte[] toOption(byte[] data) {
        return Bytes.concat(new byte[] { 1 }, data);
    }
}