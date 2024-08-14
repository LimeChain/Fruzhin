package com.limechain.storage.block;

import com.limechain.chain.lightsyncstate.Authority;
import com.limechain.chain.lightsyncstate.LightSyncState;
import com.limechain.constants.GenesisBlockHash;
import com.limechain.exception.storage.HeaderNotFoundException;
import com.limechain.network.protocol.grandpa.messages.commit.CommitMessage;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.polkaj.Hash256;
import com.limechain.storage.DBConstants;
import com.limechain.storage.LocalStorage;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

import java.math.BigInteger;
import java.util.Arrays;

@Getter
@Log
public class SyncState {

    private BigInteger lastFinalizedBlockNumber;
    private final BigInteger startingBlock;
    private final Hash256 genesisBlockHash;
    private Hash256 lastFinalizedBlockHash;
    @Setter
    private Authority[] authoritySet;
    private BigInteger latestRound;
    private BigInteger setId;

    public SyncState() {
        this.genesisBlockHash = GenesisBlockHash.POLKADOT;

        loadState();
        this.startingBlock = this.lastFinalizedBlockNumber;
    }

    private void loadState() {
        this.lastFinalizedBlockNumber = LocalStorage.find(
            DBConstants.LAST_FINALIZED_BLOCK_NUMBER, BigInteger.class).orElse(BigInteger.ZERO);
        this.lastFinalizedBlockHash = new Hash256(LocalStorage.find(
            DBConstants.LAST_FINALIZED_BLOCK_HASH, byte[].class).orElse(genesisBlockHash.getBytes()));
        this.authoritySet = LocalStorage.find(DBConstants.AUTHORITY_SET, Authority[].class).orElse(new Authority[0]);
        this.latestRound = LocalStorage.find(DBConstants.LATEST_ROUND, BigInteger.class).orElse(BigInteger.ONE);
        this.setId = LocalStorage.find(DBConstants.SET_ID, BigInteger.class).orElse(BigInteger.ZERO);
    }

    public void persistState() {
        LocalStorage.save(DBConstants.LAST_FINALIZED_BLOCK_NUMBER, lastFinalizedBlockNumber);
        LocalStorage.save(DBConstants.LAST_FINALIZED_BLOCK_HASH, lastFinalizedBlockHash.getBytes());
        LocalStorage.save(DBConstants.AUTHORITY_SET, authoritySet);
        LocalStorage.save(DBConstants.LATEST_ROUND, latestRound);
        LocalStorage.save(DBConstants.SET_ID, setId);
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
