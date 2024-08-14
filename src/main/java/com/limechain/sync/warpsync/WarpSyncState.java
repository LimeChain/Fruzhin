package com.limechain.sync.warpsync;

import com.limechain.chain.lightsyncstate.Authority;
import com.limechain.network.Network;
import com.limechain.storage.block.SyncState;
import com.limechain.tuple.Pair;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.logging.Level;

/**
 * Singleton class, holds and handles the synced state of the Host.
 */
@Log
@Setter
public class WarpSyncState {

    private final SyncState syncState;
    private final Network network;

    @Getter
    private boolean warpSyncFragmentsFinished;
    @Getter
    private boolean warpSyncFinished;

    private final Set<BigInteger> scheduledRuntimeUpdateBlocks;
    private final PriorityQueue<Pair<BigInteger, Authority[]>> scheduledAuthorityChanges;


    public WarpSyncState(SyncState syncState, Network network) {
        this(syncState,
            network,
            new HashSet<>(),
            new PriorityQueue<>(Comparator.comparing(Pair::getValue0)));
    }

    public WarpSyncState(SyncState syncState, Network network,
                         Set<BigInteger> scheduledRuntimeUpdateBlocks,
                         PriorityQueue<Pair<BigInteger, Authority[]>> scheduledAuthorityChanges) {
        this.syncState = syncState;
        this.network = network;
        this.scheduledRuntimeUpdateBlocks = scheduledRuntimeUpdateBlocks;
        this.scheduledAuthorityChanges = scheduledAuthorityChanges;
    }

//    /**
//     * Update the state with information from a block announce message.
//     * Schedule runtime updates found in header, to be executed when block is verified.
//     *
//     * @param blockAnnounceMessage received block announce message
//     */
//    public void syncBlockAnnounce(BlockAnnounceMessage blockAnnounceMessage) {
//        boolean hasRuntimeUpdate = Arrays.stream(blockAnnounceMessage.getHeader().getDigest())
//                .anyMatch(d -> d.getType() == DigestType.RUN_ENV_UPDATED);
//
//        if (hasRuntimeUpdate) {
//            scheduledRuntimeUpdateBlocks.add(blockAnnounceMessage.getHeader().getBlockNumber());
//        }
//    }

    /**
     * Updates the Host's state with information from a commit message.
     * Synchronized to avoid race condition between checking and updating latest block
     * Scheduled runtime updates for synchronized blocks are executed.
     *
     * @param commitMessage received commit message
     * @param peerId        sender of the message
     */
//    public synchronized void syncCommit(CommitMessage commitMessage, PeerId peerId) {
//        if (commitMessage.getVote().getBlockNumber().compareTo(syncState.getLastFinalizedBlockNumber()) <= 0) {
//            log.log(Level.FINE, String.format("Received commit message for finalized block %d from peer %s",
//                    commitMessage.getVote().getBlockNumber(), peerId));
//            return;
//        }
//
//        log.log(Level.INFO, "Received commit message from peer " + peerId
//                            + " for block #" + commitMessage.getVote().getBlockNumber()
//                            + " with hash " + commitMessage.getVote().getBlockHash()
//                            + " with setId " + commitMessage.getSetId() + " and round " + commitMessage.getRoundNumber()
//                            + " with " + commitMessage.getPrecommits().length + " voters");
//
//        boolean verified = JustificationVerifier.verify(commitMessage.getPrecommits(), commitMessage.getRoundNumber());
//        if (!verified) {
//            log.log(Level.WARNING, "Could not verify commit from peer: " + peerId);
//            return;
//        }
//
//        if (warpSyncFinished) {
//            updateState(commitMessage);
//        }
//    }

//    private void updateState(CommitMessage commitMessage) {
//        BigInteger lastFinalizedBlockNumber = syncState.getLastFinalizedBlockNumber();
//        if (commitMessage.getVote().getBlockNumber().compareTo(lastFinalizedBlockNumber) < 1) {
//            return;
//        }
//        syncState.finalizedCommitMessage(commitMessage);
//
//        log.log(Level.INFO, "Reached block #" + lastFinalizedBlockNumber);
//    }

