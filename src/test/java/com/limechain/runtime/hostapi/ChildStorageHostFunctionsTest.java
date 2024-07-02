package com.limechain.runtime.hostapi;

import com.google.common.primitives.Bytes;
import com.limechain.runtime.SharedMemory;
import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import com.limechain.storage.DeleteByPrefixResult;
import com.limechain.trie.BlockTrieAccessor;
import com.limechain.trie.MemoryChildTrieAccessor;
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
class ChildStorageHostFunctionsTest {
    @InjectMocks
    private ChildStorageHostFunctions childStorageHostFunctions;

    @Mock
    private SharedMemory sharedMemory;

    @Mock
    private RuntimePointerSize keyPointer;

    @Mock
    private RuntimePointerSize childStorageKeyPointer;

    @Mock
    private RuntimePointerSize valuePointer;

    @Mock
    private RuntimePointerSize resultPointer;

    @Mock
    private BlockTrieAccessor repository;

    @Mock
    private MemoryChildTrieAccessor childTrieAccessor;

    private final byte[] keyBytes = new byte[]{1, 2, 3};
    private final Nibbles key = Nibbles.fromBytes(keyBytes);
    private final byte[] childStorageKeyBytes = new byte[]{0, 0, 0};
    private final Nibbles childStorageKey = Nibbles.fromBytes(childStorageKeyBytes);
    private final byte[] valueBytes = new byte[]{4, 5, 6};

    private final byte[] emptyOption = new byte[]{0};

    @Test
    void extStorageSetVersion1() {
        when(sharedMemory.readData(childStorageKeyPointer)).thenReturn(childStorageKeyBytes);
        when(sharedMemory.readData(keyPointer)).thenReturn(keyBytes);
        when(sharedMemory.readData(valuePointer)).thenReturn(valueBytes);
        when(repository.getChildTrie(childStorageKey)).thenReturn(childTrieAccessor);

        childStorageHostFunctions.extDefaultChildStorageSetVersion1(childStorageKeyPointer, keyPointer, valuePointer);

        verify(childTrieAccessor).upsertNode(key, valueBytes);
    }

    @Test
    void extStorageGetVersion1() {
        when(sharedMemory.readData(childStorageKeyPointer)).thenReturn(childStorageKeyBytes);
        when(sharedMemory.readData(keyPointer)).thenReturn(keyBytes);
        when(repository.getChildTrie(childStorageKey)).thenReturn(childTrieAccessor);
        when(childTrieAccessor.findStorageValue(key)).thenReturn(Optional.of(valueBytes));
        when(sharedMemory.writeData(toOption(valueBytes))).thenReturn(resultPointer);

        RuntimePointerSize result = childStorageHostFunctions
                .extDefaultChildStorageGetVersion1(childStorageKeyPointer, keyPointer);

        assertEquals(resultPointer, result);
    }

    @Test
    void extStorageGetVersion1ShouldReturnNoneOptionWhenNoValue() {
        when(sharedMemory.readData(childStorageKeyPointer)).thenReturn(childStorageKeyBytes);
        when(sharedMemory.readData(keyPointer)).thenReturn(keyBytes);
        when(repository.getChildTrie(childStorageKey)).thenReturn(childTrieAccessor);
        when(childTrieAccessor.findStorageValue(key)).thenReturn(Optional.empty());
        when(sharedMemory.writeData(emptyOption)).thenReturn(resultPointer);

        RuntimePointerSize result = childStorageHostFunctions.extDefaultChildStorageGetVersion1(
                childStorageKeyPointer, keyPointer);

        assertEquals(resultPointer, result);
    }

    @Test
    void extStorageReadVersion1() {
        byte[] scaleEncodedOptionSize = new byte[]{1, 2, 0, 0, 0}; // Option with value 2
        when(sharedMemory.readData(childStorageKeyPointer)).thenReturn(childStorageKeyBytes);
        when(sharedMemory.readData(keyPointer)).thenReturn(keyBytes);
        when(repository.getChildTrie(childStorageKey)).thenReturn(childTrieAccessor);
        when(childTrieAccessor.findStorageValue(key)).thenReturn(Optional.of(valueBytes));
        when(sharedMemory.writeData(scaleEncodedOptionSize)).thenReturn(resultPointer);
        doNothing().when(sharedMemory).writeData(any(), any());

        RuntimePointerSize result = childStorageHostFunctions
                .extDefaultChildStorageReadVersion1(childStorageKeyPointer, keyPointer, valuePointer, 1);

        verify(sharedMemory).writeData(Arrays.copyOfRange(valueBytes, 1, 3), valuePointer);
        assertEquals(resultPointer, result);
    }

