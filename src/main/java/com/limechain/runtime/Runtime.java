package com.limechain.runtime;

import com.limechain.consensus.babe.dto.runtime.BabeApiConfiguration;
import com.limechain.consensus.babe.dto.runtime.BlockEquivocationProof;
import com.limechain.consensus.beefy.dto.BeefyAuthoritySet;
import com.limechain.consensus.beefy.dto.DoubleVotingProof;
import com.limechain.consensus.dto.Authority;
import com.limechain.consensus.dto.runtime.OpaqueKeyOwnershipProof;
import com.limechain.consensus.grandpa.dto.runtime.GrandpaEquivocation;
import com.limechain.network.protocol.warp.dto.Block;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.rpc.methods.author.dto.DecodedKey;
import com.limechain.runtime.version.RuntimeVersion;
import com.limechain.sync.fullsync.inherents.InherentData;
import com.limechain.transaction.dto.ApplyExtrinsicResult;
import com.limechain.transaction.dto.Extrinsic;
import com.limechain.transaction.dto.ExtrinsicArray;
import com.limechain.transaction.dto.TransactionValidationRequest;
import com.limechain.transaction.dto.TransactionValidationResponse;
import com.limechain.trie.TrieAccessor;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

public interface Runtime {

    BabeApiConfiguration getBabeApiConfiguration(@Nullable BlockHeader header);

    Optional<OpaqueKeyOwnershipProof> generateBabeKeyOwnershipProof(@Nullable BlockHeader header, BigInteger slotNumber, byte[] authorityPublicKey);

    void submitReportBabeEquivocationUnsignedExtrinsic(@Nullable BlockHeader header, BlockEquivocationProof blockEquivocationProof, byte[] keyOwnershipProof);

    List<Authority> getGrandpaApiAuthorities(@Nullable BlockHeader header);

    Optional<OpaqueKeyOwnershipProof> generateGrandpaKeyOwnershipProof(@Nullable BlockHeader header, BigInteger authoritySetId, byte[] authorityPublicKey);

    void submitReportGrandpaEquivocationUnsignedExtrinsic(@Nullable BlockHeader header, GrandpaEquivocation grandpaEquivocation, byte[] keyOwnershipProof);

    Optional<OpaqueKeyOwnershipProof> generateBeefyKeyOwnershipProof(@Nullable BlockHeader header, BigInteger authoritySetId, byte[] authorityPublicKey);

    void submitReportBeefyDoubleVotingUnsignedExtrinsic(@Nullable BlockHeader header, DoubleVotingProof doubleVotingProof, byte[] keyOwnershipProof);

    Optional<BigInteger> getBeefyGenesis(@Nullable BlockHeader header);

    Optional<BeefyAuthoritySet> getBeefyValidatorSet(@Nullable BlockHeader header);

    List<DecodedKey> decodeSessionKeys(@Nullable BlockHeader header, String sessionKeys);

    RuntimeVersion getCachedVersion();

    RuntimeVersion getVersion(@Nullable BlockHeader header);

    TransactionValidationResponse validateTransaction(@Nullable BlockHeader header, TransactionValidationRequest request);

    BlockHeader finalizeBlock(@Nullable BlockHeader header);

    byte[] checkInherents(@Nullable BlockHeader header, Block block, InherentData inherentData);

    ApplyExtrinsicResult applyExtrinsic(@Nullable BlockHeader header, Extrinsic extrinsic);

    ExtrinsicArray inherentExtrinsics(@Nullable BlockHeader header, com.limechain.consensus.babe.dto.InherentData inherentData);

    byte[] generateSessionKeys(@Nullable BlockHeader header, byte[] scaleSeed);

    byte[] getMetadata(@Nullable BlockHeader header);

    void executeBlock(@Nullable BlockHeader header, Block block);

    void initializeBlock(@Nullable BlockHeader contextHeader, BlockHeader newBlockHeader);

    BigInteger getGenesisSlotNumber(@Nullable BlockHeader header);

    Optional<byte[]> getRuntimeCode(BlockHeader header);

    /**
     * Saves the runtime instance's {@link com.limechain.trie.cache.TrieChanges} to the disk storage.
     */
    void persistsChanges(BlockHeader header);

    void close();

    void setTrieAccessor(TrieAccessor trieAccessor);
}
