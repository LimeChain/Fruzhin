package com.limechain.runtime.hostapi;

import com.limechain.config.HostConfig;
import com.limechain.exception.ThreadInterruptedException;
import com.limechain.network.Network;
import com.limechain.network.protocol.blockannounce.NodeRole;
import com.limechain.rpc.server.AppBean;
import com.limechain.runtime.hostapi.dto.HttpErrorType;
import com.limechain.runtime.hostapi.dto.HttpStatusCode;
import com.limechain.runtime.hostapi.dto.InvalidArgumentException;
import com.limechain.runtime.hostapi.dto.InvalidRequestId;
import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import com.limechain.runtime.hostapi.exceptions.OffchainResponseWaitException;
import com.limechain.storage.KVRepository;
import com.limechain.storage.offchain.OffchainStore;
import com.limechain.sync.warpsync.SyncedState;
import com.limechain.utils.scale.ScaleUtils;
import com.limechain.utils.scale.exceptions.ScaleEncodingException;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.reader.ListReader;
import io.emeraldpay.polkaj.scale.reader.UInt64Reader;
import io.emeraldpay.polkaj.scale.writer.ListWriter;
import io.emeraldpay.polkaj.scaletypes.Result;
import io.emeraldpay.polkaj.scaletypes.ResultWriter;
import io.libp2p.core.PeerId;
import io.libp2p.core.multiformats.Multiaddr;
import kotlin.Pair;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;
import org.wasmer.ImportObject;
import org.wasmer.Type;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Implementations of the Offchain and Offchain index HostAPI functions
 * For more info check
 * {<a href="https://spec.polkadot.network/chap-host-api#sect-offchain-api">Offchain API</a>}
 * {<a href="https://spec.polkadot.network/chap-host-api#sect-offchainindex-api">Offchain index API</a>}
 */
@Log
@AllArgsConstructor
public class OffchainHostFunctions {
    private final HostApi hostApi;
    private final HostConfig config;
    private final OffchainHttpRequests requests;
    private final OffchainStore persistentStorage;
    private final OffchainStore localStorage;

    private OffchainHostFunctions(final HostApi hostApi) {
        this.hostApi = hostApi;
        this.config = AppBean.getBean(HostConfig.class);
        requests = OffchainHttpRequests.getInstance();
        KVRepository<String, Object> db = SyncedState.getInstance().getRepository();
        persistentStorage = new OffchainStore(db, true);
        localStorage = new OffchainStore(db, false);
    }

    public static List<ImportObject> getFunctions(final HostApi hostApi) {
        return new OffchainHostFunctions(hostApi).buildFunctions();
    }

