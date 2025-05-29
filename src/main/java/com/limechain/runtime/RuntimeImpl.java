package com.limechain.runtime;

import com.limechain.chain.lightsyncstate.scale.AuthorityReader;
import com.limechain.consensus.babe.dto.runtime.BabeApiConfiguration;
import com.limechain.consensus.babe.dto.runtime.BlockEquivocationProof;
import com.limechain.consensus.babe.scale.runtime.BabeApiConfigurationReader;
import com.limechain.consensus.babe.scale.runtime.BlockEquivocationProofWriter;
import com.limechain.consensus.beefy.dto.BeefyAuthoritySet;
import com.limechain.consensus.beefy.dto.DoubleVotingProof;
import com.limechain.consensus.beefy.scale.runtime.BeefyAuthoritySetReader;
import com.limechain.consensus.beefy.scale.runtime.BeefyDoubleVotingProofScaleWriter;
import com.limechain.consensus.dto.Authority;
import com.limechain.consensus.dto.runtime.OpaqueKeyOwnershipProof;
import com.limechain.consensus.grandpa.dto.runtime.GrandpaEquivocation;
import com.limechain.consensus.grandpa.scale.runtime.GrandpaEquivocationScaleWriter;
import com.limechain.consensus.scale.runtime.OpaqueKeyOwnershipProofReader;
import com.limechain.exception.global.RuntimeCallException;
import com.limechain.exception.scale.ScaleEncodingException;
import com.limechain.network.protocol.blockannounce.scale.BlockHeaderScaleWriter;
import com.limechain.network.protocol.transaction.scale.TransactionReader;
import com.limechain.network.protocol.warp.dto.Block;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.network.protocol.warp.scale.reader.BlockHeaderReader;
import com.limechain.network.protocol.warp.scale.writer.BlockBodyWriter;
import com.limechain.rpc.methods.author.dto.DecodedKey;
import com.limechain.rpc.methods.author.dto.DecodedKeysReader;
import com.limechain.rpc.server.AppBean;
import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import com.limechain.runtime.version.RuntimeVersion;
import com.limechain.runtime.version.scale.RuntimeVersionReader;
import com.limechain.sync.fullsync.inherents.InherentData;
import com.limechain.sync.fullsync.inherents.scale.InherentDataWriter;
import com.limechain.transaction.dto.ApplyExtrinsicResult;
import com.limechain.transaction.dto.Extrinsic;
import com.limechain.transaction.dto.ExtrinsicArray;
import com.limechain.transaction.dto.TransactionValidationRequest;
import com.limechain.transaction.dto.TransactionValidationResponse;
import com.limechain.trie.DiskTrieAccessor;
import com.limechain.trie.TrieAccessor;
import com.limechain.trie.TrieAccessorStorage;
import com.limechain.trie.structure.nibble.Nibbles;
import com.limechain.utils.ByteArrayUtils;
import com.limechain.utils.LittleEndianUtils;
import com.limechain.utils.scale.ScaleUtils;
import com.limechain.utils.scale.readers.ApplyExtrinsicResultReader;
import com.limechain.utils.scale.readers.TransactionValidationReader;
import com.limechain.utils.scale.writers.BlockInherentsWriter;
import com.limechain.utils.scale.writers.TransactionValidationWriter;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.reader.ListReader;
import io.emeraldpay.polkaj.scale.writer.UInt64Writer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wasmer.Instance;
import org.wasmer.Module;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

