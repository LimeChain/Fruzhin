package com.limechain.runtime.hostapi;

import com.google.common.primitives.Bytes;
import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import com.limechain.storage.DeleteByPrefixResult;
import com.limechain.storage.KVRepository;
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
class ChildStorageHostFunctionsTest {
    @InjectMocks
    private ChildStorageHostFunctions childStorageHostFunctions;

    @Mock
    private HostApi hostApi;

    @Mock
    private RuntimePointerSize keyPointer;

    @Mock
    private RuntimePointerSize childStorageKeyPointer;

    @Mock
    private RuntimePointerSize valuePointer;

    @Mock
    private RuntimePointerSize resultPointer;

    @Mock
    private KVRepository<String, Object> repository;

    private final byte[] keyBytes = new byte[]{1, 2, 3};
    private final byte[] childStorageKeyBytes = new byte[]{0, 0, 0};
    private final String combinedKey = new String(Bytes.concat(childStorageKeyBytes, keyBytes));

    private final byte[] valueBytes = new byte[]{4, 5, 6};

    private final byte[] emptyOption = new byte[]{0};

    @Test
    void extStorageSetVersion1() {
        when(hostApi.getDataFromMemory(childStorageKeyPointer)).thenReturn(childStorageKeyBytes);
        when(hostApi.getDataFromMemory(keyPointer)).thenReturn(keyBytes);
        when(hostApi.getDataFromMemory(valuePointer)).thenReturn(valueBytes);

        childStorageHostFunctions.extDefaultChildStorageSetVersion1(childStorageKeyPointer, keyPointer, valuePointer);

        verify(repository).save(combinedKey, valueBytes);
    }

    @Test
    void extStorageGetVersion1() {
        when(hostApi.getDataFromMemory(childStorageKeyPointer)).thenReturn(childStorageKeyBytes);
        when(hostApi.getDataFromMemory(keyPointer)).thenReturn(keyBytes);
        when(repository.find(combinedKey)).thenReturn(Optional.of(valueBytes));
        when(hostApi.writeDataToMemory(toOption(valueBytes))).thenReturn(resultPointer);

        RuntimePointerSize result = childStorageHostFunctions
                .extDefaultChildStorageGetVersion1(childStorageKeyPointer, keyPointer);

        assertEquals(resultPointer, result);
    }

    @Test
    void extStorageGetVersion1ShouldReturnNoneOptionWhenNoValue() {
        when(hostApi.getDataFromMemory(childStorageKeyPointer)).thenReturn(childStorageKeyBytes);
        when(hostApi.getDataFromMemory(keyPointer)).thenReturn(keyBytes);
        when(repository.find(combinedKey)).thenReturn(Optional.empty());
        when(hostApi.writeDataToMemory(emptyOption)).thenReturn(resultPointer);

        RuntimePointerSize result = childStorageHostFunctions.extDefaultChildStorageGetVersion1(
                childStorageKeyPointer, keyPointer);

        assertEquals(resultPointer, result);
    }

    @Test
    void extStorageReadVersion1() {
        byte[] scaleEncodedOptionSize = new byte[]{1, 2, 0, 0, 0}; // Option with value 2
        when(hostApi.getDataFromMemory(childStorageKeyPointer)).thenReturn(childStorageKeyBytes);
        when(hostApi.getDataFromMemory(keyPointer)).thenReturn(keyBytes);
        when(repository.find(combinedKey)).thenReturn(Optional.of(valueBytes));
        when(hostApi.writeDataToMemory(scaleEncodedOptionSize)).thenReturn(resultPointer);
        doNothing().when(hostApi).writeDataToMemory(any(), any());

        RuntimePointerSize result = childStorageHostFunctions
                .extDefaultChildStorageReadVersion1(childStorageKeyPointer, keyPointer, valuePointer, 1);

        verify(hostApi).writeDataToMemory(Arrays.copyOfRange(valueBytes, 1, 3), valuePointer);
        assertEquals(resultPointer, result);
    }

    @Test
    void extStorageReadVersion1ShouldReturnNoneWhenNoValue() {
        when(hostApi.getDataFromMemory(childStorageKeyPointer)).thenReturn(childStorageKeyBytes);
        when(hostApi.getDataFromMemory(keyPointer)).thenReturn(keyBytes);
        when(repository.find(combinedKey)).thenReturn(Optional.empty());
        when(hostApi.writeDataToMemory(emptyOption)).thenReturn(resultPointer);

        RuntimePointerSize result = childStorageHostFunctions
                .extDefaultChildStorageReadVersion1(childStorageKeyPointer, keyPointer, valuePointer, 1);

        verifyNoMoreInteractions(hostApi);
        assertEquals(resultPointer, result);
    }

    @Test
    void extStorageReadVersion1ShouldReturnPointerTo0WhenOffsetGreaterThanLength() {
        byte[] scaleEncodedOptionSize = new byte[]{1, 0, 0, 0, 0}; // Option with value 0
        when(hostApi.getDataFromMemory(childStorageKeyPointer)).thenReturn(childStorageKeyBytes);
        when(hostApi.getDataFromMemory(keyPointer)).thenReturn(keyBytes);
        when(repository.find(combinedKey)).thenReturn(Optional.of(valueBytes));
        when(hostApi.writeDataToMemory(scaleEncodedOptionSize)).thenReturn(resultPointer);

        RuntimePointerSize result = childStorageHostFunctions
                .extDefaultChildStorageReadVersion1(childStorageKeyPointer, keyPointer, valuePointer, 10);

        verifyNoMoreInteractions(hostApi);
        assertEquals(resultPointer, result);
    }

