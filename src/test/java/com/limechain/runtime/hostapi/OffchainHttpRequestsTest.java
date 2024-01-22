package com.limechain.runtime.hostapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.HttpURLConnection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OffchainHttpRequestsTest {
    @InjectMocks
    private OffchainHttpRequests offchainHttpRequests;
    @Mock
    private HttpURLConnection connection;

    @Test
    void addRequest() throws IOException {
        String method = "POST";
        String uriProtocol = "https";
        String uriHost = "abc.com";

        offchainHttpRequests.addRequest(method, uriProtocol + "://" + uriHost);

        assertEquals(1, offchainHttpRequests.requests.size());
        HttpURLConnection connection = offchainHttpRequests.requests.get(0);
        assertEquals(method, connection.getRequestMethod());
        assertEquals(uriProtocol, connection.getURL().getProtocol());
        assertEquals(uriHost, connection.getURL().getHost());
    }

    @Test
    void addHeader() {
        int id = 123;
        offchainHttpRequests.requests.put(id, connection);
        String headerName = "hed";
        String headerValue = "val";

        offchainHttpRequests.addHeader(id, headerName, headerValue);

        verify(connection).setRequestProperty(headerName, headerValue);
    }

    /* NOTE: the following integration tests could be separated from the unit tests.
        The url calls can be mocked with a fake REST API, currently using: https://echo.zuplo.io/ */
    private final String echoUrl = "https://echo.zuplo.io/";

    @Test
    void sendBody() throws IOException {
        int id = offchainHttpRequests.addRequest("POST", echoUrl);
        String firstBodyPart = "bodypart1";
        String secondBodyPart = "BP2";

        offchainHttpRequests.addRequestBodyChunk(id, firstBodyPart.getBytes(), 0);
        offchainHttpRequests.addRequestBodyChunk(id, secondBodyPart.getBytes(), 0);

        JsonNode node = new ObjectMapper().readTree(offchainHttpRequests.getResponseBody(id));

        assertEquals(firstBodyPart + secondBodyPart, node.get("body").asText());
    }

    @Test
    void sendHeaders() throws IOException {
        String firstName = "testname1";
        String secondName = "tn2";
        String firstValue = "TestValue1";
        String secondValue = "TV2";
        int id = offchainHttpRequests.addRequest("GET", echoUrl);
        offchainHttpRequests.addHeader(id, firstName, firstValue);
        offchainHttpRequests.addHeader(id, secondName, secondValue);


        JsonNode node = new ObjectMapper().readTree(offchainHttpRequests.getResponseBody(id));

        assertEquals(firstValue, node.get("headers").get(firstName).asText());
        assertEquals(secondValue, node.get("headers").get(secondName).asText());
    }
}