    public List<ImportObject> buildFunctions() {
        return Arrays.asList(
                HostApi.getImportObject("ext_offchain_is_validator_version_1", argv ->
                                extOffchainIsValidator(),
                        HostApi.EMPTY_LIST_OF_TYPES, Type.I32),
                HostApi.getImportObject("ext_offchain_submit_transaction_version_1", argv ->
                                extOffchainSubmitTransaction(new RuntimePointerSize(argv.get(0))).pointerSize(),
                        List.of(Type.I64), Type.I64),
                HostApi.getImportObject("ext_offchain_network_state_version_1", argv ->
                                extOffchainNetworkState().pointerSize()
                        , HostApi.EMPTY_LIST_OF_TYPES, Type.I64),
                HostApi.getImportObject("ext_offchain_timestamp_version_1", argv ->
                                extOffchainTimestamp(),
                        HostApi.EMPTY_LIST_OF_TYPES, Type.I64),
                HostApi.getImportObject("ext_offchain_sleep_until_version_1", argv ->
                                extOffchainSleepUntil(argv.get(0).longValue()),
                        List.of(Type.I64)),
                HostApi.getImportObject("ext_offchain_random_seed_version_1", argv ->
                                extOffchainRandomSeed(),
                        HostApi.EMPTY_LIST_OF_TYPES, Type.I32),
                HostApi.getImportObject("ext_offchain_local_storage_set_version_1", argv ->
                        extOffchainLocalStorageSet(
                                argv.get(0).intValue(),
                                new RuntimePointerSize(argv.get(1)),
                                new RuntimePointerSize(argv.get(2))
                        ), List.of(Type.I32, Type.I64, Type.I64)),
                HostApi.getImportObject("ext_offchain_local_storage_clear_version_1", argv ->
                        extOffchainLocalStorageClear(argv.get(0).intValue(), new RuntimePointerSize(argv.get(1))),
                        List.of(Type.I32, Type.I64)),
                HostApi.getImportObject("ext_offchain_local_storage_compare_and_set_version_1", argv ->
                        extOffchainLocalStorageCompareAndSet(
                                argv.get(0).intValue(),
                                new RuntimePointerSize(argv.get(1)),
                                new RuntimePointerSize(argv.get(2)),
                                new RuntimePointerSize(argv.get(3))
                        ), List.of(Type.I32, Type.I64, Type.I64, Type.I64), Type.I32),
                HostApi.getImportObject("ext_offchain_local_storage_get_version_1", argv ->
                        extOffchainLocalStorageGet(argv.get(0).intValue(), new RuntimePointerSize(argv.get(1)))
                                .pointerSize(),
                        List.of(Type.I32, Type.I64), Type.I64),
                HostApi.getImportObject("ext_offchain_http_request_start_version_1", argv ->
                                extOffchainHttpRequestStart(
                                        new RuntimePointerSize(argv.get(0)),
                                        new RuntimePointerSize(argv.get(1)),
                                        new byte[0]
                                ).pointerSize()
                        , List.of(Type.I64, Type.I64, Type.I64), Type.I64),
                HostApi.getImportObject("ext_offchain_http_request_add_header_version_1", argv ->
                                extOffchainHttpRequestAddHeader(
                                        argv.get(0).intValue(),
                                        new RuntimePointerSize(argv.get(1)),
                                        new RuntimePointerSize(argv.get(2))
                                ).pointerSize()
                        , List.of(Type.I32, Type.I64, Type.I64), Type.I64),
                HostApi.getImportObject("ext_offchain_http_request_write_body_version_1", argv ->
                                extOffchainHttpRequestWriteBody(
                                        argv.get(0).intValue(),
                                        new RuntimePointerSize(argv.get(1)),
                                        new RuntimePointerSize(argv.get(2))
                                ).pointerSize()
                        , List.of(Type.I32, Type.I64, Type.I64), Type.I64),
                HostApi.getImportObject("ext_offchain_http_response_wait_version_1", argv ->
                                extOffchainHttpResponseWaitVersion1(
                                        new RuntimePointerSize(argv.get(1)),
                                        new RuntimePointerSize(argv.get(2))
                                ).pointerSize()
                        , List.of(Type.I64, Type.I64), Type.I64),
                HostApi.getImportObject("ext_offchain_http_response_headers_version_1", argv ->
                                extOffchainHttpResponseHeadersVersion1(
                                        argv.get(0).intValue()
                                ).pointerSize()
                        , List.of(Type.I32), Type.I64),
                HostApi.getImportObject("ext_offchain_http_response_read_body_version_1", argv ->
                                extOffchainHttpResponseReadBodyVersion1(
                                        argv.get(0).intValue(),
                                        new RuntimePointerSize(argv.get(1)),
                                        new RuntimePointerSize(argv.get(2))
                                ).pointerSize()
                        , List.of(Type.I32, Type.I64, Type.I64), Type.I64),
                HostApi.getImportObject("ext_offchain_index_set_version_1", argv ->
                        offchainIndexSet(new RuntimePointerSize(argv.get(0)), new RuntimePointerSize(argv.get(1))),
                        List.of(Type.I64, Type.I64)),
                HostApi.getImportObject("ext_offchain_index_clear_version_1", argv ->
                        offchainIndexClear(new RuntimePointerSize(argv.get(0))),
                        List.of(Type.I64))
        );
    }

