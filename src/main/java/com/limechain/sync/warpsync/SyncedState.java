package com.limechain.sync.warpsync;

import com.limechain.chain.lightsyncstate.Authority;
import com.limechain.constants.GenesisBlockHash;
import com.limechain.network.Network;
import com.limechain.network.protocol.blockannounce.NodeRole;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandshake;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceMessage;
import com.limechain.network.protocol.grandpa.scale.NeighbourMessage;
import com.limechain.network.protocol.warp.dto.WarpSyncJustification;
import com.limechain.network.protocol.warp.scale.JustificationReader;
import com.limechain.network.substream.sync.pb.SyncMessage;
import com.limechain.rpc.http.server.AppBean;
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
    @Getter
    @Setter
    private Hash256 lastFinalizedBlockHash;
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
    private BigInteger latestRound = BigInteger.ZERO;

    @Getter
    @Setter
    private byte[] runtime;
    @Getter
    @Setter
    private byte[] heapPages;

    @Setter
    private Network network;

    @Setter
    private boolean warpSyncFinished;

    JustificationReader justificationReader = new JustificationReader();
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
        message.setRound(this.latestRound);
        message.setLastFinalizedBlock(lastFinalizedBlockNumber.longValue());
        return message;
    }

    public synchronized void syncAnnouncedBlock(PeerId peerId, BlockAnnounceMessage announce) {
        final var blockNumber = announce.getHeader().getBlockNumber();
        if (!warpSyncFinished || blockNumber.compareTo(lastFinalizedBlockNumber) <= 0) {
            return;
        }

        final var blockSyncResponse =  network.syncBlock(peerId, blockNumber, lastFinalizedBlockNumber);
        verifyBlockResponse(blockSyncResponse).ifPresentOrElse(
                this::updateLatestBlockData,
                () -> log.log(Level.WARNING, "Failed verifying block " + blockNumber + " from peer " + peerId)
        );
    }

    private Optional<WarpSyncJustification> verifyBlockResponse(SyncMessage.BlockResponse blockResponse) {
        WarpSyncJustification lastVerifiedJustification = null;
        for (SyncMessage.BlockData blockData : blockResponse.getBlocksList()) {
            final var justification = parseJustification(blockData.getJustification().toByteArray());
            if(verifyJustification(justification)) {
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

    private boolean verifyJustification(WarpSyncJustification justification) {
        return justification.verify(authoritySet, setId);
    }

    private void updateLatestBlockData(WarpSyncJustification justification) {
       lastFinalizedBlockNumber = justification.targetBlock;
       lastFinalizedBlockHash = justification.targetHash;
    }
}