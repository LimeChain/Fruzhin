package com.limechain.runtime.hostapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OffchainHttpRequestsTest {
    @InjectMocks
    private OffchainHttpRequests offchainHttpRequests;
    @Mock
    private HttpURLConnection connection;

    private final String echoUrl = "https://echo.zuplo.io/";

    @Test
    void addRequest() throws IOException {
        String method = "POST";
        String uriProtocol = "https";
        String uriHost = "abc.com";
        offchainHttpRequests.addRequest(method, uriProtocol + "://" + uriHost);

        assertEquals(offchainHttpRequests.requests.size(), 1);
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

    // NOTE: the following integration tests could be separated from the unit tests.
    // The url calls can be mocked with a fake REST API, currently using: https://www.jsontest.com/#ip
    @Test
    void receiveResponseBody() throws IOException {
        String firstKey = "TestKey1";
        String secondKey = "TK2";
        String firstValue = "TestValue1";
        String secondValue = "TV2";
        String url = String.format("http://echo.jsontest.com/%s/%s/%s/%s", firstKey, firstValue, secondKey, secondValue);
        int id = offchainHttpRequests.addRequest("POST", url);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(offchainHttpRequests.getResponseBody(id));

        assertEquals(firstValue, node.get(firstKey).asText());
        assertEquals(secondValue, node.get(secondKey).asText());
    }

    @Test
    void receiveResponseHeaders() throws IOException {
        String firstName = "TestName1";
        String secondName = "TN2";
        String firstValue = "TestValue1";
        String secondValue = "TV2";
        int id = offchainHttpRequests.addRequest("GET", "http://headers.jsontest.com/");
        offchainHttpRequests.addHeader(id, firstName, firstValue);
        offchainHttpRequests.addHeader(id, secondName, secondValue);

        Map<String, List<String>> responseHeaders = offchainHttpRequests.getResponseHeaders(id);

        assertEquals(firstValue, responseHeaders.get(firstName).get(0));
        assertEquals(secondValue, responseHeaders.get(secondName).get(0));
    }

    @Test
    @Disabled
    // TODO: fix
    void sendBody() throws IOException {
        int id = offchainHttpRequests.addRequest("POST", echoUrl);
        offchainHttpRequests.addRequestBodyChunk(id, new byte[] { 1, 2, 3}, 0);
        offchainHttpRequests.addRequestBodyChunk(id, new byte[] { 4, 2, 1 }, 0);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(offchainHttpRequests.getResponseBody(id));

        assertEquals(new String(new byte[] { 1,2,3,4,2,1}), node.get("body").asText());
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


        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(offchainHttpRequests.getResponseBody(id));

        assertEquals(firstValue, node.get("headers").get(firstName).asText());
    }
}