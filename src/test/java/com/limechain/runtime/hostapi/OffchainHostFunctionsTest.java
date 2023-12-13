package com.limechain.runtime.hostapi;

import com.limechain.config.HostConfig;
import com.limechain.network.protocol.blockannounce.NodeRole;
import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

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
    final byte[] encodedDeadline = new byte[]{1, 127, 127, 127, 127, 127, 127, 127, 127};
    final int requestId = 1;
    final byte[] responseBody = new byte[]{1, 2, 3};
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
}