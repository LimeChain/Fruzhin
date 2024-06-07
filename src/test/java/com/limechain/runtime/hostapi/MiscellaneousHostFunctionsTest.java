package com.limechain.runtime.hostapi;

import com.limechain.rpc.server.AppBean;
import com.limechain.runtime.SharedMemory;
import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import com.limechain.storage.crypto.KeyStore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MiscellaneousHostFunctionsTest {
    private final String value = "Test passed";
    private final String target = "MiscellaneousHostFunctionsTest:84";
    @InjectMocks
    private MiscellaneousHostFunctions miscellaneousHostFunctions;
    @Mock
    private SharedMemory sharedMemory;
    @Mock
    private Number number;
    @Mock
    private RuntimePointerSize valuePointer;
    @Mock
    private RuntimePointerSize targetPointer;

    @Test
    void printNumV1() {
        miscellaneousHostFunctions.printNumV1(number);
        verifyNoMoreInteractions(sharedMemory);
    }

    @Test
    void printUtf8V1() {
        when(sharedMemory.readData(valuePointer)).thenReturn(value.getBytes());

        miscellaneousHostFunctions.printUtf8V1(valuePointer);

        Mockito.verify(sharedMemory).readData(valuePointer);
        verifyNoMoreInteractions(sharedMemory);
    }

    @Test
    void printHexV1() {
        when(sharedMemory.readData(valuePointer)).thenReturn(value.getBytes());

        miscellaneousHostFunctions.printHexV1(valuePointer);

        Mockito.verify(sharedMemory).readData(valuePointer);
        verifyNoMoreInteractions(sharedMemory);
    }

    @Test
    void runtimeVersionV1() throws IOException {
        byte[] wasmRuntime = Files.readAllBytes(Paths.get("src","test","resources","runtime.wasm"));
        byte[] runtimeData = Files.readAllBytes(Paths.get("src","test","resources","runtime.data"));
        when(sharedMemory.readData(valuePointer)).thenReturn(wasmRuntime);
        when(sharedMemory.writeData(runtimeData)).thenReturn(targetPointer);

        try(MockedStatic<AppBean> appBeanMockedStatic = mockStatic(AppBean.class)){
            appBeanMockedStatic.when(() -> AppBean.getBean(KeyStore.class)).thenReturn(mock(KeyStore.class));

            RuntimePointerSize result = miscellaneousHostFunctions.runtimeVersionV1(valuePointer);

            assertEquals(targetPointer, result);
            verify(sharedMemory).readData(valuePointer);
            verify(sharedMemory).writeData(runtimeData);
            verifyNoMoreInteractions(sharedMemory);
        }
    }

    @Test
    void logV1() {
        when(sharedMemory.readData(valuePointer)).thenReturn(value.getBytes());
        when(sharedMemory.readData(targetPointer)).thenReturn(target.getBytes());

        miscellaneousHostFunctions.logV1(1, targetPointer, valuePointer);

        verify(sharedMemory).readData(valuePointer);
        verify(sharedMemory).readData(targetPointer);
        verifyNoMoreInteractions(sharedMemory);
    }

    @Test
    void maxLevelV1() {
        assertEquals(4, miscellaneousHostFunctions.maxLevelV1());
        verifyNoMoreInteractions(sharedMemory);
    }

}