    /**
     * Check whether the local node is a potential validator. Even if this function returns 1,
     * it does not mean that any keys are configured or that the validator is registered in the chain.
     *
     * @return an integer which is equal to 1 if the local node is a potential validator or an integer equal to 0 if
     * it is not.
     */
    public int extOffchainIsValidator() {
        return config.getNodeRole() == NodeRole.AUTHORING ? 1 : 0;
    }

    /**
     * Given a SCALE encoded extrinsic, this function submits the extrinsic to the Host’s transaction pool,
     * ready to be propagated to remote peers.
     *
     * @param extrinsicPointer a pointer-size to the byte array storing the encoded extrinsic.
     * @return a  pointer-size to the SCALE encoded Result value. Neither on success nor failure is there any
     * additional data provided. The cause of a failure is implementation specific.
     */
    public RuntimePointerSize extOffchainSubmitTransaction(RuntimePointerSize extrinsicPointer) {
        byte[] extrinsic = hostApi.getDataFromMemory(extrinsicPointer);
        // TODO: add to transaction pool,  when implemented, and set success to the result of that operation
        boolean success = true;

        return hostApi.writeDataToMemory(scaleEncodedEmptyResult(success));
    }

    /**
     * Returns the SCALE encoded, opaque information about the local node’s network state.
     *
     * @return a pointer-size to the SCALE encoded Result value.
     * On success - it contains the Opaque network state structure .
     * On failure, an empty value is yielded where its cause is implementation specific.
     * @see <a href=https://spec.polkadot.network/chap-host-api#defn-opaque-network-state>Opaque Network State</a>
     */
    public RuntimePointerSize extOffchainNetworkState() {
        Network network = Network.getNetwork();
        PeerId peerId = network.getHost().getPeerId();
        List<Multiaddr> multiAddresses = network.getHost().listenAddresses();

        return hostApi.writeDataToMemory(scaleEncodedOpaqueNetwork(peerId, multiAddresses));
    }

    private byte[] scaleEncodedOpaqueNetwork(PeerId peerId, List<Multiaddr> multiAddresses) {
        try (ByteArrayOutputStream buf = new ByteArrayOutputStream();
             ScaleCodecWriter writer = new ScaleCodecWriter(buf)) {
            ByteBuffer data = ByteBuffer.wrap(peerId.getBytes());
            multiAddresses.stream().map(Multiaddr::serialize).forEach(data::put);

            Result<byte[], Exception> result = new Result<>(Result.ResultMode.OK, data.array(), null);

            new ResultWriter<byte[], Exception>()
                    .writeResult(writer, ScaleCodecWriter::writeByteArray, null, result);
            return buf.toByteArray();
        } catch (IOException e) {
            log.log(Level.WARNING, "Could not encode network state.");
            log.log(Level.WARNING, e.getMessage(), e.getStackTrace());
            return scaleEncodedEmptyResult(false);
        }
    }

    /**
     * Returns the current timestamp.
     *
     * @return the current UNIX timestamp (in milliseconds).
     */
    public long extOffchainTimestamp() {
        return Instant.now().toEpochMilli();
    }

