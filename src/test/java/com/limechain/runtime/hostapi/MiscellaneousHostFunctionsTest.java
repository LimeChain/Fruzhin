package com.limechain.runtime.hostapi;

import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import org.apache.tomcat.util.buf.HexUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        PrintStream mockedPrintStream = Mockito.mock(PrintStream.class);
        PrintStream old = System.out;
        System.setOut(mockedPrintStream);

        miscellaneousHostFunctions.printNumV1(number);

        Mockito.verify(mockedPrintStream).println(number);
        System.setOut(old);
    }

    @Test
    void printUtf8V1() {
        when(hostApi.getDataFromMemory(valuePointer)).thenReturn(value.getBytes());

        PrintStream mockedPrintStream = Mockito.mock(PrintStream.class);
        PrintStream old = System.out;
        System.setOut(mockedPrintStream);

        miscellaneousHostFunctions.printUtf8V1(valuePointer);

        Mockito.verify(hostApi).getDataFromMemory(valuePointer);
        Mockito.verify(mockedPrintStream).println(value);
        System.setOut(old);
    }

    @Test
    void printHexV1() {
        when(hostApi.getDataFromMemory(valuePointer)).thenReturn(value.getBytes());

        PrintStream mockedPrintStream = Mockito.mock(PrintStream.class);
        PrintStream old = System.out;
        System.setOut(mockedPrintStream);

        miscellaneousHostFunctions.printHexV1(valuePointer);

        Mockito.verify(hostApi).getDataFromMemory(valuePointer);
        Mockito.verify(mockedPrintStream).println(HexUtils.toHexString(value.getBytes()));
        System.setOut(old);
    }

    @Test
    @Disabled("needs working allocator api")
    void runtimeVersionV1() throws IOException {
        byte[] wasmRuntime = Files.readAllBytes(Path.of("./src/test/resources/runtime.wasm"));
        byte[] runtimeData = Files.readAllBytes(Path.of("./src/test/resources/runtime.data"));
        when(hostApi.getDataFromMemory(valuePointer)).thenReturn(wasmRuntime);
        when(hostApi.writeDataToMemory(runtimeData)).thenReturn(targetPointer);

        RuntimePointerSize result = miscellaneousHostFunctions.runtimeVersionV1(valuePointer);

        assertEquals(targetPointer, result);
        verify(hostApi).getDataFromMemory(valuePointer);
        verify(hostApi).writeDataToMemory(runtimeData);
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
    }

}