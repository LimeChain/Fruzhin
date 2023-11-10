package com.limechain.runtime.hostapi;

import com.limechain.config.HostConfig;
import com.limechain.network.protocol.blockannounce.NodeRole;
import com.limechain.runtime.hostapi.dto.InvalidArgumentException;
import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import com.limechain.storage.offchain.OffchainStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OffchainHostFunctionsTest {
    private OffchainHostFunctions offchainHostFunctions;

    @Mock
    private HostApi hostApi;
    @Mock
    private HostConfig config;
    @Mock
    private OffchainStore persistentStorage;
    @Mock
    private OffchainStore localStorage;

    @BeforeEach
    void setup() {
        offchainHostFunctions = new OffchainHostFunctions(hostApi, config, persistentStorage, localStorage);
    }

    @Test
    void extOffchainIsValidatorWhenNodeRoleIsAuthoringShouldReturnOne() {
        when(config.getNodeRole()).thenReturn(NodeRole.AUTHORING);

        int result = offchainHostFunctions.extOffchainIsValidator();

        assertEquals(1, result);
    }

    @ParameterizedTest
    @EnumSource(value = NodeRole.class, names = { "AUTHORING" }, mode = EnumSource.Mode.EXCLUDE)
    void extOffchainIsValidatorWhenNodeRoleIsNotAuthoringShouldReturnZero(NodeRole nodeRole) {
        when(config.getNodeRole()).thenReturn(nodeRole);

        int result = offchainHostFunctions.extOffchainIsValidator();

        assertEquals(0, result);
    }

    @Test
    void extOffchainTimestampShouldReturnCurrentTimeFromInstant() {
        Instant instant = mock(Instant.class);
        long time = 123L;

        try(MockedStatic<Instant> mockedStatic = mockStatic(Instant.class)) {
            mockedStatic.when(Instant::now).thenReturn(instant);
            when(instant.toEpochMilli()).thenReturn(time);

            long result = offchainHostFunctions.extOffchainTimestamp();

            assertEquals(time, result);
        }
    }

    @Test
    void extOffchainRandomSeedReturnsAPointerToA32BitNumber() {
        RuntimePointerSize runtimePointerSize = mock(RuntimePointerSize.class);
        int pointer = 123;
        when(hostApi.writeDataToMemory(argThat(argument -> argument.length == 32))).thenReturn(runtimePointerSize);
        when(runtimePointerSize.pointer()).thenReturn(pointer);

        int result = offchainHostFunctions.extOffchainRandomSeed();

        assertEquals(pointer, result);
    }

    @Test
    void extOffchainLocalStorageSetWhenKindIs1ShouldStoreKeyValueInPersistentStorage() {
        RuntimePointerSize keyPointer = mock(RuntimePointerSize.class);
        RuntimePointerSize valuePointer = mock(RuntimePointerSize.class);
        String key = "key";
        byte[] value = new byte[] {1,2,3};
        when(hostApi.getDataFromMemory(keyPointer)).thenReturn(key.getBytes());
        when(hostApi.getDataFromMemory(valuePointer)).thenReturn(value);

        offchainHostFunctions.extOffchainLocalStorageSet(1, keyPointer, valuePointer);

        verify(persistentStorage).set(key, value);
    }

    @Test
    void extOffchainLocalStorageSetWhenKindIs2ShouldStoreKeyValueInLocalStorage() {
        RuntimePointerSize keyPointer = mock(RuntimePointerSize.class);
        RuntimePointerSize valuePointer = mock(RuntimePointerSize.class);
        String key = "key";
        byte[] value = new byte[] {1,2,3};
        when(hostApi.getDataFromMemory(keyPointer)).thenReturn(key.getBytes());
        when(hostApi.getDataFromMemory(valuePointer)).thenReturn(value);

        offchainHostFunctions.extOffchainLocalStorageSet(2, keyPointer, valuePointer);

        verify(localStorage).set(key, value);
    }

    @Test
    void extOffchainLocalStorageSetWhenKindIsNot1Or2ShouldThrow() {
        RuntimePointerSize keyPointer = mock(RuntimePointerSize.class);
        RuntimePointerSize valuePointer = mock(RuntimePointerSize.class);

        assertThrows(InvalidArgumentException.class, () ->
            offchainHostFunctions.extOffchainLocalStorageSet(0, keyPointer, valuePointer)
        );
    }

    @Test
    void extOffchainLocalStorageClearWhenKindIs1ShouldClearKeyInPersistentStorage() {
        RuntimePointerSize keyPointer = mock(RuntimePointerSize.class);
        String key = "key";
        when(hostApi.getDataFromMemory(keyPointer)).thenReturn(key.getBytes());

        offchainHostFunctions.extOffchainLocalStorageClear(1, keyPointer);

        verify(persistentStorage).remove(key);
    }

    @Test
    void extOffchainLocalStorageClearWhenKindIs2ShouldClearKeyInLocalStorage() {
        RuntimePointerSize keyPointer = mock(RuntimePointerSize.class);
        String key = "key";
        when(hostApi.getDataFromMemory(keyPointer)).thenReturn(key.getBytes());

        offchainHostFunctions.extOffchainLocalStorageClear(2, keyPointer);

        verify(localStorage).remove(key);
    }

    @Test
    void extOffchainLocalStorageClearWhenKindIsNot1Or2ShouldThrow() {
        RuntimePointerSize keyPointer = mock(RuntimePointerSize.class);

        assertThrows(InvalidArgumentException.class, () ->
                offchainHostFunctions.extOffchainLocalStorageClear(0, keyPointer)
        );
    }

    @Test
    void extOffchainLocalStorageCompareAndSetWhenKindIs1AndPersistentStorageSetIsSuccessfulReturn1() {
        RuntimePointerSize keyPointer = mock(RuntimePointerSize.class);
        RuntimePointerSize oldValuePointer = mock(RuntimePointerSize.class);
        RuntimePointerSize newValuePointer = mock(RuntimePointerSize.class);

        String key = "key";
        byte[] oldValue = new byte[] {1,2,3};
        byte[] oldValueOption = new byte[] {1,12,1,2,3}; // 1 - non-empty, 12 - compact value of 3 (size of the value)
        byte[] newValue = new byte[] {4,5,6};
        when(hostApi.getDataFromMemory(keyPointer)).thenReturn(key.getBytes());
        when(hostApi.getDataFromMemory(oldValuePointer)).thenReturn(oldValueOption);
        when(hostApi.getDataFromMemory(newValuePointer)).thenReturn(newValue);
        when(persistentStorage.compareAndSet(key, oldValue, newValue)).thenReturn(true);

        int result = offchainHostFunctions.extOffchainLocalStorageCompareAndSet(1, keyPointer,
                oldValuePointer, newValuePointer);

        assertEquals(1, result);
    }

    @Test
    void extOffchainLocalStorageCompareAndSetWhenKindIs2AndLocalStorageSetIsSuccessfulReturn1() {
        RuntimePointerSize keyPointer = mock(RuntimePointerSize.class);
        RuntimePointerSize oldValuePointer = mock(RuntimePointerSize.class);
        RuntimePointerSize newValuePointer = mock(RuntimePointerSize.class);

        String key = "key";
        byte[] oldValue = new byte[] {1,2,3};
        byte[] oldValueOption = new byte[] {1,12,1,2,3}; // 1 - non-empty, 12 - compact value of 3 (size of the value)
        byte[] newValue = new byte[] {4,5,6};
        when(hostApi.getDataFromMemory(keyPointer)).thenReturn(key.getBytes());
        when(hostApi.getDataFromMemory(oldValuePointer)).thenReturn(oldValueOption);
        when(hostApi.getDataFromMemory(newValuePointer)).thenReturn(newValue);
        when(localStorage.compareAndSet(key, oldValue, newValue)).thenReturn(true);

        int result = offchainHostFunctions.extOffchainLocalStorageCompareAndSet(2, keyPointer,
                oldValuePointer, newValuePointer);

        assertEquals(1, result);
    }

    @Test
    void extOffchainLocalStorageCompareAndSetWhenKindIs1AndPersistentStorageSetFailsReturn0() {
        RuntimePointerSize keyPointer = mock(RuntimePointerSize.class);
        RuntimePointerSize oldValuePointer = mock(RuntimePointerSize.class);
        RuntimePointerSize newValuePointer = mock(RuntimePointerSize.class);

        String key = "key";
        byte[] oldValue = new byte[] {1,2,3};
        byte[] oldValueOption = new byte[] {1,12,1,2,3}; // 1 - non-empty, 12 - compact value of 3 (size of the value)
        byte[] newValue = new byte[] {4,5,6};
        when(hostApi.getDataFromMemory(keyPointer)).thenReturn(key.getBytes());
        when(hostApi.getDataFromMemory(oldValuePointer)).thenReturn(oldValueOption);
        when(hostApi.getDataFromMemory(newValuePointer)).thenReturn(newValue);
        when(persistentStorage.compareAndSet(key, oldValue, newValue)).thenReturn(false);

        int result = offchainHostFunctions.extOffchainLocalStorageCompareAndSet(1, keyPointer,
                oldValuePointer, newValuePointer);

        assertEquals(0, result);
    }

    @Test
    void extOffchainLocalStorageCompareAndSetWhenKindIs2AndLocalStorageSetFailsReturn0() {
        RuntimePointerSize keyPointer = mock(RuntimePointerSize.class);
        RuntimePointerSize oldValuePointer = mock(RuntimePointerSize.class);
        RuntimePointerSize newValuePointer = mock(RuntimePointerSize.class);

        String key = "key";
        byte[] oldValue = new byte[] {1,2,3};
        byte[] oldValueOption = new byte[] {1,12,1,2,3}; // 1 - non-empty, 12 - compact value of 3 (size of the value)
        byte[] newValue = new byte[] {4,5,6};
        when(hostApi.getDataFromMemory(keyPointer)).thenReturn(key.getBytes());
        when(hostApi.getDataFromMemory(oldValuePointer)).thenReturn(oldValueOption);
        when(hostApi.getDataFromMemory(newValuePointer)).thenReturn(newValue);
        when(localStorage.compareAndSet(key, oldValue, newValue)).thenReturn(false);

        int result = offchainHostFunctions.extOffchainLocalStorageCompareAndSet(2, keyPointer,
                oldValuePointer, newValuePointer);

        assertEquals(0, result);
    }

    @Test
    void extOffchainLocalStorageCompareAndSetWhenKindIsNot1Or2ShouldThrow() {
        RuntimePointerSize keyPointer = mock(RuntimePointerSize.class);
        RuntimePointerSize oldValuePointer = mock(RuntimePointerSize.class);
        RuntimePointerSize newValuePointer = mock(RuntimePointerSize.class);

        assertThrows(InvalidArgumentException.class, () ->
                offchainHostFunctions.extOffchainLocalStorageCompareAndSet(0,
                        keyPointer, oldValuePointer, newValuePointer)
        );
    }

    @Test
    void extOffchainLocalStorageGetWhenKindIs1ShouldGetValueFromPersistentStorage() {
        RuntimePointerSize keyPointer = mock(RuntimePointerSize.class);
        RuntimePointerSize valuePointer = mock(RuntimePointerSize.class);
        String key = "key";
        byte[] value = new byte[] {1,2,3};
        byte[] valueAsOption = new byte[] {1,1,2,3};
        when(hostApi.getDataFromMemory(keyPointer)).thenReturn(key.getBytes());
        when(persistentStorage.get(key)).thenReturn(value);
        when(hostApi.writeDataToMemory(valueAsOption)).thenReturn(valuePointer);

        RuntimePointerSize result = offchainHostFunctions.extOffchainLocalStorageGet(1, keyPointer);

        assertEquals(valuePointer, result);
    }

    @Test
    void extOffchainLocalStorageGetWhenKindIs2ShouldGetValueFromLocalStorage() {
        RuntimePointerSize keyPointer = mock(RuntimePointerSize.class);
        RuntimePointerSize valuePointer = mock(RuntimePointerSize.class);
        String key = "key";
        byte[] value = new byte[] {1,2,3};
        byte[] valueAsOption = new byte[] {1,1,2,3};
        when(hostApi.getDataFromMemory(keyPointer)).thenReturn(key.getBytes());
        when(localStorage.get(key)).thenReturn(value);
        when(hostApi.writeDataToMemory(valueAsOption)).thenReturn(valuePointer);

        RuntimePointerSize result = offchainHostFunctions.extOffchainLocalStorageGet(2, keyPointer);

        assertEquals(valuePointer, result);
    }

    @Test
    void extOffchainLocalStorageGetWhenKindIsNot1Or2ShouldThrow() {
        RuntimePointerSize keyPointer = mock(RuntimePointerSize.class);

        assertThrows(InvalidArgumentException.class, () ->
                offchainHostFunctions.extOffchainLocalStorageGet(0, keyPointer)
        );
    }

    @Test
    void offchainIndexSetShouldSetInPersistentStorage() {
        RuntimePointerSize keyPointer = mock(RuntimePointerSize.class);
        RuntimePointerSize valuePointer = mock(RuntimePointerSize.class);
        String key = "key";
        byte[] value = new byte[] {1,2,3};
        when(hostApi.getDataFromMemory(keyPointer)).thenReturn(key.getBytes());
        when(hostApi.getDataFromMemory(valuePointer)).thenReturn(value);

        offchainHostFunctions.offchainIndexSet(keyPointer, valuePointer);

        verify(persistentStorage).set(key, value);
    }

    @Test
    void offchainIndexClearShouldRemoveKeyFromPersistentStorage() {
        RuntimePointerSize keyPointer = mock(RuntimePointerSize.class);
        String key = "key";
        when(hostApi.getDataFromMemory(keyPointer)).thenReturn(key.getBytes());

        offchainHostFunctions.offchainIndexClear(keyPointer);

        verify(persistentStorage).remove(key);
    }
}