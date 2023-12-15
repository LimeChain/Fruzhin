package com.limechain.runtime.hostapi;

import com.limechain.runtime.hostapi.dto.HttpResponseType;
import com.limechain.runtime.hostapi.dto.InvalidRequestId;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Log
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

    public Map<String, List<String>> getResponseHeaders(int id) throws IOException {
        HttpURLConnection request = requests.get(id);
        if (request.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return request.getHeaderFields();
        }
        return null;
    }

    public byte[] getResponseBody(int id) throws IOException {
        HttpURLConnection request = requests.get(id);
        if (request.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return request.getResponseMessage().getBytes();
        }
        return null;
    }

    public HttpResponseType[] getRequestsResponses(int[] requestIds, int timeout) throws InterruptedException {
        HttpResponseType[] requestStatuses = new HttpResponseType[requestIds.length];

        ExecutorService executor = Executors.newFixedThreadPool(requestIds.length);
        long startTime = System.currentTimeMillis();

        try {
            Future<?>[] futures = new Future<?>[requestIds.length];
            for (int i = 0; i < requestIds.length; i++) {
                final int index = i;
                futures[i] = executor.submit(() -> {
                    try {
                        HttpURLConnection request = requests.get(requestIds[index]);
                        request.setConnectTimeout(timeout);
                        request.connect();

                        long timeElapsed = System.currentTimeMillis() - startTime;
                        long remainingTime = timeout - timeElapsed;
                        if (remainingTime > 0) {
                            request.setReadTimeout((int) remainingTime);
                        } else {
                            throw new SocketTimeoutException("Timeout while connecting");
                        }
                        request.getResponseCode();
                        requestStatuses[index] = HttpResponseType.FINISHED;
                    } catch (InvalidRequestId e) {
                        requestStatuses[index] = HttpResponseType.INVALID_ID;
                    } catch (SocketTimeoutException e) {
                        requestStatuses[index] = HttpResponseType.DEADLINE_REACHED;
                    } catch (IOException e) {
                        requestStatuses[index] = HttpResponseType.IO_ERROR;
                    }
                });
            }

            executor.shutdown();
            executor.awaitTermination(timeout, TimeUnit.MILLISECONDS);

            for (Future<?> future : futures) {
                if (!future.isDone()) {
                    future.cancel(true);
                }
            }
        } finally {
            if (!executor.isTerminated()) {
                executor.shutdownNow();
            }
        }
        return requestStatuses;
    }
}