    /**
     * Pause the execution until the deadline is reached.
     *
     * @param deadline the UNIX timestamp in milliseconds
     */
    public void extOffchainSleepUntil(long deadline) {
        long timeToSleep = extOffchainTimestamp() - deadline;
        try {
            if (timeToSleep > 0) {
                Thread.sleep(timeToSleep);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ThreadInterruptedException(e);
        }
    }

    /**
     * Generates a random seed. This is a truly random non-deterministic seed generated by the host environment.
     *
     * @return a pointer to the buffer containing the 256-bit seed.
     */
    public int extOffchainRandomSeed() {
        byte[] seed;
        try {
            seed = SecureRandom.getInstanceStrong().generateSeed(32);
        } catch (NoSuchAlgorithmException e) {
            seed = SecureRandom.getSeed(32);
        }
        return hostApi.writeDataToMemory(seed).pointer();
    }

    /**
     * Initiates a HTTP request given by the HTTP method and the URL. Returns the Id of a newly started request.
     *
     * @param methodPointer a pointer-size to the HTTP method. Possible values are “GET” and “POST”.
     * @param uriPointer    a pointer-size to the URI.
     * @param meta          a future-reserved field containing additional, SCALE encoded parameters.
     *                      Currently, an empty array should be passed.
     * @return a pointer-size to the SCALE encoded Result value containing the i16 ID of the newly started request.
     * On failure no additionally data is provided. The cause of failure is implementation specific.
     */
    public RuntimePointerSize extOffchainHttpRequestStart(RuntimePointerSize methodPointer,
                                                          RuntimePointerSize uriPointer,
                                                          byte[] meta) {
        String method = new ScaleCodecReader(hostApi.getDataFromMemory(methodPointer)).readString();
        String uri = new ScaleCodecReader(hostApi.getDataFromMemory(uriPointer)).readString();

        if (!method.equals("GET") && !method.equals("POST")) {
            log.log(Level.WARNING, "Method not allowed: " + method);
            return hostApi.writeDataToMemory(scaleEncodedEmptyResult(false));
        }

        try (ByteArrayOutputStream buf = new ByteArrayOutputStream();
             ScaleCodecWriter writer = new ScaleCodecWriter(buf)) {
            int createdId = requests.addRequest(method, uri);

            Result<Integer, Exception> result = new Result<>(Result.ResultMode.OK, createdId, null);
            new ResultWriter<Integer, Exception>()
                    .writeResult(writer, ScaleCodecWriter::writeUint16, null, result);

            return hostApi.writeDataToMemory(buf.toByteArray());
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage(), e.getStackTrace());
            return hostApi.writeDataToMemory(scaleEncodedEmptyResult(false));
        }
    }

    /**
     * Append header to the request. Returns an error if the request identifier is invalid, http_response_wait has
     * already been called on the specified request identifier, the deadline is reached or an I/O error has happened
     * (e.g. the remote has closed the connection).
     *
     * @param requestId    an i32 integer indicating the ID of the started request.
     * @param namePointer  a pointer-size to the HTTP header name.
     * @param valuePointer a pointer-size to the HTTP header value.
     * @return a pointer-size to the SCALE encoded Result value. Neither on success or failure is there any additional
     * data provided. The cause of failure is implementation specific.
     */
    public RuntimePointerSize extOffchainHttpRequestAddHeader(int requestId,
                                                              RuntimePointerSize namePointer,
                                                              RuntimePointerSize valuePointer) {
        String name = new String(hostApi.getDataFromMemory(namePointer));
        String value = new String(hostApi.getDataFromMemory(valuePointer));
        try {
            requests.addHeader(requestId, name, value);
            return hostApi.writeDataToMemory(scaleEncodedEmptyResult(true));
        } catch (InvalidRequestId e) {
            log.log(Level.WARNING, "Invalid request id: " + requestId);
            return hostApi.writeDataToMemory(scaleEncodedEmptyResult(false));
        }
    }

    /**
     * Writes a chunk of the request body. Returns a non-zero value in case the deadline is reached
     * or the chunk could not be written.
     *
     * @param requestId       the ID of the started request.
     * @param chunksPointer   a pointer-size to the chunk of bytes. Writing an empty chunk finalizes the request.
     * @param deadlinePointer a pointer-size to the SCALE encoded Option value containing the UNIX timestamp.
     *                        Passing None blocks indefinitely.
     * @return a pointer-size to the SCALE encoded Result value. On success, no additional data is provided.
     * On error, it contains the HTTP error type.
     */
    public RuntimePointerSize extOffchainHttpRequestWriteBody(int requestId,
                                                              RuntimePointerSize chunksPointer,
                                                              RuntimePointerSize deadlinePointer) {
        byte[] chunks = hostApi.getDataFromMemory(chunksPointer);
        try {
            int timeout = timeoutFromDeadline(deadlinePointer);
            requests.addRequestBodyChunk(requestId, chunks, timeout);
            return hostApi.writeDataToMemory(scaleEncodedEmptyResult(true));
        } catch (InvalidRequestId e) {
            return hostApi.writeDataToMemory(HttpErrorType.INVALID_ID.scaleEncodedResult());
        } catch (SocketTimeoutException e) {
            log.log(Level.WARNING, e.getMessage(), e.getStackTrace());
            return hostApi.writeDataToMemory(HttpErrorType.DEADLINE_REACHED.scaleEncodedResult());
        } catch (IOException e) {
            log.log(Level.WARNING, e.getMessage(), e.getStackTrace());
            return hostApi.writeDataToMemory(HttpErrorType.IO_ERROR.scaleEncodedResult());
        }
    }