    @Test
    void extStorageReadVersion1ShouldReturnNoneWhenNoValue() {
        when(sharedMemory.readData(childStorageKeyPointer)).thenReturn(childStorageKeyBytes);
        when(sharedMemory.readData(keyPointer)).thenReturn(keyBytes);
        when(repository.getChildTrie(childStorageKey)).thenReturn(childTrieAccessor);
        when(childTrieAccessor.findStorageValue(key)).thenReturn(Optional.empty());
        when(sharedMemory.writeData(emptyOption)).thenReturn(resultPointer);

        RuntimePointerSize result = childStorageHostFunctions
                .extDefaultChildStorageReadVersion1(childStorageKeyPointer, keyPointer, valuePointer, 1);

        verifyNoMoreInteractions(sharedMemory);
        assertEquals(resultPointer, result);
    }

    @Test
    void extStorageReadVersion1ShouldReturnPointerTo0WhenOffsetGreaterThanLength() {
        byte[] scaleEncodedOptionSize = new byte[]{1, 0, 0, 0, 0}; // Option with value 0
        when(sharedMemory.readData(childStorageKeyPointer)).thenReturn(childStorageKeyBytes);
        when(sharedMemory.readData(keyPointer)).thenReturn(keyBytes);
        when(repository.getChildTrie(childStorageKey)).thenReturn(childTrieAccessor);
        when(childTrieAccessor.findStorageValue(key)).thenReturn(Optional.of(valueBytes));
        when(sharedMemory.writeData(scaleEncodedOptionSize)).thenReturn(resultPointer);

        RuntimePointerSize result = childStorageHostFunctions
                .extDefaultChildStorageReadVersion1(childStorageKeyPointer, keyPointer, valuePointer, 10);

        verifyNoMoreInteractions(sharedMemory);
        assertEquals(resultPointer, result);
    }

    @Test
    void extStorageClearVersion1() {
        when(sharedMemory.readData(childStorageKeyPointer)).thenReturn(childStorageKeyBytes);
        when(sharedMemory.readData(keyPointer)).thenReturn(keyBytes);
        when(repository.getChildTrie(childStorageKey)).thenReturn(childTrieAccessor);

        childStorageHostFunctions.extDefaultChildStorageClearVersion1(childStorageKeyPointer, keyPointer);

        verify(childTrieAccessor).deleteNode(key);
    }

    @Test
    void extStorageExistsVersion1() {
        when(sharedMemory.readData(childStorageKeyPointer)).thenReturn(childStorageKeyBytes);
        when(sharedMemory.readData(keyPointer)).thenReturn(keyBytes);
        when(repository.getChildTrie(childStorageKey)).thenReturn(childTrieAccessor);
        when(childTrieAccessor.findStorageValue(key)).thenReturn(Optional.of(valueBytes));

        int result = childStorageHostFunctions.extDefaultChildStorageExistsVersion1(
                childStorageKeyPointer, keyPointer);

        assertEquals(1, result);
    }

    @Test
    void extStorageExistsVersion1WhenNonExistent() {
        when(sharedMemory.readData(childStorageKeyPointer)).thenReturn(childStorageKeyBytes);
        when(sharedMemory.readData(keyPointer)).thenReturn(keyBytes);
        when(repository.getChildTrie(childStorageKey)).thenReturn(childTrieAccessor);
        when(childTrieAccessor.findStorageValue(key)).thenReturn(Optional.empty());

        int result = childStorageHostFunctions
                .extDefaultChildStorageExistsVersion1(childStorageKeyPointer, keyPointer);

        assertEquals(0, result);
    }

    @Test
    void extStorageClearPrefixVersion1() {
        when(sharedMemory.readData(childStorageKeyPointer)).thenReturn(childStorageKeyBytes);
        when(sharedMemory.readData(keyPointer)).thenReturn(keyBytes);
        when(repository.getChildTrie(childStorageKey)).thenReturn(childTrieAccessor);

        childStorageHostFunctions.extDefaultChildStorageClearPrefixVersion1(childStorageKeyPointer, keyPointer);

        verify(childTrieAccessor).deleteMultipleNodesByPrefix(key, null);
    }

