package com.limechain.runtime.hostapi;

import com.google.common.primitives.Bytes;
import com.limechain.runtime.SharedMemory;
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
    private SharedMemory sharedMemory;

    @Mock
    private RuntimePointerSize keyPointer;

    @Mock
    private RuntimePointerSize valuePointer;

    @Mock
    private RuntimePointerSize resultPointer;

    @Mock
    private BlockTrieAccessor blockTrieAccessor;

    private final byte[] keyBytes = new byte[] { 1, 2, 3 };
    private final Nibbles key = Nibbles.fromBytes(keyBytes);

    private final byte[] valueBytes = new byte[] { 4, 5, 6 };

    private final byte[] emptyOption = new byte[] { 0 };

    @Test
    void extStorageSetVersion1() {
        when(sharedMemory.readData(keyPointer)).thenReturn(keyBytes);
        when(sharedMemory.readData(valuePointer)).thenReturn(valueBytes);

        storageHostFunctions.extStorageSetVersion1(keyPointer, valuePointer);

        verify(blockTrieAccessor).upsertNode(key, valueBytes);
    }

    @Test
    void extStorageGetVersion1() {
        when(sharedMemory.readData(keyPointer)).thenReturn(keyBytes);
        when(blockTrieAccessor.findStorageValue(key)).thenReturn(Optional.of(valueBytes));
        when(sharedMemory.writeData(toOption(valueBytes))).thenReturn(resultPointer);

        RuntimePointerSize result = storageHostFunctions.extStorageGetVersion1(keyPointer);

        assertEquals(resultPointer, result);
    }

    @Test
    void extStorageGetVersion1ShouldReturnNoneOptionWhenNoValue() {
        when(sharedMemory.readData(keyPointer)).thenReturn(keyBytes);
        when(blockTrieAccessor.findStorageValue(key)).thenReturn(Optional.empty());
        when(sharedMemory.writeData(emptyOption)).thenReturn(resultPointer);

        RuntimePointerSize result = storageHostFunctions.extStorageGetVersion1(keyPointer);

        assertEquals(resultPointer, result);
    }

    @Test
    void extStorageReadVersion1() {
        byte[] scaleEncodedOptionSize = new byte[] { 1, 2, 0, 0, 0 }; // Option with value 2
        when(sharedMemory.readData(keyPointer)).thenReturn(keyBytes);
        when(blockTrieAccessor.findStorageValue(key)).thenReturn(Optional.of(valueBytes));
        when(sharedMemory.writeData(scaleEncodedOptionSize)).thenReturn(resultPointer);
        doNothing().when(sharedMemory).writeData(any(), any());

        RuntimePointerSize result = storageHostFunctions.extStorageReadVersion1(keyPointer, valuePointer, 1);

        verify(sharedMemory).writeData(Arrays.copyOfRange(valueBytes, 1, 3), valuePointer);
        assertEquals(resultPointer, result);
    }

    @Test
    void extStorageReadVersion1ShouldReturnNoneWhenNoValue() {
        when(sharedMemory.readData(keyPointer)).thenReturn(keyBytes);
        when(blockTrieAccessor.findStorageValue(key)).thenReturn(Optional.empty());
        when(sharedMemory.writeData(emptyOption)).thenReturn(resultPointer);

        RuntimePointerSize result = storageHostFunctions.extStorageReadVersion1(keyPointer, valuePointer, 1);

        verifyNoMoreInteractions(sharedMemory);
        assertEquals(resultPointer, result);
    }

    @Test
    void extStorageReadVersion1ShouldReturnPointerTo0WhenOffsetGreaterThanLength() {
        byte[] scaleEncodedOptionSize = new byte[] { 1, 0, 0, 0, 0 }; // Option with value 0
        when(sharedMemory.readData(keyPointer)).thenReturn(keyBytes);
        when(blockTrieAccessor.findStorageValue(key)).thenReturn(Optional.of(valueBytes));
        when(sharedMemory.writeData(scaleEncodedOptionSize)).thenReturn(resultPointer);

        RuntimePointerSize result = storageHostFunctions.extStorageReadVersion1(keyPointer, valuePointer, 10);

        verifyNoMoreInteractions(sharedMemory);
        assertEquals(resultPointer, result);
    }

    @Test
    void extStorageClearVersion1() {
        when(sharedMemory.readData(keyPointer)).thenReturn(keyBytes);

        storageHostFunctions.extStorageClearVersion1(keyPointer);

        verify(blockTrieAccessor).deleteNode(key);
    }

    @Test
    void extStorageExistsVersion1() {
        when(sharedMemory.readData(keyPointer)).thenReturn(keyBytes);
        when(blockTrieAccessor.findStorageValue(key)).thenReturn(Optional.of(valueBytes));

        int result = storageHostFunctions.extStorageExistsVersion1(keyPointer);

        assertEquals(1, result);
    }

    @Test
    void extStorageExistsVersion1WhenNonExistent() {
        when(sharedMemory.readData(keyPointer)).thenReturn(keyBytes);
        when(blockTrieAccessor.findStorageValue(key)).thenReturn(Optional.empty());

        int result = storageHostFunctions.extStorageExistsVersion1(keyPointer);

        assertEquals(0, result);
    }

    @Test
    void extStorageClearPrefixVersion1() {
        when(sharedMemory.readData(keyPointer)).thenReturn(keyBytes);

        storageHostFunctions.extStorageClearPrefixVersion1(keyPointer);

        verify(blockTrieAccessor).deleteMultipleNodesByPrefix(key, null);
    }

    @Test
    void extStorageClearPrefixVersion2WhenNotAllDeleted() {
        RuntimePointerSize limitPointer = mock(RuntimePointerSize.class);
        byte[] encodedLimit = new byte[] { 1, 2, 0, 0, 0 }; // Encoded option with value 2
        byte[] encodedResult = new byte[] { 0, 2, 0, 0, 0 }; // Result with remaining items and 2 deleted
        when(sharedMemory.readData(keyPointer)).thenReturn(keyBytes);
        when(sharedMemory.readData(limitPointer)).thenReturn(encodedLimit);
        when(blockTrieAccessor.deleteMultipleNodesByPrefix(key, 2L)).thenReturn(new DeleteByPrefixResult(2, false));
        when(sharedMemory.writeData(encodedResult)).thenReturn(resultPointer);

        RuntimePointerSize result = storageHostFunctions.extStorageClearPrefixVersion2(keyPointer, limitPointer);

        assertEquals(resultPointer, result);
    }

    @Test
    void extStorageClearPrefixVersion2WhenAllDeleted() {
        RuntimePointerSize limitPointer = mock(RuntimePointerSize.class);
        byte[] encodedLimit = new byte[] { 1, 4, 0, 0, 0 }; // Encoded option with value 4
        byte[] encodedResult = new byte[] { 1, 3, 0, 0, 0 }; // Result with no remaining items and 3 deleted
        when(sharedMemory.readData(keyPointer)).thenReturn(keyBytes);
        when(sharedMemory.readData(limitPointer)).thenReturn(encodedLimit);
        when(blockTrieAccessor.deleteMultipleNodesByPrefix(key, 4L)).thenReturn(new DeleteByPrefixResult(3, true));
        when(sharedMemory.writeData(encodedResult)).thenReturn(resultPointer);

        RuntimePointerSize result = storageHostFunctions.extStorageClearPrefixVersion2(keyPointer, limitPointer);

        assertEquals(resultPointer, result);
    }

    @Test
    void extStorageAppendVersion1WhenNoSequenceShouldStoreValueAsByteArray() {
        when(sharedMemory.readData(keyPointer)).thenReturn(keyBytes);
        when(sharedMemory.readData(valuePointer)).thenReturn(valueBytes);
        when(blockTrieAccessor.findStorageValue(key)).thenReturn(Optional.empty());

        storageHostFunctions.extStorageAppendVersion1(keyPointer, valuePointer);

        verify(blockTrieAccessor).upsertNode(key, valueBytes);
    }

    @Test
    void extStorageAppendVersion1WhenSequenceExistSaveAppendedSequence() {
        byte[] sequence = new byte[] { 8, 3, 4, 5, 6, 7, 8 };
        byte[] newSequence = new byte[] { 12, 3, 4, 5, 6, 7, 8, 4, 5, 6 };
        when(sharedMemory.readData(keyPointer)).thenReturn(keyBytes);
        when(sharedMemory.readData(valuePointer)).thenReturn(valueBytes);
        when(blockTrieAccessor.findStorageValue(key)).thenReturn(Optional.of(sequence));

        storageHostFunctions.extStorageAppendVersion1(keyPointer, valuePointer);

        verify(blockTrieAccessor).upsertNode(key, newSequence);
    }

    @Test
    void extStorageChangesRootVersion1ShouldReturnPointerToEmptyOption() {
        when(sharedMemory.writeData(emptyOption)).thenReturn(resultPointer);

        RuntimePointerSize result = storageHostFunctions.extStorageChangesRootVersion1(keyPointer);

        assertEquals(resultPointer, result);
    }

    @Test
    void extStorageNextKeyVersion1WhenNextKeyExistsShouldReturnNextKeyAsOption() {
        byte[] nextKeyBytes = new byte[]{ 6, 3, 9, 8 };
        Nibbles nextKey = Nibbles.fromBytes(nextKeyBytes);
        when(sharedMemory.readData(keyPointer)).thenReturn(keyBytes);
        when(blockTrieAccessor.getNextKey(key)).thenReturn(Optional.of(nextKey));
        when(sharedMemory.writeData(toOption(nextKeyBytes))).thenReturn(resultPointer);

        RuntimePointerSize result = storageHostFunctions.extStorageNextKeyVersion1(keyPointer);

        assertEquals(resultPointer, result);
    }

    @Test
    void extStorageNextKeyVersion1WhenNextKeyDoesNotExistsShouldReturnEmptyOption() {
        when(sharedMemory.readData(keyPointer)).thenReturn(keyBytes);
        when(blockTrieAccessor.getNextKey(key)).thenReturn(Optional.empty());
        when(sharedMemory.writeData(emptyOption)).thenReturn(resultPointer);

        RuntimePointerSize result = storageHostFunctions.extStorageNextKeyVersion1(keyPointer);

        assertEquals(resultPointer, result);
    }

    @Test
    void extStorageStartTransactionVersion1ShouldStartRepositoryTransaction() {
        storageHostFunctions.extStorageStartTransactionVersion1();
        verify(blockTrieAccessor).startTransaction();
    }

    @Test
    void extStorageRollbackTransactionVersion1ShouldRollbackRepositoryTransaction() {
        storageHostFunctions.extStorageRollbackTransactionVersion1();
        verify(blockTrieAccessor).rollbackTransaction();
    }

    @Test
    void extStorageCommitTransactionVersion1ShouldRollbackRepositoryTransaction() {
        storageHostFunctions.extStorageCommitTransactionVersion1();
        verify(blockTrieAccessor).commitTransaction();
    }

    private byte[] toOption(byte[] data) {
        return Bytes.concat(new byte[] { 1 }, data);
    }
}