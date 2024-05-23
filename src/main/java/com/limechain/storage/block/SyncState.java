package com.limechain.storage.block;

import com.limechain.chain.lightsyncstate.Authority;
import com.limechain.chain.lightsyncstate.LightSyncState;
import com.limechain.constants.GenesisBlockHash;
import com.limechain.network.protocol.grandpa.messages.commit.CommitMessage;
import com.limechain.network.protocol.warp.dto.Block;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.Getter;

import java.math.BigInteger;

@Getter
public class SyncState {

    private final GenesisBlockHash genesisBlockHashCalculator;

    private final BigInteger startingBlock;
    private final Hash256 genesisBlockHash;
    private BigInteger lastFinalizedBlockNumber;
    private Hash256 lastFinalizedBlockHash;
    private BigInteger latestRound;
    private Hash256 stateRoot;
    private BigInteger setId;
    private Authority[] authoritySet;

    public SyncState(final GenesisBlockHash genesisBlockHashCalculator) {
        this.genesisBlockHashCalculator = genesisBlockHashCalculator;
        this.genesisBlockHash = genesisBlockHashCalculator.getGenesisHash();

        this.setId = BigInteger.ZERO;
        this.latestRound = BigInteger.ONE;
        this.startingBlock = BigInteger.ZERO;
        this.lastFinalizedBlockNumber = startingBlock;
        this.lastFinalizedBlockHash = genesisBlockHash;
    }

    public void finalizeHeader(BlockHeader header) {
        this.lastFinalizedBlockNumber = header.getBlockNumber();
        this.lastFinalizedBlockHash = header.getHash();
        this.stateRoot = header.getStateRoot();
    }

    public void finalizedCommitMessage(CommitMessage commitMessage) {
        this.lastFinalizedBlockHash = commitMessage.getVote().getBlockHash();
        this.lastFinalizedBlockNumber = commitMessage.getVote().getBlockNumber();
        Block blockByHash = BlockState.getInstance().getBlockByHash(commitMessage.getVote().getBlockHash());
        if (blockByHash != null) {
            this.stateRoot = blockByHash.getHeader().getStateRoot();
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
        finalizeHeader(initState.getFinalizedBlockHeader());
    }

    public void setAuthoritySet(Authority[] authoritySet) {
        this.authoritySet = authoritySet;
    }
}