    @Test
    void extStorageClearVersion1() {
        when(hostApi.getDataFromMemory(keyPointer)).thenReturn(keyBytes);
        when(hostApi.getDataFromMemory(childStorageKeyPointer)).thenReturn(childStorageKeyBytes);

        childStorageHostFunctions.extDefaultChildStorageClearVersion1(childStorageKeyPointer, keyPointer);

        verify(repository).delete(combinedKey);
    }

    @Test
    void extStorageExistsVersion1() {
        when(hostApi.getDataFromMemory(childStorageKeyPointer)).thenReturn(childStorageKeyBytes);
        when(hostApi.getDataFromMemory(keyPointer)).thenReturn(keyBytes);
        when(repository.find(combinedKey)).thenReturn(Optional.of(valueBytes));

        int result = childStorageHostFunctions.extDefaultChildStorageExistsVersion1(
                childStorageKeyPointer, keyPointer);

        assertEquals(1, result);
    }

    @Test
    void extStorageExistsVersion1WhenNonExistent() {
        when(hostApi.getDataFromMemory(childStorageKeyPointer)).thenReturn(childStorageKeyBytes);
        when(hostApi.getDataFromMemory(keyPointer)).thenReturn(keyBytes);
        when(repository.find(combinedKey)).thenReturn(Optional.empty());

        int result = childStorageHostFunctions
                .extDefaultChildStorageExistsVersion1(childStorageKeyPointer, keyPointer);

        assertEquals(0, result);
    }

    @Test
    void extStorageClearPrefixVersion1() {
        when(hostApi.getDataFromMemory(childStorageKeyPointer)).thenReturn(childStorageKeyBytes);
        when(hostApi.getDataFromMemory(keyPointer)).thenReturn(keyBytes);

        childStorageHostFunctions.extDefaultChildStorageClearPrefixVersion1(childStorageKeyPointer, keyPointer);

        verify(repository).deleteByPrefix(combinedKey, null);
    }

    @Test
    void extStorageClearPrefixVersion2WhenNotAllDeleted() {
        RuntimePointerSize limitPointer = mock(RuntimePointerSize.class);
        byte[] encodedLimit = new byte[]{1, 2, 0, 0, 0}; // Encoded option with value 2
        byte[] encodedResult = new byte[]{0, 2, 0, 0, 0}; // Result with remaining items and 2 deleted

        when(hostApi.getDataFromMemory(childStorageKeyPointer)).thenReturn(childStorageKeyBytes);
        when(hostApi.getDataFromMemory(keyPointer)).thenReturn(keyBytes);
        when(hostApi.getDataFromMemory(limitPointer)).thenReturn(encodedLimit);
        when(repository.deleteByPrefix(combinedKey, 2L)).thenReturn(new DeleteByPrefixResult(2, false));
        when(hostApi.writeDataToMemory(encodedResult)).thenReturn(resultPointer);

        RuntimePointerSize result = childStorageHostFunctions
                .extDefaultChildStorageClearPrefixVersion2(childStorageKeyPointer, keyPointer, limitPointer);

        assertEquals(resultPointer, result);
    }

    @Test
    void extStorageClearPrefixVersion2WhenAllDeleted() {
        RuntimePointerSize limitPointer = mock(RuntimePointerSize.class);
        byte[] encodedLimit = new byte[]{1, 4, 0, 0, 0}; // Encoded option with value 4
        byte[] encodedResult = new byte[]{1, 3, 0, 0, 0}; // Result with no remaining items and 3 deleted
        when(hostApi.getDataFromMemory(childStorageKeyPointer)).thenReturn(childStorageKeyBytes);
        when(hostApi.getDataFromMemory(keyPointer)).thenReturn(keyBytes);
        when(hostApi.getDataFromMemory(limitPointer)).thenReturn(encodedLimit);
        when(repository.deleteByPrefix(combinedKey, 4L)).thenReturn(new DeleteByPrefixResult(3, true));
        when(hostApi.writeDataToMemory(encodedResult)).thenReturn(resultPointer);

        RuntimePointerSize result = childStorageHostFunctions
                .extDefaultChildStorageClearPrefixVersion2(childStorageKeyPointer, keyPointer, limitPointer);

        assertEquals(resultPointer, result);
    }
    
    @Test
    void extStorageNextKeyVersion1WhenNextKeyExistsShouldReturnNextKeyAsOption() {
        String nextKey = "next key";
        when(hostApi.getDataFromMemory(childStorageKeyPointer)).thenReturn(childStorageKeyBytes);
        when(hostApi.getDataFromMemory(keyPointer)).thenReturn(keyBytes);
        when(repository.getNextKey(combinedKey)).thenReturn(Optional.of(nextKey));
        when(hostApi.writeDataToMemory(toOption(nextKey.getBytes()))).thenReturn(resultPointer);

        RuntimePointerSize result = childStorageHostFunctions
                .extDefaultChildStorageStorageNextKeyVersion1(childStorageKeyPointer, keyPointer);

        assertEquals(resultPointer, result);
    }

    @Test
    void extStorageNextKeyVersion1WhenNextKeyDoesNotExistsShouldReturnEmptyOption() {
        when(hostApi.getDataFromMemory(childStorageKeyPointer)).thenReturn(childStorageKeyBytes);
        when(hostApi.getDataFromMemory(keyPointer)).thenReturn(keyBytes);
        when(repository.getNextKey(combinedKey)).thenReturn(Optional.empty());
        when(hostApi.writeDataToMemory(emptyOption)).thenReturn(resultPointer);

        RuntimePointerSize result = childStorageHostFunctions
                .extDefaultChildStorageStorageNextKeyVersion1(childStorageKeyPointer, keyPointer);

        assertEquals(resultPointer, result);
    }

    private byte[] toOption(byte[] data) {
        return Bytes.concat(new byte[]{1}, data);
    }
}