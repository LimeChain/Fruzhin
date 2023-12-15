package com.limechain.runtime.hostapi;

import com.limechain.runtime.hostapi.dto.HttpResponseType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OffchainHttpRequestsTest {
    @Mock
    private Map<Integer, HttpURLConnection> requests;

    private OffchainHttpRequests offchainHttpRequests;

    @BeforeEach
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        MockitoAnnotations.initMocks(this);
        offchainHttpRequests = OffchainHttpRequests.getInstance();
        Field requestsField = OffchainHttpRequests.class.getDeclaredField("requests");
        requestsField.setAccessible(true);
        requestsField.set(offchainHttpRequests, requests);

    }

    @Test
    public void getRequestsResponsesTest() throws Exception {
        int[] requestIds = {1, 2};
        int timeout = 5000;

        HttpURLConnection mockConnection1 = mock(HttpURLConnection.class);
        HttpURLConnection mockConnection2 = mock(HttpURLConnection.class);

        when(requests.get(1)).thenReturn(mockConnection1);
        when(requests.get(2)).thenReturn(mockConnection2);

        when(mockConnection1.getResponseCode()).thenReturn(200);
        when(mockConnection2.getResponseCode()).thenReturn(200);

        HttpResponseType[] responses = offchainHttpRequests.getRequestsResponses(requestIds, timeout);

        assertEquals(HttpResponseType.FINISHED, responses[0]);
        assertEquals(HttpResponseType.FINISHED, responses[1]);

        verify(mockConnection1, times(1)).connect();
        verify(mockConnection2, times(1)).connect();
        verify(mockConnection1, times(1)).getResponseCode();
        verify(mockConnection2, times(1)).getResponseCode();
    }

}