    /**
     * Returns an array of request statuses (the length is the same as IDs). Note that if deadline is not provided
     * the method will block indefinitely, otherwise unready responses will produce DeadlineReached status.
     *
     * @param idsPointer      a pointer-size to the SCALE encoded array of started request IDs
     * @param deadlinePointer a pointer-size to the SCALE encoded Option value containing
     *                        the UNIX timestamp. Passing None blocks indefinitely.
     * @return a pointer-size to the SCALE encoded array of request statuses.
     */
    public RuntimePointerSize extOffchainHttpResponseWaitVersion1(RuntimePointerSize idsPointer,
                                                                  RuntimePointerSize deadlinePointer) {
        int timeout = timeoutFromDeadline(deadlinePointer);
        byte[] encodedIds = hostApi.getDataFromMemory(idsPointer);
        int[] requestIds = decodeRequestIdArray(encodedIds);

        try {
            HttpStatusCode[] requestStatuses = requests.getRequestsResponses(requestIds, timeout);
            return hostApi.writeDataToMemory(scaleEncodeArrayOfRequestStatuses(requestStatuses));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ThreadInterruptedException(e);
        }
    }

    /**
     * Read all HTTP response headers. Returns an array of key/value pairs.
     * Response headers must be read before the response body.
     *
     * @param id the ID of the started request.
     * @return a pointer-size to the SCALE encoded Result value.
     */
    public RuntimePointerSize extOffchainHttpResponseHeadersVersion1(int id) {
        try {
            Map<String, List<String>> headers = requests.getResponseHeaders(id);
            return hostApi.writeDataToMemory(scaleEncodeHeaders(headers));
        } catch (IOException e) {
            throw new OffchainResponseWaitException(e);
        }
    }

    private byte[] scaleEncodeHeaders(Map<String, List<String>> headers) throws IOException {
        List<Pair<String, String>> pairs = new ArrayList<>(headers.size());
        headers.forEach((key, values) ->
                values.forEach(value ->
                        pairs.add(new Pair<>(key, value))
                ));
        return ScaleUtils.Encode.encode(pairs, String::getBytes, String::getBytes);
    }

    /**
     * Reads a chunk of body response to the given buffer. Returns the number of bytes written or an error
     * in case a deadline is reached or the server closed the connection. If 0 is returned it means that
     * the response has been fully consumed and the request_id is now invalid.
     *
     * @param requestId       the ID of the started request
     * @param bufferPointer   pointer-size to the buffer where the body gets written to.
     * @param deadlinePointer a pointer-size to the SCALE encoded Option valud containing the UNIX timestamp.
     * @return a pointer-size to the SCALE encoded Result value
     */
    public RuntimePointerSize extOffchainHttpResponseReadBodyVersion1(int requestId,
                                                                      RuntimePointerSize bufferPointer,
                                                                      RuntimePointerSize deadlinePointer) {
        int timeout = timeoutFromDeadline(deadlinePointer);
        try {
            long startTime = Instant.now().toEpochMilli();
            HttpStatusCode responseCode = requests.executeRequest(requestId, timeout, startTime);
            if (responseCode.hasError()) {
                return hostApi.writeDataToMemory(responseCode.getErrorType().scaleEncodedResult());
            } else {
                return writeResponseToMemory(requestId, bufferPointer);
            }
        } catch (InvalidRequestId e) {
            return hostApi.writeDataToMemory(HttpErrorType.INVALID_ID.scaleEncodedResult());
        } catch (SocketTimeoutException e) {
            log.log(Level.WARNING, e.getMessage(), e.getStackTrace());
            return hostApi.writeDataToMemory(HttpErrorType.DEADLINE_REACHED.scaleEncodedResult());
        } catch (IOException e) {
            log.log(Level.WARNING, e.getMessage(), e.getStackTrace());
            return hostApi.writeDataToMemory(HttpErrorType.IO_ERROR.scaleEncodedResult());
        }
    }

