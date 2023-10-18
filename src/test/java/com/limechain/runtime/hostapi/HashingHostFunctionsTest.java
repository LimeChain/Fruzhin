package com.limechain.runtime.hostapi;

import com.limechain.utils.HashUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HashingHostFunctionsTest {
    byte[] data = "00000000".getBytes();
    @InjectMocks
    private HashingHostFunctions hashingHostFunctions;
    @Mock
    private HostApi hostApi;
    @Mock
    private RuntimePointerSize dataPointer;
    @Spy
    private RuntimePointerSize runtimePointerSize = new RuntimePointerSize(1, 1);

    @Test
    void keccak256V1() {
        when(hostApi.getDataFromMemory(dataPointer)).thenReturn(data);
        when(hostApi.addDataToMemory(any())).thenReturn(runtimePointerSize);

        try(var utils = mockStatic(HashUtils.class)){
            int result = hashingHostFunctions.keccak256V1(dataPointer);

            utils.verify(() -> HashUtils.hashWithKeccak256(data));
            assertEquals(runtimePointerSize.pointer(), result);
        }
    }

    @Test
    void keccak512V1() {
        when(hostApi.getDataFromMemory(dataPointer)).thenReturn(data);
        when(hostApi.addDataToMemory(any())).thenReturn(runtimePointerSize);

        try(var utils = mockStatic(HashUtils.class)){
            int result = hashingHostFunctions.keccak512V1(dataPointer);

            utils.verify(() -> HashUtils.hashWithKeccak512(data));
            assertEquals(runtimePointerSize.pointer(), result);
        }
    }

    @Test
    void sha2256V1() {
        when(hostApi.getDataFromMemory(dataPointer)).thenReturn(data);
        when(hostApi.addDataToMemory(any())).thenReturn(runtimePointerSize);

        try(var utils = mockStatic(HashUtils.class)){
            int result = hashingHostFunctions.sha2256V1(dataPointer);

            utils.verify(() -> HashUtils.hashWithSha256(data));
            assertEquals(runtimePointerSize.pointer(), result);
        }
    }

    @Test
    void blake2128V1() {
        when(hostApi.getDataFromMemory(dataPointer)).thenReturn(data);
        when(hostApi.addDataToMemory(any())).thenReturn(runtimePointerSize);

        try(var utils = mockStatic(HashUtils.class)){
            int result = hashingHostFunctions.blake2128V1(dataPointer);

            utils.verify(() -> HashUtils.hashWithBlake2b128(data));
            assertEquals(runtimePointerSize.pointer(), result);
        }
    }

    @Test
    void blake2256V1() {
        when(hostApi.getDataFromMemory(dataPointer)).thenReturn(data);
        when(hostApi.addDataToMemory(any())).thenReturn(runtimePointerSize);

        try(var utils = mockStatic(HashUtils.class)){
            int result = hashingHostFunctions.blake2256V1(dataPointer);

            utils.verify(() -> HashUtils.hashWithBlake2b(data));
            assertEquals(runtimePointerSize.pointer(), result);
        }
    }

    @Test
    void twox64V1() {
        when(hostApi.getDataFromMemory(dataPointer)).thenReturn(data);
        when(hostApi.addDataToMemory(any())).thenReturn(runtimePointerSize);

        try(var utils = mockStatic(HashUtils.class)){
            int result = hashingHostFunctions.twox64V1(dataPointer);

            utils.verify(() -> HashUtils.hashXx64(0, data));
            assertEquals(runtimePointerSize.pointer(), result);
        }
    }

    @Test
    void twox128V1() {
        when(hostApi.getDataFromMemory(dataPointer)).thenReturn(data);
        when(hostApi.addDataToMemory(any())).thenReturn(runtimePointerSize);

        try(var utils = mockStatic(HashUtils.class)){
            int result = hashingHostFunctions.twox128V1(dataPointer);

            utils.verify(() -> HashUtils.hashXx128(0, data));
            assertEquals(runtimePointerSize.pointer(), result);
        }
    }

    @Test
    void twox256V1() {
        when(hostApi.getDataFromMemory(dataPointer)).thenReturn(data);
        when(hostApi.addDataToMemory(any())).thenReturn(runtimePointerSize);

        try(var utils = mockStatic(HashUtils.class)){
            int result = hashingHostFunctions.twox256V1(dataPointer);

            utils.verify(() -> HashUtils.hashXx256(0, data));
            assertEquals(runtimePointerSize.pointer(), result);
        }
    }
}