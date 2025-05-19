package com.limechain.sync.state;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import com.limechain.config.HostConfig;
import com.limechain.rpc.client.SyncStateRpcClient;
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

/**
 * SyncStateRequesterRpc is a service responsible for retrieving the full blockchain state (key-value pairs)
 * from a node via RPC WebSocket API.
 * <p>
 * It ensures complete retrieval by:
 * - Paged requests to fetch all keys.
 * - Individual value requests for each key.
 * - Retrying multiple times in case of temporary failures.
 * - Aborting the program if critical failures happen.
 * <p>
 * This approach guarantees correctness compared to the protocol-based warp sync, which failed due to missing or malformed key/value data.
 * <p>
 * <p>
 * ------------------
 * High-Level Workflow:
 * ------------------
 * <p>
 * 1. Initialize a pool of WebSocket clients.
 * 2. Fetch all state keys using `state_getKeysPaged` in batches.
 * 3. For each key, retrieve its corresponding value using `state_getStorage`.
 * 4. Retry failed operations a limited number of times.
 * 5. If critical errors occur (e.g., persistent missing keys/values), abort the program.
 * 6. Finally, return the full validated key-value map.
 * <p>
 * Threads:
 * - One thread fetches key batches sequentially.
 * - A virtual thread pool fetches values for keys in parallel for efficiency.
 */
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
    private static final int SLEEP_BETWEEN_RETRIES = 5000;
    private static final int SLEEP_AFTER_EMPTY_KEYS = 1000;
    private static final long LOG_INTERVAL = 20000;
    private long lastLogTime = System.currentTimeMillis();

    private static final ObjectMapper mapper = new ObjectMapper();

    private final BlockingQueue<SyncStateRpcClient> clientPool = new LinkedBlockingQueue<>();
    private final Map<WebSocketClient, CompletableFuture<String>> responseMap = new ConcurrentHashMap<>();
    private String wsUrl;

    @Autowired
    public void setHostConfig(HostConfig hostConfig) {
        this.wsUrl = hostConfig.getNodeSynckPath();
    }

    /**
     * Initializes connections and starts the retrieval process
     *
     * @param blockHash is the last finalized block hash for which we want the full state
     */
    public Map<ByteString, ByteString> requestState(String blockHash) {
        try {
            initializeWebSocketPool();
            return startStateRetrieval(blockHash);
        } catch (Exception e) {
            log.severe(String.format("requestState: Error in requestState: %s", e.getMessage()));
            return Collections.emptyMap();
        } finally {
            closeAllClients();
        }
    }

    /**
     * Main retrieval loop.
     * <p>
     * Repeatedly:
     * - Fetches the next batch of keys.
     * - Fetches values for these keys in parallel.
     * - If we get less than BATCH_SIZE keys, we assume we are done.
     * <p>
     * Retry behavior:
     * - If a batch fetch fails, we retry up to MAX_KEY_RETRIES times before aborting.
     */
    public Map<ByteString, ByteString> startStateRetrieval(String blockHash) {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
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
                        Thread.sleep(SLEEP_AFTER_EMPTY_KEYS);
                        continue;
                    }

                    // Process the keys in parallel (each fetches its own value)
                    processKeysBatch(keys, executor, stateData, blockHash);
                    long now = System.currentTimeMillis();

                    if (now - lastLogTime >= LOG_INTERVAL) {
                        log.info(String.format("Progress: %d keys processed with values.",
                                stateData.size()));
                        lastLogTime = now;
                    }

                    lastKey = keys.getLast(); // Use last key from previous batch as start for next
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
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                        log.severe("startStateRetrieval: Interrupted during fetching batch, aborting...");
                        System.exit(1);
                    }

                    if (retryCount >= MAX_KEY_RETRIES) {
                        //Todo: In future we may think of requesting state for different block instead of aborting.
                        log.severe(String.format("startStateRetrieval: Fatal - could not retrieve keys after %d attempts",
                                MAX_VALUE_RETRIES));

                        System.exit(1);
                    }
                    Thread.sleep(SLEEP_BETWEEN_RETRIES);
                }
            }

            log.info(String.format("startStateRetrieval: Completed - %d total keys and their valid values.",
                    stateData.size()));

            // Final return: Hex-encoded keys and values wrapped in ByteString
            return stateData.entrySet().stream()
                    .filter(entry -> entry.getValue() != null)
                    .collect(Collectors.toMap(
                            entry -> ByteString.fromHex(StringUtils.remove0xPrefix(entry.getKey())),
                            entry -> ByteString.fromHex(StringUtils.remove0xPrefix(entry.getValue()))
                    ));
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                log.severe("startStateRetrieval: Interrupted during fetching batch, aborting...");
                System.exit(1);
            }

            log.severe(String.format("startStateRetrieval: Error in startStateRetrieval: %s", e.getMessage()));
            return Collections.emptyMap();
        }
    }

    /**
     * Takes a batch of keys and retrieves their corresponding values in parallel.
     */
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

    /**
     * Attempts to fetch the value for a given key.
     * <p>
     * Retries if null response received or any error occurs.
     * Aborts program if retrieval fails after MAX_VALUE_RETRIES.
     */
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
                Thread.sleep(SLEEP_BETWEEN_RETRIES);
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

    /**
     * Initializes multiple WebSocket clients and adds them to the client pool for reuse.
     */
    private void initializeWebSocketPool() throws Exception {
        for (int i = 0; i < CONNECTION_POOL_SIZE; i++) {
            SyncStateRpcClient client = new SyncStateRpcClient(new URI(wsUrl), responseMap);
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
            } catch (Exception e) {
                log.warning("closeAllClients: Failed to close WebSocketClient: " + e.getMessage());
            }
        }
    }

    /**
     * RPC call to retrieve a batch of keys.
     */
    private List<String> getKeysPaged(String startKey, String blockHash) throws Exception {
        String response = sendRequestWithPooledClient(
                GET_KEYS_PAGED_METHOD_NAME,
                new String[]{PREFIX, String.valueOf(BATCH_SIZE), startKey, blockHash}
        );
        JsonNode result = getJsonNode(response);

        return mapper.convertValue(result, List.class);
    }

    /**
     * RPC call to retrieve the value of a single key.
     */
    private String getStorage(String key, String blockHash) throws Exception {
        String response = sendRequestWithPooledClient(
                GET_STORAGE_METHOD_NAME,
                new String[]{key, blockHash}
        );
        JsonNode result = getJsonNode(response);

        return result.isNull() ? null : result.asText();
    }

    /**
     * Validates and extracts the 'result' part of a JSON-RPC response.
     * Throws if missing to avoid silent failures.
     */
    private JsonNode getJsonNode(String response) throws JsonProcessingException {
        JsonNode root = mapper.readTree(response);
        if (root == null || !root.has("result")) {
            throw new IllegalStateException("Invalid JSON response: missing result field");
        }
        return root.get("result");
    }

    /**
     * Takes a WebSocket client from the pool, sends a request, waits for the response.
     */
    private String sendRequestWithPooledClient(String methodName, String[] args) throws Exception {
        SyncStateRpcClient client = clientPool.take();
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
