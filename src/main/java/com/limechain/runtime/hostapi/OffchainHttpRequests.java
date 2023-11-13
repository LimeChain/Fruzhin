package com.limechain.runtime.hostapi;

import com.limechain.runtime.hostapi.dto.InvalidRequestId;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OffchainHttpRequests {
    private static OffchainHttpRequests INSTANCE;
    private short idCounter = 0;
    private final Map<Integer, HttpURLConnection> requests = new HashMap<>();

    public static OffchainHttpRequests getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new OffchainHttpRequests();
        }
        return INSTANCE;
    }

    public int addRequest(String method, String uri) throws IOException {
        int id = idCounter++;
        HttpURLConnection connection = (HttpURLConnection) new URL(uri).openConnection();
        connection.setRequestMethod(method);
        connection.setDoOutput(true);
        connection.setChunkedStreamingMode(0);
        requests.put(id, connection);
        return id;
    }

    public void addHeader(int id, String headerName, String headerValue) {
        HttpURLConnection request = requests.get(id);
        if (request == null) {
            throw new InvalidRequestId();
        }
        request.setRequestProperty(headerName, headerValue);
    }

    public void addRequestBodyChunk(int id, byte[] chunk, int timeout) throws IOException {
        HttpURLConnection request = requests.get(id);
        if (request == null) {
            throw new InvalidRequestId();
        }
        request.setConnectTimeout(timeout);
        request.getOutputStream().write(chunk);
    }
}
