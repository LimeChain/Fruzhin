package com.limechain.runtime.hostapi;

import com.limechain.config.HostConfig;
import com.limechain.network.protocol.blockannounce.NodeRole;
import com.limechain.runtime.hostapi.dto.HttpResponseType;
import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.writer.UInt64Writer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OffchainHostFunctionsTest {
    @InjectMocks
    private OffchainHostFunctions offchainHostFunctions;

    @Mock
    private OffchainHttpRequests requests;
    @Mock
    private HostApi hostApi;
    @Mock
    private HostConfig config;
    @Mock
    private RuntimePointerSize runtimePointerSize;
    @Mock
    private RuntimePointerSize resultPointer;
    @Mock
    private RuntimePointerSize deadlinePointer;
    @Mock
    private RuntimePointerSize bufferPointer;

    final int timeout = 10;
    final int requestId = 1;
    Map<String, List<String>> responseHeaders = new HashMap<>() {
        {
            put("Test", new ArrayList<>() {
                {
                    add("List");
                }
            });
        }
    };

    @Test
    void extOffchainIsValidatorWhenNodeRoleIsAuthoringShouldReturnOne() {
        when(config.getNodeRole()).thenReturn(NodeRole.AUTHORING);

        int result = offchainHostFunctions.extOffchainIsValidator();

        assertEquals(1, result);
    }

    @ParameterizedTest
    @EnumSource(value = NodeRole.class, names = {"AUTHORING"}, mode = EnumSource.Mode.EXCLUDE)
    void extOffchainIsValidatorWhenNodeRoleIsNotAuthoringShouldReturnZero(NodeRole nodeRole) {
        when(config.getNodeRole()).thenReturn(nodeRole);

        int result = offchainHostFunctions.extOffchainIsValidator();

        assertEquals(0, result);
    }

    @Test
    void extOffchainTimestampShouldReturnCurrentTimeFromInstant() {
        Instant instant = mock(Instant.class);
        long time = 123L;

        try (MockedStatic<Instant> mockedStatic = mockStatic(Instant.class)) {
            mockedStatic.when(Instant::now).thenReturn(instant);
            when(instant.toEpochMilli()).thenReturn(time);

            long result = offchainHostFunctions.extOffchainTimestamp();

            assertEquals(time, result);
        }
    }

    @Test
    void extOffchainRandomSeedReturnsAPointerToA32BitNumber() {
        int pointer = 123;
        when(hostApi.writeDataToMemory(argThat(argument -> argument.length == 32))).thenReturn(runtimePointerSize);
        when(runtimePointerSize.pointer()).thenReturn(pointer);

        int result = offchainHostFunctions.extOffchainRandomSeed();

        assertEquals(pointer, result);
    }

    @Test
    void extOffchainHttpResponseHeadersVersion1Test() {
        try {
            when(requests.getResponseHeaders(requestId)).thenReturn(responseHeaders);
            byte[] scaleEncodedHeaders = offchainHostFunctions.scaleEncodeHeaders(responseHeaders);
            when(hostApi.writeDataToMemory(scaleEncodedHeaders)).thenReturn(resultPointer);

            RuntimePointerSize result = offchainHostFunctions.extOffchainHttpResponseHeadersVersion1(requestId);
            assertEquals(resultPointer, result);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void extOffchainHttpResponseWaitVersion1Test() throws IOException, InterruptedException {
        int scaleOptionalTrue = 1;
        byte[] scaleEncodedRequestIds = new byte[]{4, requestId, 0, 0, 0};

        Instant instant = Instant.now();
        MockedStatic<Instant> mockedStatic = mockStatic(Instant.class);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ScaleCodecWriter writer = new ScaleCodecWriter(buf);
        UInt64Writer int64Writer = new UInt64Writer();
        writer.writeByte(scaleOptionalTrue);
        int64Writer.write(writer, new BigInteger(String.valueOf(instant.toEpochMilli()))
                .add(BigInteger.valueOf(timeout)));
        mockedStatic.when(Instant::now).thenReturn(instant);

        when(hostApi.getDataFromMemory(deadlinePointer)).thenReturn(buf.toByteArray());
        HttpResponseType[] expectedResponses = new HttpResponseType[]{HttpResponseType.FINISHED};
        when(requests.getRequestsResponses(new int[]{requestId}, timeout)).thenReturn(expectedResponses);

        when(hostApi.getDataFromMemory(runtimePointerSize)).thenReturn(scaleEncodedRequestIds);
        when(hostApi.writeDataToMemory(offchainHostFunctions
                .scaleEncodeArrayOfRequestStatuses(expectedResponses))).thenReturn(resultPointer);

        RuntimePointerSize result = offchainHostFunctions
                .extOffchainHttpResponseWaitVersion1(runtimePointerSize, deadlinePointer);
        assertEquals(resultPointer, result);
        mockedStatic.close();
    }

    @Test
    void extOffchainHttpResponseBodyVersion1ReturnsDeadlineReachedTest() throws InterruptedException, IOException {
        int deadlineReachedTimeout = -1;
        Instant instant = Instant.now();
        MockedStatic<Instant> mockedStatic = mockStatic(Instant.class);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ScaleCodecWriter writer = new ScaleCodecWriter(buf);
        UInt64Writer int64Writer = new UInt64Writer();
        int scaleOptionalTrue = 1;
        writer.writeByte(scaleOptionalTrue);
        int64Writer.write(writer, new BigInteger(String.valueOf(instant.toEpochMilli()))
                .add(BigInteger.valueOf(deadlineReachedTimeout)));

        mockedStatic.when(Instant::now).thenReturn(instant);
        when(hostApi.getDataFromMemory(deadlinePointer)).thenReturn(buf.toByteArray());
        when(requests.getRequestsResponses(new int[]{requestId}, deadlineReachedTimeout))
                .thenReturn(new HttpResponseType[]{HttpResponseType.DEADLINE_REACHED});
        when(hostApi.writeDataToMemory(HttpResponseType.DEADLINE_REACHED.scaleEncodedResult()))
                .thenReturn(resultPointer);

        RuntimePointerSize result = offchainHostFunctions.extOffchainHttpResponseReadBodyVersion1(requestId,
                bufferPointer,
                deadlinePointer);
        assertEquals(resultPointer, result);
        mockedStatic.close();
    }

    @Test
    void extOffchainHttpResponseBodyVersion1ReturnsIOErrorTest() throws IOException, InterruptedException {
        Instant instant = Instant.now();
        MockedStatic<Instant> mockedStatic = mockStatic(Instant.class);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ScaleCodecWriter writer = new ScaleCodecWriter(buf);
        UInt64Writer int64Writer = new UInt64Writer();
        int scaleOptionalTrue = 1;
        writer.writeByte(scaleOptionalTrue);
        int64Writer.write(writer, new BigInteger(String.valueOf(instant.toEpochMilli()))
                .add(BigInteger.valueOf(timeout)));

        mockedStatic.when(Instant::now).thenReturn(instant);

        when(hostApi.getDataFromMemory(deadlinePointer)).thenReturn(buf.toByteArray());
        when(requests.getRequestsResponses(new int[]{requestId}, timeout))
                .thenReturn(new HttpResponseType[]{HttpResponseType.IO_ERROR});

        when(hostApi.writeDataToMemory(HttpResponseType.IO_ERROR.scaleEncodedResult()))
                .thenReturn(resultPointer);

        RuntimePointerSize result = offchainHostFunctions.extOffchainHttpResponseReadBodyVersion1(requestId,
                bufferPointer,
                deadlinePointer);
        assertEquals(resultPointer, result);
        mockedStatic.close();
    }

    @Test
    void extOffchainHttpResponseBodyVersion1ReturnsInvalidIdTest() throws IOException, InterruptedException {
        Instant instant = Instant.now();

        MockedStatic<Instant> mockedStatic = mockStatic(Instant.class);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ScaleCodecWriter writer = new ScaleCodecWriter(buf);
        UInt64Writer int64Writer = new UInt64Writer();
        int scaleOptionalTrue = 1;
        writer.writeByte(scaleOptionalTrue);
        int64Writer.write(writer, new BigInteger(String.valueOf(instant.toEpochMilli()))
                .add(BigInteger.valueOf(timeout)));

        mockedStatic.when(Instant::now).thenReturn(instant);

        when(hostApi.getDataFromMemory(deadlinePointer)).thenReturn(buf.toByteArray());
        when(requests.getRequestsResponses(new int[]{requestId}, timeout))
                .thenReturn(new HttpResponseType[]{HttpResponseType.INVALID_ID});

        when(hostApi.writeDataToMemory(HttpResponseType.INVALID_ID.scaleEncodedResult()))
                .thenReturn(resultPointer);

        RuntimePointerSize result = offchainHostFunctions.extOffchainHttpResponseReadBodyVersion1(requestId,
                bufferPointer,
                deadlinePointer);
        assertEquals(resultPointer, result);
        mockedStatic.close();
    }

}