package com.limechain.sync.warpsync;

import com.limechain.chain.lightsyncstate.Authority;
import com.limechain.constants.GenesisBlockHash;
import com.limechain.network.Network;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandshake;
import com.limechain.network.protocol.grandpa.messages.commit.CommitMessage;
import com.limechain.network.protocol.grandpa.messages.neighbour.NeighbourMessage;
import com.limechain.network.protocol.sync.pb.SyncMessage.BlockResponse;
import com.limechain.network.protocol.sync.pb.SyncMessage.BlockData;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.network.protocol.warp.dto.ConsensusEngine;
import com.limechain.network.protocol.warp.dto.HeaderDigest;
import com.limechain.network.protocol.warp.dto.Justification;
import com.limechain.network.protocol.warp.scale.BlockHeaderReader;
import com.limechain.network.protocol.warp.scale.JustificationReader;
import com.limechain.rpc.server.AppBean;
import com.limechain.runtime.Runtime;
import com.limechain.storage.DBConstants;
import com.limechain.storage.KVRepository;
import com.limechain.sync.JustificationVerifier;
import com.limechain.sync.warpsync.dto.AuthoritySetChange;
import com.limechain.sync.warpsync.dto.GrandpaDigestMessageType;
import com.limechain.sync.warpsync.scale.ForcedChangeReader;
import com.limechain.sync.warpsync.scale.ScheduledChangeReader;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import com.limechain.sync.warpsync.dto.StateDto;
import com.limechain.trie.Trie;
import io.emeraldpay.polkaj.types.Hash256;
import io.libp2p.core.PeerId;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.javatuples.Pair;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Singleton class, holds and handles the synced state of the Host.
 */
@Getter
@Setter
@Log
public class SyncedState {
    private static final SyncedState INSTANCE = new SyncedState();

    protected SyncedState() {
    }

    public static SyncedState getInstance() {
        return INSTANCE;
    }

    public static final int NEIGHBOUR_MESSAGE_VERSION = 1;

    private boolean warpSyncFragmentsFinished;
    private boolean warpSyncFinished;

    private BigInteger lastFinalizedBlockNumber = BigInteger.ZERO;
    private Hash256 lastFinalizedBlockHash;
    private Hash256 stateRoot;

    private Authority[] authoritySet;
    private BigInteger setId = BigInteger.ZERO;
    private BigInteger latestRound = BigInteger.ONE;

    private Runtime runtime;
    private byte[] runtimeCode;
    private byte[] heapPages;

    private KVRepository<String, Object> repository;
    private Trie trie;
    private Network network;

    private final PriorityQueue<Pair<BigInteger, Authority[]>> scheduledAuthorityChanges =
            new PriorityQueue<>(Comparator.comparing(Pair::getValue0));

    /**
     * Creates a Block Announce handshake based on the latest finalized Host state
     *
     * @return our Block Announce handshake
     */
    public BlockAnnounceHandshake getHandshake() {
        Hash256 genesisBlockHash;
        if (network == null)
            network = AppBean.getBean(Network.class);

        switch (network.getChain()) {
            case POLKADOT -> genesisBlockHash = GenesisBlockHash.POLKADOT;
            case KUSAMA -> genesisBlockHash = GenesisBlockHash.KUSAMA;
            case WESTEND -> genesisBlockHash = GenesisBlockHash.WESTEND;
            case LOCAL -> genesisBlockHash = GenesisBlockHash.LOCAL;
            default -> throw new IllegalStateException("Unexpected value: " + network.chain);
        }

        Hash256 lastFinalizedBlockHash = this.lastFinalizedBlockHash == null
                ? genesisBlockHash
                : this.lastFinalizedBlockHash;
        return new BlockAnnounceHandshake(
                network.getNodeRole().getValue(),
                this.lastFinalizedBlockNumber,
                lastFinalizedBlockHash,
                genesisBlockHash
        );
    }

    /**
     * Creates a GRANDPA handshake based on the latest finalized Host state
     *
     * @return our GRANDPA handshake
     */
    public NeighbourMessage getNeighbourMessage() {
        return new NeighbourMessage(
                NEIGHBOUR_MESSAGE_VERSION,
                this.latestRound,
                this.setId,
                this.lastFinalizedBlockNumber
        );
    }

    /**
     * Updates the Host's state with information from a commit message.
     * Synchronized to avoid race condition between checking and updating latest block
     *
     * @param commitMessage received commit message
     * @param peerId        sender of the message
     */
    public synchronized void syncCommit(CommitMessage commitMessage, PeerId peerId) {
        if (commitMessage.getVote().getBlockNumber().compareTo(lastFinalizedBlockNumber) <= 0) {
            log.log(Level.FINE, String.format("Received commit message for finalized block %d from peer %s",
                    commitMessage.getVote().getBlockNumber(), peerId));
            return;
        }

        log.log(Level.INFO, "Received commit message from peer " + peerId
                + " for block #" + commitMessage.getVote().getBlockNumber()
                + " with hash " + commitMessage.getVote().getBlockHash()
                + " with setId " + commitMessage.getSetId() + " and round " + commitMessage.getRoundNumber()
                + " with " + commitMessage.getPrecommits().length + " voters");

        boolean verified = JustificationVerifier.verify(commitMessage.getPrecommits(), commitMessage.getRoundNumber());
        if (!verified) {
            log.log(Level.WARNING, "Could not verify commit from peer: " + peerId);
            return;
        }

        if (warpSyncFinished) {
            updateState(commitMessage);
        }
    }

