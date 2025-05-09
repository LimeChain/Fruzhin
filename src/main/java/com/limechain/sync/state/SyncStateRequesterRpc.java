package com.limechain.sync.state;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import com.limechain.config.HostConfig;
import com.limechain.rpc.client.StateSyncRpcClient;
import com.limechain.utils.StringUtils;
import lombok.extern.java.Log;
import org.java_websocket.client.WebSocketClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
@Log
public class SyncStateRequesterRpc {
    private static final String PREFIX = "0x";
    public static final String GET_KEYS_PAGED_METHOD_NAME = "state_getKeysPaged";
    public static final String GET_STORAGE_METHOD_NAME = "state_getStorage";
    private static final int BATCH_SIZE = 1000;
    private static final int MAX_KEY_RETRIES = 100;
    private static final int MAX_VALUE_RETRIES = 100;
    private static final int WS_TIMEOUT_SECONDS = 30;
    private static final int CONNECTION_POOL_SIZE = 100;
    private static final long LOG_INTERVAL = 5000;
    private static final ObjectMapper mapper = new ObjectMapper();

    private final AtomicInteger processedKeys = new AtomicInteger(0);
    private long lastLogTime = System.currentTimeMillis();

    private final BlockingQueue<StateSyncRpcClient> clientPool = new LinkedBlockingQueue<>();
    private final Map<WebSocketClient, CompletableFuture<String>> responseMap = new ConcurrentHashMap<>();
    private String wsUrl;

    @Autowired
    public void setHostConfig(HostConfig hostConfig) {
        this.wsUrl = hostConfig.getNodeSynckPath();
    }

    public Map<ByteString, ByteString> requestState(String blockHash) {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        Map<String, String> stateData = new ConcurrentHashMap<>();
        String lastKey = null;
        int retryCount = 0;
        int totalKeysProcessed = 0;
        final AtomicInteger failedKeys = new AtomicInteger(0);

        try {
            initializeWebSocketPool();

            List<String> keys;
            log.info("Starting state retrieval for block: " + blockHash);
            while (true) {
                try {
                    keys = getKeysPaged(lastKey, blockHash);
                    totalKeysProcessed += keys.size();
                    log.info(String.format("Retrieved %d keys in total", totalKeysProcessed));

                    if (keys.isEmpty()) {
                        retryCount++;
                        log.warning(String.format("Received empty key list (attempt %d of %d)", retryCount, MAX_KEY_RETRIES));
                        if (retryCount >= MAX_KEY_RETRIES) {
                            log.severe("Aborting: Too many empty key list responses");
                            System.exit(1);
                        }
                        Thread.sleep(1000);
                        continue;
                    }

                    List<CompletableFuture<Void>> futures = new ArrayList<>();
                    for (String key : keys) {
                        futures.add(
                                CompletableFuture.runAsync(() -> {
                                    int attempts = 0;
                                    String value = null;
                                    while (attempts < MAX_VALUE_RETRIES) {
                                        try {
                                            value = getStorage(key, blockHash);
                                            if (value != null) break;
                                            log.warning(String.format("Attempt %d: Retrieved null for key %s", attempts + 1, key));
                                        } catch (Exception e) {
                                            log.warning(String.format("Attempt %d: Error retrieving key %s: %s", attempts + 1, key, e.getMessage()));
                                        }
                                        attempts++;
                                        try {
                                            Thread.sleep(500);
                                        } catch (InterruptedException e) {
                                            Thread.currentThread().interrupt();
                                            log.severe("Interrupted during retry sleep");
                                            System.exit(1);
                                        }
                                    }
                                    if (value == null) {
                                        log.severe(String.format("Fatal: Could not retrieve value for key %s after %d attempts", key, MAX_VALUE_RETRIES));
                                        System.exit(1);
                                    }
                                    stateData.put(key, value);
                                }, executor)
                        );
                    }

                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                    processedKeys.addAndGet(keys.size());

                    long now = System.currentTimeMillis();
                    if (now - lastLogTime >= LOG_INTERVAL) {
                        log.info(String.format("Progress: %d keys processed, %d valid values, %d failed.",
                                processedKeys.get(), stateData.size(), failedKeys.get()));
                        lastLogTime = now;
                    }

                    lastKey = keys.getLast();
                    retryCount = 0;

                    if (keys.size() < BATCH_SIZE) {
                        log.info("Finished: Received less than batch size");
                        break;
                    }
                } catch (Exception e) {
                    retryCount++;
                    log.severe(String.format("Error fetching batch (attempt %d): %s", retryCount, e.getMessage()));
                    if (retryCount >= MAX_KEY_RETRIES) {
                        log.severe(String.format("Fatal: Could not retrieve keys after %d attempts", MAX_VALUE_RETRIES));
                        System.exit(1);
                    }
                    Thread.sleep(5000);
                }
            }

            log.info(String.format("Completed: %d total keys, %d valid values, %d failed",
                    totalKeysProcessed, stateData.size(), failedKeys.get()));

            return stateData.entrySet().stream()
                    .filter(entry -> entry.getValue() != null)
                    .collect(Collectors.toMap(
                            entry -> ByteString.fromHex(StringUtils.remove0xPrefix(entry.getKey())),
                            entry -> ByteString.fromHex(StringUtils.remove0xPrefix(entry.getValue()))
                    ));
        } catch (Exception e) {
            log.severe(String.format("Error in collectState: %s", e.getMessage()));
            return Collections.emptyMap();
        } finally {
            for (WebSocketClient client : clientPool) {
                try {
                    client.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void initializeWebSocketPool() throws Exception {
        for (int i = 0; i < CONNECTION_POOL_SIZE; i++) {
            StateSyncRpcClient client = new StateSyncRpcClient(new URI(wsUrl), responseMap);
            client.setConnectionLostTimeout(WS_TIMEOUT_SECONDS);

            if (!client.connectBlocking(WS_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new RuntimeException("Failed to connect WebSocket");
            }

            clientPool.add(client);
        }
    }

    private List<String> getKeysPaged(String startKey, String blockHash) throws Exception {
        String response = sendRequestWithPooledClient(GET_KEYS_PAGED_METHOD_NAME, new String[]{
                        PREFIX,
                        String.valueOf(BATCH_SIZE),
                        startKey,
                        blockHash
                }
        );
        JsonNode responseNode = mapper.readTree(response);
        JsonNode result = responseNode.get("result");

        if (result == null) return Collections.emptyList();

        return mapper.convertValue(result, List.class);
    }

    private String getStorage(String key, String blockHash) throws Exception {
        String response = sendRequestWithPooledClient(GET_STORAGE_METHOD_NAME, new String[]{
                        key,
                        blockHash
                }
        );
        JsonNode responseNode = mapper.readTree(response);
        JsonNode result = responseNode.get("result");

        return result != null && !result.isNull() ? result.asText() : null;
    }

    private String sendRequestWithPooledClient(String methodName, String[] args) throws Exception {
        StateSyncRpcClient client = clientPool.take();
        CompletableFuture<String> future = new CompletableFuture<>();
        responseMap.put(client, future);

        try {
            client.send(methodName, args);
            return future.get(WS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } finally {
            clientPool.put(client);
        }
    }
}
