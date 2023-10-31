package com.limechain.runtime.hostapi;

import com.limechain.config.HostConfig;
import com.limechain.network.Network;
import com.limechain.network.protocol.blockannounce.NodeRole;
import com.limechain.rpc.server.AppBean;
import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scaletypes.Result;
import io.emeraldpay.polkaj.scaletypes.ResultWriter;
import io.libp2p.core.PeerId;
import io.libp2p.core.multiformats.Multiaddr;
import lombok.AllArgsConstructor;
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

/**
 * Implementations of the Offchain and Offchain index HostAPI functions
 * For more info check
 * {<a href="https://spec.polkadot.network/chap-host-api#sect-offchain-api">Offchain API</a>}
 * {<a href="https://spec.polkadot.network/chap-host-api#sect-offchainindex-api">Offchain index API</a>}
 */
@AllArgsConstructor
public class OffchainHostFunctions {
    private final HostApi hostApi;
    private final HostConfig config;

    public OffchainHostFunctions() {
        hostApi = HostApi.getInstance();
        config = AppBean.getBean(HostConfig.class);
    }
    public static List<ImportObject> getFunctions() {
        return new OffchainHostFunctions().buildFunctions();
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
                HostApi.getImportObject("ext_offchain_local_storage_set_version_1", argv -> {

                }, List.of(Type.I32, Type.I64, Type.I64)),
                HostApi.getImportObject("ext_offchain_local_storage_clear_version_1", argv -> {

                }, List.of(Type.I32, Type.I64)),
                HostApi.getImportObject("ext_offchain_local_storage_compare_and_set_version_1", argv -> {
                    return 0;
                }, List.of(Type.I32, Type.I64, Type.I64, Type.I64), Type.I32),
                HostApi.getImportObject("ext_offchain_local_storage_get_version_1", argv -> {
                    return 0;
                }, List.of(Type.I32, Type.I64), Type.I64),
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
                HostApi.getImportObject("ext_offchain_index_set_version_1", argv -> {

                }, List.of(Type.I64, Type.I64)),
                HostApi.getImportObject("ext_offchain_index_clear_version_1", argv -> {

                }, List.of(Type.I64))
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
}
