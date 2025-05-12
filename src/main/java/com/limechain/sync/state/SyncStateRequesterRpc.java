package com.limechain.sync.state;

import com.fasterxml.jackson.core.JsonProcessingException;
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
    private static final ObjectMapper mapper = new ObjectMapper();

    private final BlockingQueue<StateSyncRpcClient> clientPool = new LinkedBlockingQueue<>();
    private final Map<WebSocketClient, CompletableFuture<String>> responseMap = new ConcurrentHashMap<>();
    private String wsUrl;

    @Autowired
    public void setHostConfig(HostConfig hostConfig) {
        this.wsUrl = hostConfig.getNodeSynckPath();
    }

    public Map<ByteString, ByteString> requestState(String blockHash) {
        try {
            initializeWebSocketPool();
            log.info("requestState: Starting state retrieval for block: " + blockHash);
            return startStateRetrieval(blockHash);
        } catch (Exception e) {
            log.severe(String.format("requestState: Error in requestState: %s", e.getMessage()));
            return Collections.emptyMap();
        } finally {
            closeAllClients();
        }
    }

    public Map<ByteString, ByteString> startStateRetrieval(String blockHash) {
        Map<String, String> stateData = new ConcurrentHashMap<>();
        String lastKey = null;
        int retryCount = 0;

        try {
            List<String> keys;
            log.info("startStateRetrieval: Starting state retrieval for block: " + blockHash);

            while (true) {
                try {
                    keys = getKeysPaged(lastKey, blockHash);

                    if (keys.isEmpty()) {
                        retryCount++;
                        log.warning(String.format("startStateRetrieval: Received empty key list (attempt %d of %d)",
                                retryCount,
                                MAX_KEY_RETRIES));

                        if (retryCount >= MAX_KEY_RETRIES) {
                            //Todo: In future we may think of requesting state for different block instead of aborting.
                            log.severe("startStateRetrieval: Aborting - too many empty key list responses");
                            System.exit(1);
                        }
                        Thread.sleep(1000);
                        continue;
                    }

                    ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
                    processKeysBatch(keys, executor, stateData, blockHash);

                    lastKey = keys.getLast();
                    retryCount = 0;

                    if (keys.size() < BATCH_SIZE) {
                        log.info("startStateRetrieval: Finished - received less than batch size");
                        break;
                    }
                } catch (Exception e) {
                    retryCount++;
                    log.severe(String.format("startStateRetrieval: Error fetching batch (attempt %d): %s",
                            retryCount,
                            e.getMessage()));

                    if (retryCount >= MAX_KEY_RETRIES) {
                        //Todo: In future we may think of requesting state for different block instead of aborting.
                        log.severe(String.format("startStateRetrieval: Fatal - could not retrieve keys after %d attempts",
                                MAX_VALUE_RETRIES));

                        System.exit(1);
                    }
                    Thread.sleep(5000);
                }
            }

            log.info(String.format("startStateRetrieval: Completed - %d total keys and their valid values.",
                    stateData.size()));

            return stateData.entrySet().stream()
                    .filter(entry -> entry.getValue() != null)
                    .collect(Collectors.toMap(
                            entry -> ByteString.fromHex(StringUtils.remove0xPrefix(entry.getKey())),
                            entry -> ByteString.fromHex(StringUtils.remove0xPrefix(entry.getValue()))
                    ));
        } catch (Exception e) {
            log.severe(String.format("startStateRetrieval: Error in collectState: %s", e.getMessage()));
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

    private void processKeysBatch(List<String> keys,
                                  ExecutorService executor,
                                  Map<String, String> stateData,
                                  String blockHash) {

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (String key : keys) {
            futures.add(
                    CompletableFuture.runAsync(() -> {
                        retrieveAndStoreValue(key, blockHash, stateData);
                    }, executor)
            );
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private void retrieveAndStoreValue(String key, String blockHash, Map<String, String> stateData) {
        int attempts = 0;
        String value = null;

        while (attempts < MAX_VALUE_RETRIES) {
            try {
                value = getStorage(key, blockHash);
                if (value != null) break;

                log.warning(String.format("retrieveAndStoreValue: Attempt %d - retrieved null for key %s",
                        attempts + 1,
                        key));

            } catch (Exception e) {
                log.warning(String.format("retrieveAndStoreValue: Attempt %d - error retrieving key %s: %s",
                        attempts + 1,
                        key, e.getMessage()));
            }
            attempts++;
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.severe("retrieveAndStoreValue: Interrupted during retry sleep");
                System.exit(1);
            }
        }
        if (value == null) {
            //Todo: In future we may think of requesting state for different block instead of aborting.
            log.severe(String.format("retrieveAndStoreValue: Fatal - could not retrieve value for key %s after %d attempts",
                    key,
                    MAX_VALUE_RETRIES)
            );
            System.exit(1);
        }
        stateData.put(key, value);
    }

    private void initializeWebSocketPool() throws Exception {
        for (int i = 0; i < CONNECTION_POOL_SIZE; i++) {
            StateSyncRpcClient client = new StateSyncRpcClient(new URI(wsUrl), responseMap);
            client.setConnectionLostTimeout(WS_TIMEOUT_SECONDS);

            if (!client.connectBlocking(WS_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new RuntimeException("initializeWebSocketPool: Failed to connect WebSocket");
            }

            clientPool.add(client);
        }
    }

    private void closeAllClients() {
        for (WebSocketClient client : clientPool) {
            try {
                client.close();
            } catch (Exception ignored) {
            }
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
        JsonNode result = getJsonNode(response);

        if (result == null) return Collections.emptyList();

        return mapper.convertValue(result, List.class);
    }

    private String getStorage(String key, String blockHash) throws Exception {
        String response = sendRequestWithPooledClient(GET_STORAGE_METHOD_NAME, new String[]{
                        key,
                        blockHash
                }
        );
        JsonNode result = getJsonNode(response);

        return result != null && !result.isNull() ? result.asText() : null;
    }

    private static JsonNode getJsonNode(String response) throws JsonProcessingException {
        JsonNode responseNode = mapper.readTree(response);
        return responseNode.get("result");
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