    private RuntimePointerSize writeResponseToMemory(int requestId, RuntimePointerSize bufferPointer) throws IOException {
        byte[] data = requests.readResponseBody(requestId, bufferPointer.size());
        hostApi.writeDataToMemory(data, bufferPointer);

        byte[] result = scaleEncodeIntResult(data.length);
        return hostApi.writeDataToMemory(result);
    }

    private int timeoutFromDeadline(RuntimePointerSize deadlinePointer) {
        byte[] deadlineBytes = hostApi.getDataFromMemory(deadlinePointer);

        long currentTimestamp = Instant.now().toEpochMilli();
        return new ScaleCodecReader(deadlineBytes)
                .readOptional(new UInt64Reader())
                .map(deadline -> deadline.longValue() - currentTimestamp)
                .map(Long::intValue)
                .orElse(0);
    }

    private int[] decodeRequestIdArray(byte[] encodedRequestIds) {
        ScaleCodecReader reader = new ScaleCodecReader(encodedRequestIds);
        ListReader<Long> listReader = new ListReader<>(ScaleCodecReader::readUint32);
        return reader.read(listReader).stream()
                .mapToInt(Long::intValue)
                .toArray();
    }

    byte[] scaleEncodeIntResult(int value) {
        try (ByteArrayOutputStream buf = new ByteArrayOutputStream();
             ScaleCodecWriter writer = new ScaleCodecWriter(buf)) {

            Result<Integer, Exception> result = new Result<>(Result.ResultMode.OK, value, null);
            new ResultWriter<Integer, Exception>()
                    .writeResult(writer, ScaleCodecWriter::writeUint16, null, result);

            return buf.toByteArray();
        } catch (IOException e) {
            throw new ScaleEncodingException(e);
        }
    }

    byte[] scaleEncodeArrayOfRequestStatuses(HttpStatusCode[] requestStatuses) {
        try (ByteArrayOutputStream buf = new ByteArrayOutputStream();
             ScaleCodecWriter writer = new ScaleCodecWriter(buf)) {
            List<byte[]> scaleEncodedStatuses = Arrays.stream(requestStatuses)
                    .map(HttpStatusCode::scaleEncoded)
                    .toList();
            new ListWriter<>(ScaleCodecWriter::writeByteArray).write(writer, scaleEncodedStatuses);

            return buf.toByteArray();
        } catch (IOException e) {
            throw new ScaleEncodingException(e);
        }
    }

    private byte[] scaleEncodedEmptyResult(boolean success) {
        Result.ResultMode resultMode = success ? Result.ResultMode.OK : Result.ResultMode.ERR;
        return new byte[]{resultMode.getValue()};
    }

    /**
     * Sets a value in the local storage. This storage is not part of the consensus,
     * it’s only accessible by the offchain worker tasks running on the same machine and is persisted between runs.
     *
     * @param kind          an i32 integer indicating the storage kind. A value equal to 1 is used for
     *                      a persistent storage and a value equal to 2 for local storage
     * @param keyPointer    a pointer-size to the key.
     * @param valuePointer  a pointer-size to the value.
     */
    public void extOffchainLocalStorageSet(int kind, RuntimePointerSize keyPointer, RuntimePointerSize valuePointer) {
        OffchainStore store = storageByKind(kind);
        String key = new String(hostApi.getDataFromMemory(keyPointer));
        byte[] value = hostApi.getDataFromMemory(valuePointer);

        store.set(key, value);
    }

