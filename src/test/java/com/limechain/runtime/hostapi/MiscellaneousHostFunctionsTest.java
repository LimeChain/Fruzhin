package com.limechain.runtime.hostapi;

import org.apache.tomcat.util.buf.HexUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintStream;

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
    void runtimeVersionV1() {
        //todo: test
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

    @Test
    void extPanicHandlerAbortOnPanicVersion1() {
        when(hostApi.getDataFromMemory(valuePointer)).thenReturn(value.getBytes());

        miscellaneousHostFunctions.extPanicHandlerAbortOnPanicVersion1(valuePointer);

        verify(hostApi).getDataFromMemory(valuePointer);
        verifyNoMoreInteractions(hostApi);
    }
}