    private void updateState(CommitMessage commitMessage) {
        if (commitMessage.getVote().getBlockNumber().compareTo(lastFinalizedBlockNumber) < 1) {
            return;
        }
        latestRound = commitMessage.getRoundNumber();
        lastFinalizedBlockHash = commitMessage.getVote().getBlockHash();
        lastFinalizedBlockNumber = commitMessage.getVote().getBlockNumber();
        log.log(Level.INFO, "Reached block #" + lastFinalizedBlockNumber);
        persistState();
    }

    /**
     * Updates the Host's state with information from a neighbour message.
     * Tries to update Host's set data (id and authorities) if neighbour has a greater set id than the Host.
     * Synchronized to avoid race condition between checking and updating set id
     *
     * @param neighbourMessage received neighbour message
     * @param peerId           sender of message
     */
    public synchronized void syncNeighbourMessage(NeighbourMessage neighbourMessage, PeerId peerId) {
        if (warpSyncFinished && neighbourMessage.getSetId().compareTo(setId) > 0) {
            updateSetData(neighbourMessage.getLastFinalizedBlock().add(BigInteger.ONE), peerId);
        }
    }

    private void updateSetData(BigInteger setChangeBlock, PeerId peerId) {
        BlockResponse response = network.syncBlock(peerId, setChangeBlock);
        BlockData block = response.getBlocksList().get(0);

        if (block.getIsEmptyJustification()) {
            log.log(Level.WARNING, "No justification for block " + setChangeBlock);
            return;
        }

        Justification justification = new JustificationReader().read(
                new ScaleCodecReader(block.getJustification().toByteArray()));
        boolean verified = justification != null
                && JustificationVerifier.verify(justification.precommits, justification.round);

        if (verified) {
            BlockHeader header = new BlockHeaderReader().read(new ScaleCodecReader(block.getHeader().toByteArray()));
            this.lastFinalizedBlockNumber = header.getBlockNumber();
            this.lastFinalizedBlockHash = new Hash256(header.getHash());

            handleAuthorityChanges(header.getDigest(), setChangeBlock);
            handleScheduledEvents();
            persistState();
        }
    }

    /**
     * Executes authority changes, scheduled for the current block.
     */
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
            log.log(Level.INFO, "Successfully transitioned to authority set id: " + setId);
            new Thread(() -> network.sendNeighbourMessages()).start();
        }
    }

    /**
     * Handles authority changes coming from a block header digest and schedules them.
     *
     * @param headerDigests digest of the block header
     * @param blockNumber   block that contains the digest
     */
    public void handleAuthorityChanges(HeaderDigest[] headerDigests, BigInteger blockNumber) {
        // Update authority set and set id
        AuthoritySetChange authorityChanges;
        for (HeaderDigest digest : headerDigests) {
            if (digest.getId() == ConsensusEngine.GRANDPA) {
                ScaleCodecReader reader = new ScaleCodecReader(digest.getMessage());
                GrandpaDigestMessageType type = GrandpaDigestMessageType.fromId(reader.readByte());

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

    /**
     * Persists the Host's current state to the DB.
     */
    public void persistState() {
        List<Pair<String, BigInteger>> authorities = Arrays
                .stream(authoritySet)
                .map(authority -> {
                    String key = authority.getPublicKey().toString();
                    BigInteger weight = authority.getWeight();
                    return new Pair<>(key, weight);
                }).toList();

        StateDto stateDto = new StateDto(
                latestRound,
                lastFinalizedBlockHash.toString(),
                lastFinalizedBlockNumber,
                authorities,
                setId
        );
        repository.save(DBConstants.SYNC_STATE_KEY, stateDto);
    }

    /**
     * Loads the Host's saved state from the DB.
     *
     * @return is the state loaded successfully
     */
    public boolean loadState() {
        Optional<Object> syncState = repository.find(DBConstants.SYNC_STATE_KEY);
        if (syncState.isPresent() && syncState.get() instanceof StateDto state) {
            this.latestRound = state.latestRound();
            this.lastFinalizedBlockHash = Hash256.from(state.lastFinalizedBlockHash());
            this.lastFinalizedBlockNumber = state.lastFinalizedBlockNumber();
            this.authoritySet = state.authoritySet()
                    .stream()
                    .map(pair -> {
                        Hash256 publicKey = Hash256.from(pair.getValue0());
                        BigInteger weight = pair.getValue1();
                        return new Authority(publicKey, weight);
                    }).toArray(Authority[]::new);

            this.setId = state.setId();

            return true;
        }
        return false;
    }

    /**
     * Saves the merkle proof and root hash of the state trie
     *
     * @param proof array of proof byte arrays
     */
    public void saveProofState(byte[][] proof) {
        repository.save(DBConstants.STATE_TRIE_MERKLE_PROOF, proof);
        repository.save(DBConstants.STATE_TRIE_ROOT_HASH, stateRoot.toString());
    }

    /**
     * Loads the saved state trie merkle proof
     *
     * @return array of proof byte arrays
     */

    public byte[][] loadProof() {
        return (byte[][]) repository.find(DBConstants.STATE_TRIE_MERKLE_PROOF).orElse(null);
    }

    /**
     * Loads the saved state trie root hash
     *
     * @return root hash
     */

    public Hash256 loadStateRoot() {
        Object storedRootState = repository.find(DBConstants.STATE_TRIE_ROOT_HASH).orElse(null);
        return storedRootState == null ? null : Hash256.from(storedRootState.toString());
    }
}