    @Test
    void extStorageClearPrefixVersion2WhenNotAllDeleted() {
        RuntimePointerSize limitPointer = mock(RuntimePointerSize.class);
        byte[] encodedLimit = new byte[]{1, 2, 0, 0, 0}; // Encoded option with value 2
        byte[] encodedResult = new byte[]{0, 2, 0, 0, 0}; // Result with remaining items and 2 deleted

        when(sharedMemory.readData(childStorageKeyPointer)).thenReturn(childStorageKeyBytes);
        when(sharedMemory.readData(keyPointer)).thenReturn(keyBytes);
        when(sharedMemory.readData(limitPointer)).thenReturn(encodedLimit);
        when(repository.getChildTrie(childStorageKey)).thenReturn(childTrieAccessor);
        when(childTrieAccessor.deleteMultipleNodesByPrefix(key, 2L)).thenReturn(new DeleteByPrefixResult(2, false));
        when(sharedMemory.writeData(encodedResult)).thenReturn(resultPointer);

        RuntimePointerSize result = childStorageHostFunctions
                .extDefaultChildStorageClearPrefixVersion2(childStorageKeyPointer, keyPointer, limitPointer);

        assertEquals(resultPointer, result);
    }

    @Test
    void extStorageClearPrefixVersion2WhenAllDeleted() {
        RuntimePointerSize limitPointer = mock(RuntimePointerSize.class);
        byte[] encodedLimit = new byte[]{1, 4, 0, 0, 0}; // Encoded option with value 4
        byte[] encodedResult = new byte[]{1, 3, 0, 0, 0}; // Result with no remaining items and 3 deleted
        when(sharedMemory.readData(childStorageKeyPointer)).thenReturn(childStorageKeyBytes);
        when(sharedMemory.readData(keyPointer)).thenReturn(keyBytes);
        when(sharedMemory.readData(limitPointer)).thenReturn(encodedLimit);
        when(repository.getChildTrie(childStorageKey)).thenReturn(childTrieAccessor);
        when(childTrieAccessor.deleteMultipleNodesByPrefix(key, 4L)).thenReturn(new DeleteByPrefixResult(3, true));
        when(sharedMemory.writeData(encodedResult)).thenReturn(resultPointer);

        RuntimePointerSize result = childStorageHostFunctions
                .extDefaultChildStorageClearPrefixVersion2(childStorageKeyPointer, keyPointer, limitPointer);

        assertEquals(resultPointer, result);
    }

    @Test
    void extStorageNextKeyVersion1WhenNextKeyExistsShouldReturnNextKeyAsOption() {
        byte[] nextKeyBytes = new byte[]{ 6, 3, 9, 8 };
        Nibbles nextKey = Nibbles.fromBytes(nextKeyBytes);
        when(sharedMemory.readData(childStorageKeyPointer)).thenReturn(childStorageKeyBytes);
        when(sharedMemory.readData(keyPointer)).thenReturn(keyBytes);
        when(repository.getChildTrie(childStorageKey)).thenReturn(childTrieAccessor);
        when(childTrieAccessor.getNextKey(key)).thenReturn(Optional.of(nextKey));
        when(sharedMemory.writeData(toOption(nextKeyBytes))).thenReturn(resultPointer);

        RuntimePointerSize result = childStorageHostFunctions
                .extDefaultChildStorageStorageNextKeyVersion1(childStorageKeyPointer, keyPointer);

        assertEquals(resultPointer, result);
    }

    @Test
    void extStorageNextKeyVersion1WhenNextKeyDoesNotExistsShouldReturnEmptyOption() {
        when(sharedMemory.readData(childStorageKeyPointer)).thenReturn(childStorageKeyBytes);
        when(sharedMemory.readData(keyPointer)).thenReturn(keyBytes);
        when(repository.getChildTrie(childStorageKey)).thenReturn(childTrieAccessor);
        when(sharedMemory.writeData(emptyOption)).thenReturn(resultPointer);

        RuntimePointerSize result = childStorageHostFunctions
                .extDefaultChildStorageStorageNextKeyVersion1(childStorageKeyPointer, keyPointer);

        assertEquals(resultPointer, result);
    }

    private byte[] toOption(byte[] data) {
        return Bytes.concat(new byte[]{1}, data);
    }
}