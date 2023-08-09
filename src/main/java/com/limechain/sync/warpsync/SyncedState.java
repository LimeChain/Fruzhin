package com.limechain.sync.warpsync;

import com.limechain.chain.lightsyncstate.Authority;
import com.limechain.constants.GenesisBlockHash;
import com.limechain.network.Network;
import com.limechain.network.protocol.blockannounce.NodeRole;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandshake;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceMessage;
import com.limechain.network.protocol.grandpa.messages.commit.CommitMessage;
import com.limechain.network.protocol.grandpa.messages.neighbour.NeighbourMessage;
import com.limechain.network.protocol.sync.pb.SyncMessage;
import com.limechain.network.protocol.warp.dto.WarpSyncJustification;
import com.limechain.network.protocol.warp.scale.JustificationReader;
import com.limechain.rpc.http.server.AppBean;
import com.limechain.runtime.Runtime;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.types.Hash256;
import io.libp2p.core.PeerId;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

import java.math.BigInteger;
import java.util.Optional;
import java.util.logging.Level;

@Log
public class SyncedState {
    private static final SyncedState INSTANCE = new SyncedState();
    JustificationReader justificationReader = new JustificationReader();
    @Getter
    @Setter
    private Hash256 lastFinalizedBlockHash;
    @Getter
    @Setter
    private boolean isFinished;
    @Getter
    @Setter
    private Hash256 stateRoot;
    @Getter
    @Setter
    private BigInteger lastFinalizedBlockNumber = BigInteger.ZERO;
    @Getter
    @Setter
    private Authority[] authoritySet;
    @Getter
    @Setter
    private BigInteger setId;

    @Getter
    @Setter
    private BigInteger latestSetId = BigInteger.ZERO;

    @Getter
    @Setter
    private BigInteger latestRound = BigInteger.ONE;

    @Getter
    @Setter
    private byte[] runtimeCode;
    @Getter
    @Setter
    private byte[] heapPages;
    @Getter
    @Setter
    private Runtime runtime;
    @Setter
    private Network network;
    @Setter
    private boolean warpSyncFinished;

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
                this.getLastFinalizedBlockNumber().toString(),
                lastFinalizedBlockHash,
                genesisBlockHash
        );
    }

    public NeighbourMessage getNeighbourMessage() {
        NeighbourMessage message = new NeighbourMessage();
        message.setVersion(1);
        message.setSetId(this.setId);
        message.setRound(BigInteger.ONE);
        message.setLastFinalizedBlock(this.lastFinalizedBlockNumber.longValue());
        return message;
    }

    public synchronized void syncAnnouncedBlock(PeerId peerId, BlockAnnounceMessage announce) {
        final var blockNumber = announce.getHeader().getBlockNumber();
        if (!warpSyncFinished || blockNumber.compareTo(lastFinalizedBlockNumber) <= 0) {
            return;
        }

        final var blockSyncResponse = network.syncBlock(peerId, blockNumber, lastFinalizedBlockNumber);
        verifyBlockResponse(blockSyncResponse).ifPresentOrElse(
                this::updateLatestBlockData,
                () -> log.log(Level.WARNING, "Failed verifying block " + blockNumber + " from peer " + peerId)
        );
    }

    private Optional<WarpSyncJustification> verifyBlockResponse(SyncMessage.BlockResponse blockResponse) {
        WarpSyncJustification lastVerifiedJustification = null;
        for (SyncMessage.BlockData blockData : blockResponse.getBlocksList()) {
            final var justification = parseJustification(blockData.getJustification().toByteArray());
            if (verifyJustification(justification)) {
                lastVerifiedJustification = justification;
            } else {
                //TODO: failed verification logic
                return Optional.empty();
            }
        }
        return Optional.ofNullable(lastVerifiedJustification);
    }

    // TODO: Not verified if this is the right justification format
    private WarpSyncJustification parseJustification(byte[] bytes) {
        return justificationReader.read(new ScaleCodecReader(bytes));
    }

    public void syncCommit(CommitMessage commitMessage, PeerId peerId) {
        if(!verifyCommitJustification(commitMessage)) {
            log.log(Level.WARNING, "Could not verify commit from peer: " + peerId);
            return;
        }

        if(warpSyncFinished) {
            updateState(commitMessage);
        }
    }

    private void updateState(CommitMessage commitMessage) {
        latestRound = commitMessage.getRoundNumber();
        lastFinalizedBlockHash = commitMessage.getVote().getBlockHash();
        lastFinalizedBlockNumber = BigInteger.valueOf(commitMessage.getVote().getBlockNumber());
    }

    //TODO: implement when right authority set is received
    public boolean verifyCommitJustification(CommitMessage commitMessage) {
        /*WarpSyncJustification justification = new WarpSyncJustification();
        justification.setRound(commitMessage.getRoundNumber());
        justification.setTargetHash(commitMessage.getVote().getBlockHash());
        justification.setTargetBlock(BigInteger.valueOf(commitMessage.getVote().getBlockNumber()));
        justification.setPrecommits(commitMessage.getPrecommits());
        return verifyJustification(justification);*/
        return true;
    }

    public boolean verifyJustification(WarpSyncJustification justification) {
        return justification.verify(authoritySet, setId);
    }

    private void updateLatestBlockData(WarpSyncJustification justification) {
        lastFinalizedBlockNumber = justification.targetBlock;
        lastFinalizedBlockHash = justification.targetHash;
    }
}