package com.limechain.runtime.hostapi;

import com.limechain.rpc.server.AppBean;
import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import com.limechain.storage.crypto.KeyStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
    private HostApi hostApi;
    @Mock
    private Number number;
    @Mock
    private RuntimePointerSize valuePointer;
    @Mock
    private RuntimePointerSize targetPointer;

    @Test
    void printNumV1() {

        miscellaneousHostFunctions.printNumV1(number);
        verifyNoMoreInteractions(hostApi);

    }

    @Test
    void printUtf8V1() {
        when(hostApi.getDataFromMemory(valuePointer)).thenReturn(value.getBytes());

        miscellaneousHostFunctions.printUtf8V1(valuePointer);

        Mockito.verify(hostApi).getDataFromMemory(valuePointer);
        verifyNoMoreInteractions(hostApi);
    }

    @Test
    void printHexV1() {
        when(hostApi.getDataFromMemory(valuePointer)).thenReturn(value.getBytes());

        miscellaneousHostFunctions.printHexV1(valuePointer);

        Mockito.verify(hostApi).getDataFromMemory(valuePointer);
        verifyNoMoreInteractions(hostApi);
    }

    @Test
    void runtimeVersionV1() throws IOException {
        byte[] wasmRuntime = Files.readAllBytes(Path.of("./src/test/resources/runtime.wasm"));
        byte[] runtimeData = Files.readAllBytes(Path.of("./src/test/resources/runtime.data"));
        when(hostApi.getDataFromMemory(valuePointer)).thenReturn(wasmRuntime);
        when(hostApi.writeDataToMemory(runtimeData)).thenReturn(targetPointer);

        try(MockedStatic<AppBean> appBeanMockedStatic = mockStatic(AppBean.class)){
            appBeanMockedStatic.when(() -> AppBean.getBean(KeyStore.class)).thenReturn(mock(KeyStore.class));

            RuntimePointerSize result = miscellaneousHostFunctions.runtimeVersionV1(valuePointer);

            assertEquals(targetPointer, result);
            verify(hostApi).getDataFromMemory(valuePointer);
            verify(hostApi).writeDataToMemory(runtimeData);
            verifyNoMoreInteractions(hostApi);
        }
    }

    @Test
    void logV1() {
        when(hostApi.getDataFromMemory(valuePointer)).thenReturn(value.getBytes());
        when(hostApi.getDataFromMemory(targetPointer)).thenReturn(target.getBytes());

        miscellaneousHostFunctions.logV1(1, targetPointer, valuePointer);

        verify(hostApi).getDataFromMemory(valuePointer);
        verify(hostApi).getDataFromMemory(targetPointer);
        verifyNoMoreInteractions(hostApi);
    }

    @Test
    void maxLevelV1() {
        assertEquals(4, miscellaneousHostFunctions.maxLevelV1());
        verifyNoMoreInteractions(hostApi);
    }

}