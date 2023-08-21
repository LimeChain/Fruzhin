package com.limechain.sync.warpsync;

import com.limechain.chain.lightsyncstate.Authority;
import com.limechain.constants.GenesisBlockHash;
import com.limechain.network.Network;
import com.limechain.network.protocol.blockannounce.NodeRole;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandshake;
import com.limechain.network.protocol.grandpa.messages.commit.CommitMessage;
import com.limechain.network.protocol.grandpa.messages.neighbour.NeighbourMessage;
import com.limechain.rpc.server.AppBean;
import com.limechain.runtime.Runtime;
import com.limechain.storage.DBConstants;
import com.limechain.storage.KVRepository;
import com.limechain.sync.JustificationVerifier;
import com.limechain.sync.warpsync.dto.StateDto;
import io.emeraldpay.polkaj.types.Hash256;
import io.libp2p.core.PeerId;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

import java.math.BigInteger;
import java.util.Optional;
import java.util.logging.Level;

@Getter
@Setter
@Log
public class SyncedState {
    public static final int NEIGHBOUR_MESSAGE_VERSION = 1;
    private static final SyncedState INSTANCE = new SyncedState();
    private Hash256 lastFinalizedBlockHash;
    private boolean isFinished;
    private Hash256 stateRoot;
    private BigInteger lastFinalizedBlockNumber = BigInteger.ZERO;
    private Authority[] authoritySet;
    private BigInteger setId;

    private BigInteger latestSetId = BigInteger.ZERO;

    private BigInteger latestRound = BigInteger.ONE;

    private byte[] runtimeCode;
    private byte[] heapPages;
    private Runtime runtime;
    private boolean warpSyncFinished;
    private KVRepository<String, Object> repository;

    public static SyncedState getInstance() {
        return INSTANCE;
    }

    public BlockAnnounceHandshake getHandshake() {
        Hash256 genesisBlockHash;
        Network network = AppBean.getBean(Network.class);
        switch (network.getChain()) {
            case POLKADOT -> genesisBlockHash = GenesisBlockHash.POLKADOT;
            case KUSAMA -> genesisBlockHash = GenesisBlockHash.KUSAMA;
            case WESTEND -> genesisBlockHash = GenesisBlockHash.WESTEND;
            case LOCAL -> genesisBlockHash = GenesisBlockHash.LOCAL;
            default -> throw new IllegalStateException("Unexpected value: " + network.chain);
        }

        Hash256 lastFinalizedBlockHash = this.getLastFinalizedBlockHash() == null
                ? genesisBlockHash
                : this.getLastFinalizedBlockHash();
        return new BlockAnnounceHandshake(
                NodeRole.LIGHT.getValue(),
                this.getLastFinalizedBlockNumber(),
                lastFinalizedBlockHash,
                genesisBlockHash
        );
    }

    public NeighbourMessage getNeighbourMessage() {
        return new NeighbourMessage(
                NEIGHBOUR_MESSAGE_VERSION,
                this.latestRound,
                this.setId,
                this.lastFinalizedBlockNumber
        );
    }

    public void syncCommit(CommitMessage commitMessage, PeerId peerId) {
        boolean verified = JustificationVerifier.verify(commitMessage.getPrecommits(), commitMessage.getRoundNumber());
        if (!verified) {
            log.log(Level.WARNING, "Could not verify commit from peer: " + peerId);
            return;
        }

        if (warpSyncFinished) {
            updateState(commitMessage);
        }
    }

    private synchronized void updateState(CommitMessage commitMessage) {
        if (commitMessage.getVote().getBlockNumber().compareTo(lastFinalizedBlockNumber) < 1) {
            return;
        }
        latestRound = commitMessage.getRoundNumber();
        lastFinalizedBlockHash = commitMessage.getVote().getBlockHash();
        lastFinalizedBlockNumber = commitMessage.getVote().getBlockNumber();
        log.log(Level.INFO, "Reached block #" + lastFinalizedBlockNumber);
        saveState();
    }

    public void saveState() {
        StateDto stateDto = new StateDto(
                latestRound,
                lastFinalizedBlockHash.toString(),
                lastFinalizedBlockNumber
        );
        repository.save(DBConstants.SYNC_STATE_KEY, stateDto);
    }

    public void loadState() {
        Optional<Object> syncState = repository.find(DBConstants.SYNC_STATE_KEY);
        if (syncState.isPresent() && syncState.get() instanceof StateDto state) {
            this.latestRound = state.latestRound();
            this.lastFinalizedBlockHash = Hash256.from(state.lastFinalizedBlockHash());
            this.lastFinalizedBlockNumber = state.lastFinalizedBlockNumber();
        }
    }
}