    /**
     * Remove a value from the local storage.
     *
     * @param kind          an i32 integer indicating the storage kind. A value equal to 1 is used for
     *                      a persistent storage and a value equal to 2 for local storage
     * @param keyPointer    a pointer-size to the key.
     */
    public void extOffchainLocalStorageClear(int kind, RuntimePointerSize keyPointer) {
        OffchainStore store = storageByKind(kind);
        String key = new String(hostApi.getDataFromMemory(keyPointer));

        store.remove(key);
    }

    /**
     * Sets a new value in the local storage if the condition matches the current value.
     *
     * @param kind              an i32 integer indicating the storage kind. A value equal to 1 is used for
     *                          a persistent storage and a value equal to 2 for local storage
     * @param keyPointer        a pointer-size to the key.
     * @param oldValuePointer   a pointer-size to the SCALE encoded Option value containing the old key.
     * @param newValuePointer   a pointer-size to the new value.
     * @return an i32 integer equal to 1 if the new value has been set or a value equal to 0 if otherwise.
     */
    public int extOffchainLocalStorageCompareAndSet(int kind,
                                                    RuntimePointerSize keyPointer,
                                                    RuntimePointerSize oldValuePointer,
                                                    RuntimePointerSize newValuePointer) {
        OffchainStore store = storageByKind(kind);
        String key = new String(hostApi.getDataFromMemory(keyPointer));
        byte[] oldValue = valueFromOption(hostApi.getDataFromMemory(oldValuePointer));
        byte[] newValue = hostApi.getDataFromMemory(newValuePointer);

        return store.compareAndSet(key, oldValue, newValue) ? 1 : 0;
    }

    private byte[] valueFromOption(byte[] scaleEncodedOption) {
        return new ScaleCodecReader(scaleEncodedOption)
                .readOptional(ScaleCodecReader::readByteArray)
                .orElse(null);
    }

    /**
     * Gets a value from the local storage.
     *
     * @param kind              an i32 integer indicating the storage kind. A value equal to 1 is used for
     *                          a persistent storage and a value equal to 2 for local storage
     * @param keyPointer        a pointer-size to the key.
     * @return a pointer-size to the SCALE encoded Option value containing the value or the corresponding key.
     */
    public RuntimePointerSize extOffchainLocalStorageGet(int kind, RuntimePointerSize keyPointer) {
        OffchainStore store = storageByKind(kind);
        String key = new String(hostApi.getDataFromMemory(keyPointer));

        byte[] value = store.get(key);
        return hostApi.writeDataToMemory(scaleEncodedOption(value));
    }

    private OffchainStore storageByKind(int kind) {
        return switch (kind) {
            case 1 -> persistentStorage;
            case 2 -> localStorage;
            default -> throw new InvalidArgumentException("storage kind", kind);
        };
    }

    private byte[] scaleEncodedOption(byte[] value) {
        try(ByteArrayOutputStream buf = new ByteArrayOutputStream();
            ScaleCodecWriter writer = new ScaleCodecWriter(buf)
        ) {
            writer.writeOptional(ScaleCodecWriter::writeByteArray, value);
            return buf.toByteArray();
        } catch (IOException e) {
            throw new ScaleEncodingException(e);
        }
    }

    /**
     * Write a key-value pair to the Offchain DB in a buffered fashion.
     *
     * @param keyPointer    a pointer-size containing the key.
     * @param valuePointer  a pointer-size containing the value.
     */
    public void offchainIndexSet(RuntimePointerSize keyPointer, RuntimePointerSize valuePointer) {
        byte[] key = hostApi.getDataFromMemory(keyPointer);
        byte[] value = hostApi.getDataFromMemory(valuePointer);

        persistentStorage.set(new String(key), value);
    }

    /**
     * Remove a key and its associated value from the Offchain DB.
     *
     * @param keyPointer a pointer-size containing the key.
     */
    public void offchainIndexClear(RuntimePointerSize keyPointer) {
        byte[] key = hostApi.getDataFromMemory(keyPointer);

        persistentStorage.remove(new String(key));
    }
}
