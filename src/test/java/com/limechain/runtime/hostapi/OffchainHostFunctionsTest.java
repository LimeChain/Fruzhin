package com.limechain.runtime.hostapi;

import com.limechain.config.HostConfig;
import com.limechain.network.protocol.blockannounce.NodeRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OffchainHostFunctionsTest {
    @InjectMocks
    private OffchainHostFunctions offchainHostFunctions;

    @Mock
    private HostApi hostApi;
    @Mock
    private HostConfig config;

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
}