@Log
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class RuntimeImpl implements Runtime {

    Module module;
    Context context;
    Instance instance;

    private final TrieAccessorStorage accessorStorage = AppBean.getBean(TrieAccessorStorage.class);

    @Override
    public synchronized BabeApiConfiguration getBabeApiConfiguration(BlockHeader header) {
        return ScaleUtils.Decode.decode(call(header, RuntimeEndpoint.BABE_API_CONFIGURATION), BabeApiConfigurationReader.getInstance());
    }

    @Override
    public synchronized Optional<OpaqueKeyOwnershipProof> generateBabeKeyOwnershipProof(BlockHeader header, BigInteger slotNumber,
                                                                                        byte[] authorityPublicKey) {
        byte[] encodedProof = ArrayUtils.addAll(ScaleUtils.Encode.encode(
                new UInt64Writer(), slotNumber), authorityPublicKey);
        byte[] encodedResponse = call(header, RuntimeEndpoint.BABE_API_GENERATE_KEY_OWNERSHIP_PROOF, encodedProof);
        return new ScaleCodecReader(encodedResponse).readOptional(OpaqueKeyOwnershipProofReader.getInstance());
    }

    @Override
    public synchronized void submitReportBabeEquivocationUnsignedExtrinsic(BlockHeader header, BlockEquivocationProof blockEquivocationProof,
                                                                           byte[] keyOwnershipProof) {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream();
             ScaleCodecWriter scaleCodecWriter = new ScaleCodecWriter(buffer)) {
            BlockEquivocationProofWriter.getInstance().write(scaleCodecWriter, blockEquivocationProof);
            scaleCodecWriter.writeAsList(keyOwnershipProof);
            call(header, RuntimeEndpoint.BABE_API_SUBMIT_REPORT_EQUIVOCATION_UNSIGNED_EXTRINSIC, buffer.toByteArray());
        } catch (IOException e) {
            throw new ScaleEncodingException("Unexpected exception while encoding.");
        }
    }

    @Override
    public synchronized List<Authority> getGrandpaApiAuthorities(BlockHeader header) {
        return ScaleUtils.Decode.decode(
                call(header, RuntimeEndpoint.GRANDPA_API_GRANDPA_AUTHORITIES), new ListReader<>(AuthorityReader.getInstance())
        );
    }

    @Override
    public synchronized Optional<OpaqueKeyOwnershipProof> generateGrandpaKeyOwnershipProof(BlockHeader header, BigInteger authoritySetId, byte[] authorityPublicKey) {
        byte[] encodedProof = ArrayUtils.addAll(ScaleUtils.Encode.encode(
                new UInt64Writer(), authoritySetId), authorityPublicKey);
        byte[] encodedResponse = call(header, RuntimeEndpoint.GRANDPA_API_GENERATE_KEY_OWNERSHIP_PROOF, encodedProof);
        return new ScaleCodecReader(encodedResponse).readOptional(OpaqueKeyOwnershipProofReader.getInstance());
    }

    @Override
    public synchronized void submitReportGrandpaEquivocationUnsignedExtrinsic(BlockHeader header, GrandpaEquivocation grandpaEquivocation, byte[] keyOwnershipProof) {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream();
             ScaleCodecWriter scaleCodecWriter = new ScaleCodecWriter(buffer)) {
            GrandpaEquivocationScaleWriter.getInstance().write(scaleCodecWriter, grandpaEquivocation);
            scaleCodecWriter.writeAsList(keyOwnershipProof);
            call(header, RuntimeEndpoint.GRANDPA_API_SUBMIT_REPORT_EQUIVOCATION_UNSIGNED_EXTRINSIC, buffer.toByteArray());
        } catch (IOException e) {
            throw new ScaleEncodingException("Unexpected exception while encoding.");
        }
    }

    @Override
    public synchronized Optional<OpaqueKeyOwnershipProof> generateBeefyKeyOwnershipProof(BlockHeader header, BigInteger authoritySetId, byte[] authorityPublicKey) {
        byte[] encodedProof = ArrayUtils.addAll(ScaleUtils.Encode.encode(
                new UInt64Writer(), authoritySetId), authorityPublicKey);
        byte[] encodedResponse = call(header, RuntimeEndpoint.BEEFY_API_GENERATE_KEY_OWNERSHIP_PROOF, encodedProof);
        return new ScaleCodecReader(encodedResponse).readOptional(OpaqueKeyOwnershipProofReader.getInstance());
    }

    @Override
    public synchronized void submitReportBeefyDoubleVotingUnsignedExtrinsic(BlockHeader header, DoubleVotingProof doubleVotingProof, byte[] keyOwnershipProof) {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream();
             ScaleCodecWriter scaleCodecWriter = new ScaleCodecWriter(buffer)) {
            BeefyDoubleVotingProofScaleWriter.getInstance().write(scaleCodecWriter, doubleVotingProof);
            scaleCodecWriter.writeAsList(keyOwnershipProof);
            call(header, RuntimeEndpoint.BEEFY_API_SUBMIT_REPORT_DOUBLE_VOTING_UNSIGNED_EXTRINSIC, buffer.toByteArray());
        } catch (IOException e) {
            throw new ScaleEncodingException("Unexpected exception while encoding.");
        }
    }

    @Override
    public synchronized Optional<BeefyAuthoritySet> getBeefyValidatorSet(BlockHeader header) {
        byte[] encodedResponse = call(header, RuntimeEndpoint.BEEFY_API_VALIDATOR_SET);
        return encodedResponse == null
                ? Optional.empty()
                : new ScaleCodecReader(encodedResponse).readOptional(BeefyAuthoritySetReader.getInstance());
    }

    @Override
    public synchronized Optional<BigInteger> getBeefyGenesis(BlockHeader header) {
        byte[] encodedResponse = call(header, RuntimeEndpoint.BEEFY_API_BEEFY_GENESIS);
        return encodedResponse == null
                ? Optional.empty()
                : new ScaleCodecReader(encodedResponse).readOptional(ScaleCodecReader.UINT32).map(BigInteger::valueOf);
    }

    @Override
    public synchronized List<DecodedKey> decodeSessionKeys(BlockHeader header, String sessionKeys) {
        byte[] encodedRequest = ScaleUtils.Encode.encode(
                ScaleCodecWriter::writeByteArray, com.limechain.utils.StringUtils.hexToBytes(sessionKeys));
        byte[] encodedResponse = call(header, RuntimeEndpoint.SESSION_KEYS_DECODE_SESSION_KEYS, encodedRequest);
        return ScaleUtils.Decode.decode(encodedResponse, DecodedKeysReader.getInstance());
    }

    @Override
    public synchronized RuntimeVersion getCachedVersion() {
        return context.getRuntimeVersion();
    }

    @Override
    public synchronized RuntimeVersion getVersion(BlockHeader header) {
        return ScaleUtils.Decode.decode(call(header, RuntimeEndpoint.CORE_VERSION), RuntimeVersionReader.getInstance());
    }

    @Override
    public synchronized TransactionValidationResponse validateTransaction(BlockHeader header, TransactionValidationRequest request) {
        byte[] encodedRequest = ScaleUtils.Encode.encode(TransactionValidationWriter.getInstance(), request);
        byte[] encodedResponse = callAndBackup(header, RuntimeEndpoint.TRANSACTION_QUEUE_VALIDATE_TRANSACTION, encodedRequest);

        return ScaleUtils.Decode.decode(encodedResponse, TransactionValidationReader.getInstance());
    }

    @Override
    public synchronized BlockHeader finalizeBlock(BlockHeader header) {
        byte[] encodedResponse = call(header, RuntimeEndpoint.BLOCKBUILDER_FINALIZE_BLOCK);
        return ScaleUtils.Decode.decode(encodedResponse, BlockHeaderReader.getInstance());
    }

    @Override
    public synchronized byte[] checkInherents(BlockHeader header, Block block, InherentData inherentData) {
        byte[] encodedRequest = serializeCheckInherentsParameter(block, inherentData);
        return call(header, RuntimeEndpoint.BLOCKBUILDER_CHECK_INHERENTS, encodedRequest);
    }

    @Override
    public synchronized ApplyExtrinsicResult applyExtrinsic(BlockHeader header, Extrinsic extrinsic) {
        byte[] encodedRequest = ScaleUtils.Encode.encodeAsListOfBytes(ByteArrayUtils.toIterable(extrinsic.getData()));
        byte[] encodedResponse = call(header, RuntimeEndpoint.BLOCKBUILDER_APPLY_EXTRINISIC, encodedRequest);

        return ScaleUtils.Decode.decode(encodedResponse, ApplyExtrinsicResultReader.getInstance());
    }

    @Override
    public synchronized ExtrinsicArray inherentExtrinsics(BlockHeader header, com.limechain.consensus.babe.dto.InherentData inherentData) {
        byte[] encodedRequest = ScaleUtils.Encode.encode(BlockInherentsWriter.getInstance(), inherentData);
        byte[] encodedResponse = call(header, RuntimeEndpoint.BLOCKBUILDER_INHERENT_EXTRINISICS, encodedRequest);

        return ScaleUtils.Decode.decode(encodedResponse, TransactionReader.getInstance());
    }

    @Override
    public synchronized byte[] generateSessionKeys(BlockHeader header, byte[] scaleSeed) {
        byte[] encodedRequest = ScaleUtils.Encode.encodeOptional(ScaleCodecWriter::writeByteArray, scaleSeed);
        return call(header, RuntimeEndpoint.SESSION_KEYS_GENERATE_SESSION_KEYS, encodedRequest);
    }

    @Override
    public synchronized byte[] getMetadata(BlockHeader header) {
        return call(header, RuntimeEndpoint.METADATA_METADATA);
    }

    @Override
    public synchronized void executeBlock(BlockHeader header, Block block) {
        byte[] param = serializeExecuteBlockParameter(block);
        call(header, RuntimeEndpoint.CORE_EXECUTE_BLOCK, param);
    }

    @Override
    public synchronized void initializeBlock(BlockHeader header, BlockHeader blockHeader) {
        byte[] encHeader = ScaleUtils.Encode.encode(BlockHeaderScaleWriter.getInstance(), blockHeader);
        call(header, RuntimeEndpoint.CORE_INITIALIZE_BLOCK, encHeader);
    }

    @Override
    public synchronized BigInteger getGenesisSlotNumber(BlockHeader header) {
        var optGenesisSlotBytes = this.findStorageValue(header, RuntimeStorageKey.GENESIS_SLOT.getNibbles());
        return optGenesisSlotBytes.map(LittleEndianUtils::fromLittleEndianByteArray).orElse(null);
    }

    @Override
    public synchronized Optional<byte[]> getRuntimeCode(BlockHeader header) {
        return this.findStorageValue(header, RuntimeStorageKey.CODE.getNibbles());
    }

    @Override
    public synchronized void persistsChanges(BlockHeader header) {
        accessorStorage.get(header.getHash()).persistChanges();
    }

    @Override
    public synchronized void close() {
        module.close();
        instance.close();
    }

    private synchronized byte[] serializeExecuteBlockParameter(Block block) {
        byte[] encodedUnsealedHeader = ScaleUtils.Encode.encode(
                BlockHeaderScaleWriter.getInstance()::writeUnsealed,
                block.getHeader()
        );
        byte[] encodedBody = ScaleUtils.Encode.encode(BlockBodyWriter.getInstance(), block.getBody());

        return ArrayUtils.addAll(encodedUnsealedHeader, encodedBody);
    }

    private synchronized byte[] serializeCheckInherentsParameter(Block block, InherentData inherentData) {
        byte[] executeBlockParameter = serializeExecuteBlockParameter(block);
        byte[] scaleEncodedInherentData = ScaleUtils.Encode.encode(InherentDataWriter.getInstance(), inherentData);
        return ArrayUtils.addAll(executeBlockParameter, scaleEncodedInherentData);
    }

    /**
     * Calls an exported runtime function with no parameters.
     *
     * @param function the name Runtime function to call
     * @return the SCALE encoded response
     */
    @Nullable
    private synchronized byte[] call(@Nullable BlockHeader header, RuntimeEndpoint function) {
        try {
            return call(header, function, new byte[0]);
        } catch (RuntimeCallException e) {
            log.warning(String.format("call: Couldn't execute runtime call %s: %s", function, e.getMessage()));
            return null;
        }
    }

    @Nullable
    private synchronized byte[] call(@Nullable BlockHeader header, RuntimeEndpoint function, byte[] parameter) {
        if (header != null) {
            DiskTrieAccessor parentAccessor = accessorStorage.get(header.getParentHash());
            DiskTrieAccessor newAccessor = accessorStorage.appendStorage(header.getHash(), parentAccessor);
            context.setTrieAccessor(newAccessor);
        }
        try {
            return callInner(function, context.getSharedMemory().writeData(parameter));
        } catch (RuntimeCallException e) {
            log.warning(String.format("call: Couldn't execute runtime call %s: %s", function, e.getMessage()));
            return null;
        }
    }

    @Nullable
    private synchronized byte[] callAndBackup(@Nullable BlockHeader header,
                                              RuntimeEndpoint function,
                                              @NotNull byte[] parameter) {
        try {
            if (header != null) {
                DiskTrieAccessor parentAccessor = accessorStorage.get(header.getParentHash());
                DiskTrieAccessor newAccessor = accessorStorage.appendStorage(header.getHash(), parentAccessor);
                context.setTrieAccessor(newAccessor);
            }
            context.getTrieAccessor().prepareBackup();
            return callInner(function, context.getSharedMemory().writeData(parameter));
        } catch (RuntimeCallException e) {
            log.warning(String.format("callAndBackup: Couldn't execute runtime call %s: %s", function, e.getMessage()));
            return null;
        } finally {
            context.trieAccessor.backup();
        }
    }

    @Nullable
    private synchronized byte[] callInner(RuntimeEndpoint function, RuntimePointerSize parameterPtrSize) {
        String functionName = function.getName();
        log.finest(String.format("Making a runtime call: %s", functionName));
        try {
            Object[] response = instance.exports.getFunction(functionName)
                    .apply(parameterPtrSize.pointer(), parameterPtrSize.size());

            if (response == null) {
                return null;

            }
            RuntimePointerSize responsePtrSize = new RuntimePointerSize((long) response[0]);
            return context.getSharedMemory().readData(responsePtrSize);
        } catch (Exception e) {
            throw new RuntimeCallException(e.getMessage());
        }
    }

    @Override
    public synchronized void setTrieAccessor(TrieAccessor trieAccessor) {
        this.context.setTrieAccessor(trieAccessor);
    }

    private synchronized Optional<byte[]> findStorageValue(@Nullable BlockHeader header, Nibbles key) {
        if (header != null) {
            DiskTrieAccessor parentAccessor = accessorStorage.get(header.getParentHash());
            DiskTrieAccessor newAccessor = accessorStorage.appendStorage(header.getHash(), parentAccessor);
            context.setTrieAccessor(newAccessor);
        }
        return context.getTrieAccessor().findStorageValue(key);
    }
}