    /**
     * Updates the Host's state with information from a neighbour message.
     * Tries to update Host's set data (id and authorities) if neighbour has a greater set id than the Host.
     * Synchronized to avoid race condition between checking and updating set id
     *
     * @param neighbourMessage received neighbour message
     * @param peerId           sender of message
     */
//    public void syncNeighbourMessage(NeighbourMessage neighbourMessage, PeerId peerId) {
//        network.sendNeighbourMessage(peerId);
//        if (warpSyncFinished && neighbourMessage.getSetId().compareTo(syncState.getSetId()) > 0) {
//            updateSetData(neighbourMessage.getLastFinalizedBlock().add(BigInteger.ONE), peerId);
//        }
//    }

//    private void updateSetData(BigInteger setChangeBlock, PeerId peerId) {
//        BlockResponse response = network.syncBlock(peerId, setChangeBlock);
//        BlockData block = response.getBlocksList().get(0);
//
//        if (block.getIsEmptyJustification()) {
//            log.log(Level.WARNING, "No justification for block " + setChangeBlock);
//            return;
//        }
//
//        Justification justification = new JustificationReader().read(
//                new ScaleCodecReader(block.getJustification().toByteArray()));
//        boolean verified = justification != null
//                           && JustificationVerifier.verify(justification.getPrecommits(), justification.getRound());
//
//        if (verified) {
//            BlockHeader header = new BlockHeaderReader().read(new ScaleCodecReader(block.getHeader().toByteArray()));
//
//            syncState.finalizeHeader(header);
//            handleAuthorityChanges(header.getDigest(), setChangeBlock);
//            handleScheduledEvents();
//        }
//    }

    /**
     * Executes authority changes, scheduled for the current block.
     */
    public void handleScheduledEvents() {
        Pair<BigInteger, Authority[]> data = scheduledAuthorityChanges.peek();
        BigInteger setId = syncState.getSetId();
        boolean updated = false;
        while (data != null) {
            if (data.getValue0().compareTo(syncState.getLastFinalizedBlockNumber()) < 1) {
                setId = syncState.incrementSetId();
                syncState.resetRound();
                syncState.setAuthoritySet(data.getValue1());
                scheduledAuthorityChanges.poll();
                updated = true;
            } else break;
            data = scheduledAuthorityChanges.peek();
        }
        if (warpSyncFinished && updated) {
            log.log(Level.INFO, "Successfully transitioned to authority set id: " + setId);
            new Thread(network::sendNeighbourMessages).start();
        }
    }

    /**
     * Handles authority changes coming from a block header digest and schedules them.
     *
     * @param headerDigests digest of the block header
     * @param blockNumber   block that contains the digest
     */
//    public void handleAuthorityChanges(HeaderDigest[] headerDigests, BigInteger blockNumber) {
//        // Update authority set and set id
//        AuthoritySetChange authorityChanges;
//        for (HeaderDigest digest : headerDigests) {
//            if (digest.getId() == ConsensusEngine.GRANDPA) {
//                ScaleCodecReader reader = new ScaleCodecReader(digest.getMessage());
//                GrandpaDigestMessageType type = GrandpaDigestMessageType.fromId(reader.readByte());
//
//                if (type == null) {
//                    log.log(Level.SEVERE, "Could not get grandpa message type");
//                    throw new IllegalStateException("Unknown grandpa message type");
//                }
//
//                switch (type) {
//                    case SCHEDULED_CHANGE -> {
//                        ScheduledChangeReader authorityChangesReader = new ScheduledChangeReader();
//                        authorityChanges = authorityChangesReader.read(reader);
//                        scheduledAuthorityChanges
//                                .add(new Pair<>(blockNumber.add(authorityChanges.getDelay()),
//                                        authorityChanges.getAuthorities()));
//                        return;
//                    }
//                    case FORCED_CHANGE -> {
//                        ForcedChangeReader authorityForcedChangesReader = new ForcedChangeReader();
//                        authorityChanges = authorityForcedChangesReader.read(reader);
//                        scheduledAuthorityChanges
//                                .add(new Pair<>(blockNumber.add(authorityChanges.getDelay()),
//                                        authorityChanges.getAuthorities()));
//                        return;
//                    }
//                    case ON_DISABLED -> {
//                        log.log(Level.SEVERE, "'ON DISABLED' grandpa message not implemented");
//                        return;
//                    }
//                    case PAUSE -> {
//                        log.log(Level.SEVERE, "'PAUSE' grandpa message not implemented");
//                        return;
//                    }
//                    case RESUME -> {
//                        log.log(Level.SEVERE, "'RESUME' grandpa message not implemented");
//                        return;
//                    }
//                }
//            }
//        }
//    }

}