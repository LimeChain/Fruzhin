package com.limechain.runtime.hostapi;

import com.limechain.config.HostConfig;
import com.limechain.network.Network;
import com.limechain.network.protocol.blockannounce.NodeRole;
import com.limechain.rpc.server.AppBean;
import com.limechain.runtime.hostapi.dto.InvalidArgumentException;
import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import com.limechain.storage.KVRepository;
import com.limechain.storage.offchain.OffchainStore;
import com.limechain.sync.warpsync.SyncedState;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scaletypes.Result;
import io.emeraldpay.polkaj.scaletypes.ResultWriter;
import io.libp2p.core.PeerId;
import io.libp2p.core.multiformats.Multiaddr;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;
import org.wasmer.ImportObject;
import org.wasmer.Type;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
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
    private final OffchainStore persistentStorage;
    private final OffchainStore localStorage;

    private OffchainHostFunctions(final HostApi hostApi) {
        this.hostApi = hostApi;
        this.config = AppBean.getBean(HostConfig.class);
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
                HostApi.getImportObject("ext_offchain_http_request_start_version_1", argv -> {
                    return 0;
                }, List.of(Type.I64, Type.I64, Type.I64), Type.I64),
                HostApi.getImportObject("ext_offchain_http_request_add_header_version_1", argv -> {
                    return 0;
                }, List.of(Type.I32, Type.I64, Type.I64), Type.I64),
                HostApi.getImportObject("ext_offchain_http_request_write_body_version_1", argv -> {
                    return 0;
                }, List.of(Type.I32, Type.I64, Type.I64), Type.I64),
                HostApi.getImportObject("ext_offchain_http_response_wait_version_1", argv -> {
                    return 0;
                }, List.of(Type.I64, Type.I64), Type.I64),
                HostApi.getImportObject("ext_offchain_http_response_headers_version_1", argv -> {
                    return 0;
                }, List.of(Type.I32), Type.I64),
                HostApi.getImportObject("ext_offchain_http_response_read_body_version_1", argv -> {
                    return 0;
                }, List.of(Type.I32, Type.I64, Type.I64), Type.I64),
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

    private byte[] scaleEncodedEmptyResult(boolean success) {
        Result.ResultMode resultMode = success ? Result.ResultMode.OK : Result.ResultMode.ERR;
        return new byte[] { resultMode.getValue() };
    }

    /**
     * Returns the SCALE encoded, opaque information about the local node’s network state.
     *
     * @return a pointer-size to the SCALE encoded Result value.
     * On success - it contains the Opaque network state structure .
     * On failure, an empty value is yielded where its cause is implementation specific.
     *
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
            throw new RuntimeException(e);
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
            throw new RuntimeException(e);
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
