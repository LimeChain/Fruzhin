package com.limechain.runtime.hostapi;

import com.limechain.exception.hostapi.SocketTimeoutException;
import com.limechain.runtime.hostapi.dto.HttpErrorType;
import com.limechain.runtime.hostapi.dto.HttpStatusCode;
import com.limechain.runtime.hostapi.dto.InvalidRequestId;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Log
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OffchainHttpRequests {
    private static OffchainHttpRequests instance;
    private short idCounter = 0;
    protected final Map<Integer, HttpURLConnection> requests = new HashMap<>();

    public static OffchainHttpRequests getInstance() {
        if (instance == null) {
            instance = new OffchainHttpRequests();
        }
        return instance;
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
        HttpURLConnection request = getExistingRequest(id);
        request.setRequestProperty(headerName, headerValue);
    }

    public void addRequestBodyChunk(int id, byte[] chunk, int timeout) throws IOException {
        HttpURLConnection request = getExistingRequest(id);
        request.setConnectTimeout(timeout);

        if (chunk.length == 0) {
            request.connect();
        } else {
            request.getOutputStream().write(chunk);
        }
    }

    public Map<String, List<String>> getResponseHeaders(int id) throws IOException {
        HttpURLConnection request = getExistingRequest(id);
        if (request.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return request.getHeaderFields();
        }
        return new HashMap<>();
    }

    public byte[] getResponseBody(int id) throws IOException {
        HttpURLConnection request = getExistingRequest(id);
        if (request.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return request.getInputStream().readAllBytes();
        }
        return new byte[]{};
    }

    public byte[] readResponseBody(int id, int bytes) throws IOException {
        HttpURLConnection request = getExistingRequest(id);
        if (request.getResponseCode() != HttpURLConnection.HTTP_OK) {
            return new byte[]{};
        }
        return request.getInputStream().readNBytes(bytes);
    }

    public HttpStatusCode[] getRequestsResponses(int[] requestIds, int timeout) throws InterruptedException {
        HttpStatusCode[] requestStatuses = new HttpStatusCode[requestIds.length];

        ExecutorService executor = Executors.newFixedThreadPool(requestIds.length);
        try {
            Future<?>[] futures = new Future<?>[requestIds.length];
            for (int i = 0; i < requestIds.length; i++) {
                final int index = i;
                futures[i] = executor.submit(() -> {
                    long startTime = Instant.now().toEpochMilli();
                    requestStatuses[index] = executeRequest(requestIds[index], timeout, startTime);
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

    public HttpStatusCode executeRequest(int requestId, int timeout, long startTime) {
        try {
            HttpURLConnection request = getExistingRequest(requestId);
            int statusCode = waitRequestResponseWithTimeout(request, timeout, startTime);
            return HttpStatusCode.success(statusCode);
        } catch (InvalidRequestId e) {
            return HttpStatusCode.error(HttpErrorType.INVALID_ID);
        } catch (SocketTimeoutException e) {
            log.log(Level.WARNING, e.getMessage(), e.getStackTrace());
            return HttpStatusCode.error(HttpErrorType.DEADLINE_REACHED);
        } catch (IOException e) {
            log.log(Level.WARNING, e.getMessage(), e.getStackTrace());
            return HttpStatusCode.error(HttpErrorType.IO_ERROR);
        }
    }

    private int waitRequestResponseWithTimeout(HttpURLConnection request,
                                               int timeout,
                                               long startTime) throws IOException {
        request.setConnectTimeout(timeout);
        request.connect();

        long timeElapsed = Instant.now().toEpochMilli() - startTime;
        long remainingTime = timeout - timeElapsed;
        if (remainingTime > 0) {
            request.setReadTimeout((int) remainingTime);
        } else {
            throw new SocketTimeoutException("Timeout while connecting");
        }
        return request.getResponseCode();
    }

    private HttpURLConnection getExistingRequest(int id) throws InvalidRequestId {
        return Optional.ofNullable(requests.get(id)).orElseThrow(InvalidRequestId::new);
    }
}
