package com.limechain.runtime.hostapi;

import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import com.limechain.utils.HashUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HashingHostFunctionsTest {
    byte[] data = {1, 2, 3};
    byte[] hashedData = {4, 5, 6};
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
        when(hostApi.writeDataToMemory(hashedData)).thenReturn(runtimePointerSize);

        try (var utils = mockStatic(HashUtils.class)) {
            utils.when(() -> HashUtils.hashWithKeccak256(data)).thenReturn(hashedData);
            int result = hashingHostFunctions.keccak256V1(dataPointer);

            assertEquals(runtimePointerSize.pointer(), result);
        }
    }

    @Test
    void keccak512V1() {
        when(hostApi.getDataFromMemory(dataPointer)).thenReturn(data);
        when(hostApi.writeDataToMemory(hashedData)).thenReturn(runtimePointerSize);

        try (var utils = mockStatic(HashUtils.class)) {
            utils.when(() -> HashUtils.hashWithKeccak512(data)).thenReturn(hashedData);
            int result = hashingHostFunctions.keccak512V1(dataPointer);

            assertEquals(runtimePointerSize.pointer(), result);
        }
    }

    @Test
    void sha2256V1() {
        when(hostApi.getDataFromMemory(dataPointer)).thenReturn(data);
        when(hostApi.writeDataToMemory(hashedData)).thenReturn(runtimePointerSize);

        try (var utils = mockStatic(HashUtils.class)) {
            utils.when(() -> HashUtils.hashWithSha256(data)).thenReturn(hashedData);
            int result = hashingHostFunctions.sha2256V1(dataPointer);

            assertEquals(runtimePointerSize.pointer(), result);
        }
    }

    @Test
    void blake2128V1() {
        when(hostApi.getDataFromMemory(dataPointer)).thenReturn(data);
        when(hostApi.writeDataToMemory(hashedData)).thenReturn(runtimePointerSize);

        try (var utils = mockStatic(HashUtils.class)) {
            utils.when(() -> HashUtils.hashWithBlake2b128(data)).thenReturn(hashedData);
            int result = hashingHostFunctions.blake2128V1(dataPointer);

            assertEquals(runtimePointerSize.pointer(), result);
        }
    }

    @Test
    void blake2256V1() {
        when(hostApi.getDataFromMemory(dataPointer)).thenReturn(data);
        when(hostApi.writeDataToMemory(hashedData)).thenReturn(runtimePointerSize);

        try (var utils = mockStatic(HashUtils.class)) {
            utils.when(() -> HashUtils.hashWithBlake2b(data)).thenReturn(hashedData);
            int result = hashingHostFunctions.blake2256V1(dataPointer);

            assertEquals(runtimePointerSize.pointer(), result);
        }
    }

    @Test
    void twox64V1() {
        when(hostApi.getDataFromMemory(dataPointer)).thenReturn(data);
        when(hostApi.writeDataToMemory(hashedData)).thenReturn(runtimePointerSize);

        try (var utils = mockStatic(HashUtils.class)) {
            utils.when(() -> HashUtils.hashXx64(0, data)).thenReturn(hashedData);
            int result = hashingHostFunctions.twox64V1(dataPointer);

            assertEquals(runtimePointerSize.pointer(), result);
        }
    }

    @Test
    void twox128V1() {
        when(hostApi.getDataFromMemory(dataPointer)).thenReturn(data);
        when(hostApi.writeDataToMemory(hashedData)).thenReturn(runtimePointerSize);

        try (var utils = mockStatic(HashUtils.class)) {
            utils.when(() -> HashUtils.hashXx128(0, data)).thenReturn(hashedData);
            int result = hashingHostFunctions.twox128V1(dataPointer);

            assertEquals(runtimePointerSize.pointer(), result);
        }
    }

    @Test
    void twox256V1() {
        when(hostApi.getDataFromMemory(dataPointer)).thenReturn(data);
        when(hostApi.writeDataToMemory(hashedData)).thenReturn(runtimePointerSize);

        try (var utils = mockStatic(HashUtils.class)) {
            utils.when(() -> HashUtils.hashXx256(0, data)).thenReturn(hashedData);
            int result = hashingHostFunctions.twox256V1(dataPointer);

            assertEquals(runtimePointerSize.pointer(), result);
        }
    }
}