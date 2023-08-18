package com.limechain.sync.warpsync;

import com.limechain.chain.lightsyncstate.Authority;
import com.limechain.constants.GenesisBlockHash;
import com.limechain.network.Network;
import com.limechain.network.protocol.blockannounce.NodeRole;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandshake;
import com.limechain.network.protocol.grandpa.messages.commit.CommitMessage;
import com.limechain.network.protocol.grandpa.messages.neighbour.NeighbourMessage;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.network.protocol.warp.dto.ConsensusEngine;
import com.limechain.network.protocol.warp.dto.HeaderDigest;
import com.limechain.network.protocol.warp.scale.BlockHeaderReader;
import com.limechain.network.protocol.warp.scale.JustificationReader;
import com.limechain.rpc.server.AppBean;
import com.limechain.runtime.Runtime;
import com.limechain.sync.JustificationVerifier;
import com.limechain.sync.warpsync.dto.AuthoritySetChange;
import com.limechain.sync.warpsync.dto.GrandpaMessageType;
import com.limechain.sync.warpsync.scale.ForcedChangeReader;
import com.limechain.sync.warpsync.scale.ScheduledChangeReader;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.types.Hash256;
import io.libp2p.core.PeerId;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.javatuples.Pair;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.logging.Level;

import static com.limechain.network.protocol.sync.pb.SyncMessage.BlockResponse;

@Log
public class SyncedState {
    private static final SyncedState INSTANCE = new SyncedState();
    public static final int NEIGHBOUR_MESSAGE_VERSION = 1;
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
    private BigInteger setId = BigInteger.ZERO;

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
    @Getter
    private boolean warpSyncFinished;
    private final PriorityQueue<Pair<BigInteger, Authority[]>> scheduledAuthorityChanges =
            new PriorityQueue<>(Comparator.comparing(Pair::getValue0));
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
    }

    public void updateSetData(NeighbourMessage neighbourMessage, PeerId peerId) {
        BigInteger setChangeBlock = neighbourMessage.getLastFinalizedBlock().add(BigInteger.ONE);

        BlockResponse response = network.syncBlock(peerId, setChangeBlock);
        var block = response.getBlocksList().get(0);

        var justification = new JustificationReader().read(
                new ScaleCodecReader(block.getJustification().toByteArray()));
        boolean verified = JustificationVerifier.verify(justification.precommits, justification.round);
        if (verified) {
            var header = new BlockHeaderReader().read(new ScaleCodecReader(block.getHeader().toByteArray()));
            handleAuthorityChanges(header.getDigest(), setChangeBlock);
            BlockHeader blockHeader = new BlockHeaderReader().read(
                    new ScaleCodecReader(block.getHeader().toByteArray()));
            this.lastFinalizedBlockNumber = blockHeader.getBlockNumber();
            this.lastFinalizedBlockHash = new Hash256(blockHeader.getHash());
            handleScheduledEvents();
        }
    }

    public void handleScheduledEvents() {
        Pair<BigInteger, Authority[]> data = scheduledAuthorityChanges.peek();
        boolean updated = false;
        while (data != null) {
            if (data.getValue0().compareTo(this.getLastFinalizedBlockNumber()) < 1) {
                authoritySet = data.getValue1();
                setId = setId.add(BigInteger.ONE);
                latestRound = BigInteger.ONE;
                scheduledAuthorityChanges.poll();
                updated = true;
            } else break;
            data = scheduledAuthorityChanges.peek();
        }
        if (warpSyncFinished && updated) {
            new Thread(() -> network.sendNeighbourMessages()).start();
        }
    }

    public void handleAuthorityChanges(HeaderDigest[] headerDigests, BigInteger blockNumber) {
        // Update authority set and set id
        AuthoritySetChange authorityChanges;
        for (HeaderDigest digest : headerDigests) {
            if (digest.getId() == ConsensusEngine.GRANDPA) {
                ScaleCodecReader reader = new ScaleCodecReader(digest.getMessage());
                GrandpaMessageType type = GrandpaMessageType.fromId(reader.readByte());

                if (type == null) {
                    log.log(Level.SEVERE, "Could not get grandpa message type");
                    throw new IllegalStateException("Unknown grandpa message type");
                }

                switch (type) {
                    case SCHEDULED_CHANGE -> {
                        ScheduledChangeReader authorityChangesReader = new ScheduledChangeReader();
                        authorityChanges = authorityChangesReader.read(reader);
                        scheduledAuthorityChanges
                                .add(new Pair<>(blockNumber.add(authorityChanges.getDelay()),
                                        authorityChanges.getAuthorities()));
                        return;
                    }
                    case FORCED_CHANGE -> {
                        ForcedChangeReader authorityForcedChangesReader = new ForcedChangeReader();
                        authorityChanges = authorityForcedChangesReader.read(reader);
                        scheduledAuthorityChanges
                                .add(new Pair<>(blockNumber.add(authorityChanges.getDelay()),
                                        authorityChanges.getAuthorities()));
                        return;
                    }
                    case ON_DISABLED -> {
                        log.log(Level.SEVERE, "'ON DISABLED' grandpa message not implemented");
                        return;
                    }
                    case PAUSE -> {
                        log.log(Level.SEVERE, "'PAUSE' grandpa message not implemented");
                        return;
                    }
                    case RESUME -> {
                        log.log(Level.SEVERE, "'RESUME' grandpa message not implemented");
                        return;
                    }
                }
            }
        }
    }
}