package com.limechain.storage.block;

import com.limechain.chain.lightsyncstate.Authority;
import com.limechain.chain.lightsyncstate.LightSyncState;
import com.limechain.constants.GenesisBlockHash;
import com.limechain.exception.storage.HeaderNotFoundException;
import com.limechain.network.protocol.grandpa.messages.commit.CommitMessage;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.polkaj.Hash256;
import com.limechain.storage.DBConstants;
import com.limechain.storage.KVRepository;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

import java.math.BigInteger;

@Getter
@Log
public class SyncState {

    private final KVRepository<String, Object> repository;
    private BigInteger lastFinalizedBlockNumber;
    private final BigInteger startingBlock;
    private final Hash256 genesisBlockHash;
    private Hash256 lastFinalizedBlockHash;
    @Setter
    private Authority[] authoritySet;
    private BigInteger latestRound;
    private BigInteger setId;

    public SyncState(KVRepository<String, Object> repository) {
        this.genesisBlockHash = GenesisBlockHash.POLKADOT;
        this.repository = repository;

        if (repository != null) {
            loadPersistedState();
        } else {
            loadDefaultState();
        }
        this.startingBlock = this.lastFinalizedBlockNumber;
    }

    private void loadDefaultState() {
        this.lastFinalizedBlockNumber = BigInteger.ZERO;
        this.lastFinalizedBlockHash = new Hash256(genesisBlockHash.getBytes());
        this.authoritySet = new Authority[0];
        this.latestRound = BigInteger.ONE;
        this.setId = BigInteger.ZERO;
    }

    private void loadPersistedState() {
        this.lastFinalizedBlockNumber =
                (BigInteger) repository.find(DBConstants.LAST_FINALIZED_BLOCK_NUMBER).orElse(BigInteger.ZERO);
        this.lastFinalizedBlockHash = new Hash256(
                (byte[]) repository.find(DBConstants.LAST_FINALIZED_BLOCK_HASH).orElse(genesisBlockHash.getBytes()));
        this.authoritySet = (Authority[]) repository.find(DBConstants.AUTHORITY_SET).orElse(new Authority[0]);
        this.latestRound = (BigInteger) repository.find(DBConstants.LATEST_ROUND).orElse(BigInteger.ONE);
        this.setId = (BigInteger) repository.find(DBConstants.SET_ID).orElse(BigInteger.ZERO);
    }

    public void persistState() {
        repository.save(DBConstants.LAST_FINALIZED_BLOCK_NUMBER, lastFinalizedBlockNumber);
        repository.save(DBConstants.LAST_FINALIZED_BLOCK_HASH, lastFinalizedBlockHash.getBytes());
        repository.save(DBConstants.AUTHORITY_SET, authoritySet);
        repository.save(DBConstants.LATEST_ROUND, latestRound);
        repository.save(DBConstants.SET_ID, setId);
    }

    public void finalizeHeader(BlockHeader header) {
        this.lastFinalizedBlockNumber = header.getBlockNumber();
        this.lastFinalizedBlockHash = header.getHash();
    }

    public void finalizedCommitMessage(CommitMessage commitMessage) {
        try {
            this.lastFinalizedBlockHash = commitMessage.getVote().getBlockHash();
            this.lastFinalizedBlockNumber = commitMessage.getVote().getBlockNumber();
        } catch (HeaderNotFoundException ignored) {
            log.fine("Received commit message for a block that is not in the block store");
        }
    }

    public BigInteger incrementSetId() {
        this.setId = this.setId.add(BigInteger.ONE);
        return setId;
    }

    public void resetRound() {
        this.latestRound = BigInteger.ONE;
    }

    public void setLightSyncState(LightSyncState initState) {
        this.setId = initState.getGrandpaAuthoritySet().getSetId();
        setAuthoritySet(initState.getGrandpaAuthoritySet().getCurrentAuthorities());
//        finalizeHeader(initState.getFinalizedBlockHeader());
    }

    public String getStateRoot() {
        return null;
    }
}
