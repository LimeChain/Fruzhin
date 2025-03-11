package com.limechain.network.protocol.grandpa;

import com.limechain.exception.grandpa.GrandpaGenericException;
import com.limechain.exception.storage.HeaderNotFoundException;
import com.limechain.exception.storage.LowerThanRootException;
import com.limechain.exception.sync.JustificationVerificationException;
import com.limechain.consensus.grandpa.GrandpaService;
import com.limechain.consensus.grandpa.round.GrandpaRound;
import com.limechain.consensus.grandpa.dto.GrandpaAuthoritySet;
import com.limechain.consensus.grandpa.GrandpaSetState;
import com.limechain.consensus.grandpa.dto.RoundState;
import com.limechain.consensus.grandpa.dto.SignedVote;
import com.limechain.consensus.grandpa.dto.SubRound;
import com.limechain.consensus.grandpa.dto.Vote;
import com.limechain.network.PeerMessageCoordinator;
import com.limechain.network.protocol.grandpa.messages.catchup.req.CatchUpReqMessage;
import com.limechain.network.protocol.grandpa.messages.catchup.res.CatchUpResMessage;
import com.limechain.network.protocol.grandpa.messages.commit.CommitMessage;
import com.limechain.network.protocol.grandpa.messages.neighbour.NeighbourMessage;
import com.limechain.network.protocol.grandpa.messages.vote.FullVote;
import com.limechain.network.protocol.grandpa.messages.vote.FullVoteScaleWriter;
import com.limechain.consensus.grandpa.dto.runtime.GrandpaEquivocation;
import com.limechain.network.protocol.grandpa.messages.vote.SignedMessage;
import com.limechain.network.protocol.grandpa.messages.vote.VoteMessage;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.network.protocol.warp.dto.Justification;
import com.limechain.runtime.Runtime;
import com.limechain.runtime.hostapi.dto.Key;
import com.limechain.runtime.hostapi.dto.VerifySignature;
import com.limechain.state.AbstractState;
import com.limechain.state.StateManager;
import com.limechain.storage.block.state.BlockState;
import com.limechain.sync.JustificationVerifier;
import com.limechain.sync.SyncMode;
import com.limechain.sync.state.SyncState;
import com.limechain.utils.Ed25519Utils;
import com.limechain.utils.async.AsyncExecutor;
import com.limechain.utils.scale.ScaleUtils;
import io.emeraldpay.polkaj.types.Hash256;
import io.libp2p.core.PeerId;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Log
@RequiredArgsConstructor
@Component
public class GrandpaMessageHandler {

    private static final BigInteger CATCH_UP_THRESHOLD = BigInteger.TWO;
    private static final int THREAD_POOL_SIZE = 4;

    private final StateManager stateManager;
    private final GrandpaService grandpaService;
    private final PeerMessageCoordinator messageCoordinator;
    private final AsyncExecutor asyncExecutor = AsyncExecutor.withPoolSize(THREAD_POOL_SIZE);


    /**
     * Handles a vote message, extracts signed vote, associates it with the correct round.
     * Depending on the subround type, the vote is added to pre-votes, pre-commits, or marked as the primary proposal.
     *
     * @param voteMessage received vote message.
     */
    public void handleVoteMessage(VoteMessage voteMessage) {
        GrandpaSetState grandpaSetState = stateManager.getGrandpaSetState();
        BigInteger voteMessageSetId = voteMessage.getSetId();

        if (!voteMessageSetId.equals(grandpaSetState.getSetId())) {
            throw new GrandpaGenericException("Vote message has a different setId.");
        }

        // TODO: If we're not an active authority no round will be playing. We should only verify and broadcast.
        BigInteger voteMessageRoundNumber = voteMessage.getRound();

        GrandpaRound currentRound = grandpaSetState.getCurrentGrandpaRound();
        if (currentRound == null) {
            log.fine("handleVoteMessage: No running grandpa round.");
            return;
        }

        BigInteger currentRoundNumber = currentRound.getRoundNumber();

        if (voteMessageRoundNumber.compareTo(currentRoundNumber.subtract(BigInteger.ONE)) < 0) {
            throw new GrandpaGenericException("Vote message is invalid as it refers to a round that is " +
                    "at least two behind the current one.");
        }

        if (voteMessageRoundNumber.compareTo(currentRoundNumber) > 0) {
            throw new GrandpaGenericException("Vote message is invalid as it refers to a future round.");
        }

        SignedMessage signedMessage = voteMessage.getMessage();
        SignedVote receivedSignedVote = new SignedVote(
                new Vote(signedMessage.getBlockHash(), signedMessage.getBlockNumber()),
                signedMessage.getSignature(),
                signedMessage.getAuthorityPublicKey()
        );

        if (!isMessageSignatureValid(voteMessage)) {
            log.warning(String.format(
                    "Invalid vote message signature for round %s, set %s, block hash %s, block number %s",
                    voteMessageRoundNumber, voteMessageSetId, signedMessage.getBlockHash(), signedMessage.getBlockNumber()
            ));
            return;
        }

        GrandpaRound grandpaRound = grandpaSetState.getGrandpaRound(voteMessageRoundNumber);
        SubRound subround = signedMessage.getStage();
        if (isVoteEquivocationDetected(receivedSignedVote, grandpaRound, subround, voteMessageSetId)) {
            log.warning(String.format(
                    "Detected vote equivocation or duplication for round %s, set %s, block hash %s, block number %s",
                    voteMessageRoundNumber, voteMessageSetId, signedMessage.getBlockHash(), signedMessage.getBlockNumber()
            ));
            return;
        }

        Hash256 authorityPublicKey = signedMessage.getAuthorityPublicKey();
        switch (subround) {
            case SubRound.PRE_VOTE -> {
                grandpaRound.getPreVotes().put(authorityPublicKey, receivedSignedVote);
                grandpaRound.update(false, true, false);
            }
            case SubRound.PRE_COMMIT -> {
                grandpaRound.getPreCommits().put(authorityPublicKey, receivedSignedVote);
                grandpaRound.update(false, false, true);
            }
            case SubRound.PRIMARY_PROPOSAL -> grandpaRound.setPrimaryVote(receivedSignedVote.getVote());
            default -> throw new GrandpaGenericException("Unknown subround: " + subround);
        }
    }

    /**
     * Updates the Host's state with information from a commit message.
     * Synchronized to avoid race condition between checking and updating latest block
     * Scheduled runtime updates for synchronized blocks are executed.
     *
     * @param commitMessage received commit message
     * @param peerId        sender of the message
     */
    public synchronized void handleCommitMessage(CommitMessage commitMessage, PeerId peerId) {
        if (!commitMessage.getSetId().equals(stateManager.getGrandpaSetState().getSetId())) {
            log.fine(String.format("handleCommitMessage: Received commit set id, %d, doesn't match local set id, %d",
                    commitMessage.getSetId(), stateManager.getGrandpaSetState().getSetId()));
            return;
        }

        if (commitMessage.getVote().getBlockNumber().compareTo(
                stateManager.getSyncState().getLastFinalizedBlockNumber()) <= 0) {
            log.fine(String.format("Received commit message for finalized block %d from peer %s",
                    commitMessage.getVote().getBlockNumber(), peerId));
            return;
        }

        log.fine("Received commit message from peer " + peerId
                + " for block #" + commitMessage.getVote().getBlockNumber()
                + " with hash " + commitMessage.getVote().getBlockHash()
                + " with setId " + commitMessage.getSetId() + " and round " + commitMessage.getRoundNumber()
                + " with " + commitMessage.getPreCommits().length + " voters");

        boolean verified = JustificationVerifier.verify(Justification.fromCommitMessage(commitMessage));

        if (!verified) {
            log.warning("Could not verify commit from peer: " + peerId);
            return;
        }

        if (!SyncMode.HEAD.equals(AbstractState.getSyncMode())) {
            handleCommitPreHead(commitMessage);
        } else {
            handleCommitAtHead(commitMessage);
        }
    }

    /**
     * Handles commit messages during the process of syncing.<br>
     * If the block from the commit message is present in the block tree it is finalized, otherwise the best block is
     * finalized since it is an ancestor of the one in the message. This is only always true during syncing.
     *
     * @param commitMessage the message received via the grandpa sub-stream.
     */
    private void handleCommitPreHead(CommitMessage commitMessage) {

        BlockState blockState = stateManager.getBlockState();
        SyncState syncState = stateManager.getSyncState();

        if (!blockState.hasHeader(commitMessage.getVote().getBlockHash())) {

            BlockHeader bestBlockHeader;
            try {
                bestBlockHeader = blockState.bestBlockHeader();
                blockState.setFinalizedHash(bestBlockHeader,
                        null,
                        stateManager.getGrandpaSetState().getSetId());
                syncState.finalizeHeader(bestBlockHeader);

                log.info(String.format(
                        "handleCommitPreHead: Commit block #%d not in tree. Finalized best block with #%d and hash %s",
                        commitMessage.getVote().getBlockNumber(),
                        bestBlockHeader.getBlockNumber(),
                        bestBlockHeader.getHash()));
            } catch (HeaderNotFoundException e) {
                log.warning("handleCommitPreHead: No block header found for best block.");
                return;
            } catch (LowerThanRootException e) {
                log.warning("handleCommitPreHead: Lower than root exception for best block.");
            }

            blockState.getJustifications()
                    .put(commitMessage.getVote().getBlockHash(), Justification.fromCommitMessage(commitMessage));

            return;
        }

        syncState.finalizedCommitMessage(commitMessage);
        log.info(String.format(
                "handleCommitPreHead: Finalized block with #%d and hash %s",
                commitMessage.getVote().getBlockNumber(), commitMessage.getVote().getBlockHash()));
    }

    /**
     * Handles commit messages when the node is at the head of the chain.<br>
     *
     * @param commitMessage the message
     */
    private void handleCommitAtHead(CommitMessage commitMessage) {

        if (AbstractState.isActiveAuthority()) {
            // TODO: When we receive a grandpa justification we should apply it and create an appropriate round.
            return;
        }

        stateManager.getSyncState().finalizedCommitMessage(commitMessage);
        log.fine(String.format(
                "handleCommitAtHead: Finalized block with #%d and hash %s",
                commitMessage.getVote().getBlockNumber(), commitMessage.getVote().getBlockHash()));
    }

    /**
     * Initiates and sends a catch-up request to a specific peer.
     *
     * @param neighbourMessage received neighbour message
     * @param peerId           peer to send the catch-up message to
     */
    public void initiateAndSendCatchUpRequest(NeighbourMessage neighbourMessage, PeerId peerId) {
        GrandpaSetState grandpaSetState = stateManager.getGrandpaSetState();
        // If peer has the same voter set id
        if (neighbourMessage.getSetId().equals(grandpaSetState.getSetId())) {

            // Check if needed to catch-up peer
            if (neighbourMessage.getRoundNumber().compareTo(
                    grandpaSetState.fetchLatestRoundNumber().add(CATCH_UP_THRESHOLD)) >= 0) {
                log.log(Level.FINE, "Neighbor message indicates that the round of Peer " + peerId + " is ahead.");

                CatchUpReqMessage catchUpReqMessage = CatchUpReqMessage.builder()
                        .round(neighbourMessage.getRoundNumber())
                        .setId(neighbourMessage.getSetId()).build();

                messageCoordinator.sendCatchUpRequestToPeer(peerId, catchUpReqMessage);
            }
        }
    }

    /**
     * Handles a catch-up request from a peer, initiating and sending corresponding catch-up response.
     *
     * @param peerId            peer requesting catch-up message
     * @param catchUpReqMessage received catch-up request message
     * @param peerIds           set of connected peer ids
     */
    public void initiateAndSendCatchUpResponse(PeerId peerId,
                                               CatchUpReqMessage catchUpReqMessage,
                                               Supplier<Set<PeerId>> peerIds) {

        GrandpaSetState grandpaSetState = stateManager.getGrandpaSetState();
        if (!peerIds.get().contains(peerId)) {
            throw new GrandpaGenericException("Requesting catching up from a non-peer.");
        }

        if (!catchUpReqMessage.getSetId().equals(grandpaSetState.getSetId())) {
            throw new GrandpaGenericException("Catch up message has a different setId.");
        }

        if (catchUpReqMessage.getRound().compareTo(grandpaSetState.fetchLatestRoundNumber()) > 0) {
            throw new GrandpaGenericException("Catching up on a round in the future.");
        }

        GrandpaRound grandpaRound = grandpaSetState.getGrandpaRound(catchUpReqMessage.getRound());

        SignedVote[] preVotes = getPreVoteJustification(grandpaRound);
        SignedVote[] preCommits = getPreCommitJustification(grandpaRound);

        BlockHeader finalizedBlockHeader = grandpaRound.getFinalizedBlock();

        CatchUpResMessage catchUpResMessage = CatchUpResMessage.builder()
                .roundNumber(grandpaRound.getRoundNumber())
                .setId(grandpaSetState.getSetId())
                .preCommits(preCommits)
                .preVotes(preVotes)
                .blockHash(finalizedBlockHeader.getHash())
                .blockNumber(finalizedBlockHeader.getBlockNumber())
                .build();

        messageCoordinator.sendCatchUpResponseToPeer(peerId, catchUpResMessage);
    }

    /**
     * Handles a catch-up response from a peer, validating pre-votes and pre-commits.
     * Initializes a new round, determines the Grandpa Ghost and finalization estimate,
     * checks round compatibility, and executes the Play-Grandpa-Round if valid.
     *
     * @param peerId            peer responding with catch-up message
     * @param catchUpResMessage received catch-up response message
     * @param peerIds           set of connected peer ids
     */
    public void handleCatchUpResponse(PeerId peerId,
                                      CatchUpResMessage catchUpResMessage,
                                      Supplier<Set<PeerId>> peerIds) {

        GrandpaSetState grandpaSetState = stateManager.getGrandpaSetState();

        if (!peerIds.get().contains(peerId)) {
            throw new GrandpaGenericException("handleCatchUpResponse: Response from a non-peer.");
        }

        if (!catchUpResMessage.getSetId().equals(grandpaSetState.getSetId())) {
            throw new GrandpaGenericException("handleCatchUpResponse: Response has a different setId.");
        }

        GrandpaRound round = grandpaSetState.getCurrentGrandpaRound();
        if (catchUpResMessage.getRoundNumber().compareTo(round.getRoundNumber()) <= 0) {
            throw new GrandpaGenericException("handleCatchUpResponse: Catching up into a round in the past.");
        }

        BlockState blockState = stateManager.getBlockState();
        if (blockState.hasHeader(catchUpResMessage.getBlockHash())) {
            throw new GrandpaGenericException("handleCatchUpResponse: Response for a block not in tree received.");
        }

        verifyCatchupMessage(catchUpResMessage);

        boolean isNewerThanCurrent = catchUpResMessage.getBlockNumber().compareTo(round.getRoundNumber()) > 0;

        if (isNewerThanCurrent) {

            RoundState roundState = RoundState.builder()
                    .roundNumber(catchUpResMessage.getRoundNumber())
                    .lastFinalizedBlock(round.getLastFinalizedBlock())
                    .finalizedBlock(BlockHeader.fromHash(catchUpResMessage.getBlockHash()))
                    .build();

            Optional<GrandpaAuthoritySet> authSetOpt = grandpaService.getAuthoritiesForBlock(
                    roundState.getFinalizedBlock().getBlockNumber());

            if (authSetOpt.isEmpty()) {
                log.warning(String.format("createNextRound: No authority set found for block %d",
                        roundState.getFinalizedBlock().getBlockNumber()));
                return;
            }

            round = grandpaService.createInitialRound(roundState);
            round.complete();
        }


        setPreVotesAndPvEquivocations(round, catchUpResMessage.getPreVotes());
        round.update(false, true, false);

        setPreCommitsAndPcEquivocations(round, catchUpResMessage.getPreCommits());
        round.update(false, false, true);

        if (isNewerThanCurrent) {
            grandpaSetState.getCurrentGrandpaRound().complete();
            grandpaSetState.addNewGrandpaRound(round);
        }

        grandpaService.tryStartFromPreviousRound(round);
    }

    private void verifyCatchupMessage(CatchUpResMessage catchUpResMessage) {
        CompletableFuture<Boolean> verifiedPreVotesFuture = asyncExecutor.executeAsync(() ->
                JustificationVerifier.verify(Justification.fromCatchUpResPreVotes(catchUpResMessage)));

        CompletableFuture<Boolean> verifiedPreCommitsFuture = asyncExecutor.executeAsync(() ->
                JustificationVerifier.verify(Justification.fromCatchUpResPreCommits(catchUpResMessage)));

        // Combines verified of preVotes and preCommits - it is true only if both and verified.
        CompletableFuture<Boolean> verifiedFuture = verifiedPreVotesFuture.thenCombine(verifiedPreCommitsFuture,
                (preVotes, preCommits) -> preVotes && preCommits);

        boolean verified = verifiedFuture.join();
        if (!verified) {
            throw new JustificationVerificationException("Justification could not be verified.");
        }
    }

    private boolean isMessageSignatureValid(VoteMessage voteMessage) {
        SignedMessage signedMessage = voteMessage.getMessage();

        FullVote fullVote = new FullVote(
                signedMessage.getStage(),
                new Vote(signedMessage.getBlockHash(), signedMessage.getBlockNumber()),
                voteMessage.getRound(),
                voteMessage.getSetId()
        );

        byte[] encodedFullVote = ScaleUtils.Encode.encode(FullVoteScaleWriter.getInstance(), fullVote);

        VerifySignature verifySignature = new VerifySignature(
                signedMessage.getSignature().getBytes(),
                encodedFullVote,
                signedMessage.getAuthorityPublicKey().getBytes(),
                Key.ED25519);

        return Ed25519Utils.verifySignature(verifySignature);
    }

    private boolean isVoteEquivocationDetected(SignedVote receivedSignedVote,
                                               GrandpaRound round,
                                               SubRound subRound,
                                               BigInteger voteMessageSetId) {

        if (!EnumSet.of(SubRound.PRE_VOTE, SubRound.PRE_COMMIT).contains(subRound)) {
            return false;
        }

        boolean isPreCommit = (subRound == SubRound.PRE_COMMIT);
        Map<Hash256, SignedVote> votes = isPreCommit ? round.getPreCommits() : round.getPreVotes();
        Hash256 authorityPublicKey = receivedSignedVote.getAuthorityPublicKey();

        SignedVote foundSignedVote = votes.get(authorityPublicKey);
        if (foundSignedVote == null) {
            return false;
        }

        Hash256 foundVoteBlockHash = foundSignedVote.getVote().getBlockHash();
        Hash256 receivedVoteBlockHash = receivedSignedVote.getVote().getBlockHash();

        if (foundVoteBlockHash.equals(receivedVoteBlockHash)) {
            log.warning(String.format(
                    "Voter : %s sent duplicated vote with block hash: %s",
                    authorityPublicKey, receivedVoteBlockHash));
            return true;
        }

        reportVoteEquivocation(receivedSignedVote, foundSignedVote, voteMessageSetId, round, isPreCommit);
        return true;
    }

    private void reportVoteEquivocation(SignedVote receivedSignedVote,
                                        SignedVote foundSignedVote,
                                        BigInteger voteMessageSetId,
                                        GrandpaRound round,
                                        boolean isPreCommit) {

        Hash256 authorityPublicKey = receivedSignedVote.getAuthorityPublicKey();
        Map<Hash256, List<SignedVote>> equivocations = isPreCommit ?
                round.getPcEquivocations() : round.getPvEquivocations();

        BlockState blockState = stateManager.getBlockState();
        Runtime runtime = blockState.getRuntime(blockState.getHighestFinalizedHash());
        equivocations.computeIfAbsent(authorityPublicKey, _ -> new ArrayList<>()).add(receivedSignedVote);
        GrandpaEquivocation grandpaEquivocation =
                GrandpaEquivocation.builder()
                        .setId(voteMessageSetId)
                        .equivocationStage((byte) (isPreCommit ? 1 : 0))
                        .roundNumber(round.getRoundNumber())
                        .firstBlockNumber(foundSignedVote.getVote().getBlockNumber())
                        .firstBlockHash(foundSignedVote.getVote().getBlockHash())
                        .firstSignature(foundSignedVote.getSignature())
                        .secondBlockNumber(receivedSignedVote.getVote().getBlockNumber())
                        .secondBlockHash(receivedSignedVote.getVote().getBlockHash())
                        .secondSignature(receivedSignedVote.getSignature())
                        .build();

        runtime.generateGrandpaKeyOwnershipProof(voteMessageSetId, authorityPublicKey.getBytes())
                .ifPresentOrElse(
                        key -> runtime.submitReportGrandpaEquivocationUnsignedExtrinsic(
                                grandpaEquivocation, key.getProof()
                        ),
                        () -> log.warning(String.format(
                                "Failure to report Grandpa vote equivocation for authority: %s.", authorityPublicKey
                        ))
                );
    }

    private void setPreVotesAndPvEquivocations(GrandpaRound grandpaRound, SignedVote[] votes) {
        setVotesAndEquivocations(grandpaRound, votes, GrandpaRound::setPreVotes, GrandpaRound::setPvEquivocations);
    }

    private void setPreCommitsAndPcEquivocations(GrandpaRound grandpaRound, SignedVote[] votes) {
        setVotesAndEquivocations(grandpaRound, votes, GrandpaRound::setPreCommits, GrandpaRound::setPcEquivocations);
    }

    private void setVotesAndEquivocations(GrandpaRound grandpaRound,
                                          SignedVote[] votes,
                                          BiConsumer<GrandpaRound, Map<Hash256, SignedVote>> setUniqueVotes,
                                          BiConsumer<GrandpaRound, Map<Hash256, List<SignedVote>>> setEquivocations) {

        // Group votes by AuthorityPublicKey
        Map<Hash256, List<SignedVote>> voteCount = Arrays.stream(votes)
                .collect(Collectors.groupingBy(SignedVote::getAuthorityPublicKey));

        Map<Hash256, SignedVote> uniqueVotes = new ConcurrentHashMap<>();
        Map<Hash256, List<SignedVote>> equivocations = new ConcurrentHashMap<>();

        for (Map.Entry<Hash256, List<SignedVote>> entry : voteCount.entrySet()) {
            List<SignedVote> voteList = entry.getValue();
            Hash256 authorityKey = entry.getKey();

            if (voteList.size() == 1) {
                uniqueVotes.put(authorityKey, voteList.getFirst());
            } else {
                equivocations.put(authorityKey, voteList);
            }
        }

        setUniqueVotes.accept(grandpaRound, uniqueVotes);
        setEquivocations.accept(grandpaRound, equivocations);
    }

    private SignedVote[] getPreVoteJustification(GrandpaRound requestedRound) {
        BlockHeader estimate = requestedRound.getBestFinalCandidate();
        BlockState blockState = stateManager.getBlockState();
        Hash256 estimateHash = estimate.getHash();

        Predicate<SignedVote> isDescendant = vote ->
                blockState.isDescendantOf(estimateHash, vote.getVote().getBlockHash());

        return Stream.concat(
                        requestedRound.getPreVotes().values().stream(),
                        requestedRound.getPvEquivocations().values().stream().flatMap(List::stream)
                )
                .filter(isDescendant)
                .toArray(SignedVote[]::new);
    }

    private SignedVote[] getPreCommitJustification(GrandpaRound requestedRound) {
        BlockHeader finalizedBlock = requestedRound.getFinalizedBlock();
        BigInteger totalWeight = BigInteger.ZERO;
        BigInteger threshold = requestedRound.getThreshold();

        List<SignedVote> result = new ArrayList<>();

        Stream<SignedVote> allPreCommits = Stream.concat(
                requestedRound.getPcEquivocations().values().stream().flatMap(List::stream),
                requestedRound.getPreCommits().values().stream()
        );

        for (SignedVote vote : allPreCommits.toList()) {
            if (totalWeight.compareTo(threshold) >= 0) break;
            totalWeight = increaseWeightAndAddVote(vote, finalizedBlock, totalWeight, result);
        }

        return result.toArray(SignedVote[]::new);
    }

    private BigInteger increaseWeightAndAddVote(SignedVote vote,
                                                BlockHeader finalizedBlock,
                                                BigInteger totalWeight,
                                                List<SignedVote> result) {

        BlockState blockState = stateManager.getBlockState();
        if (finalizedBlock.getBlockNumber().compareTo(vote.getVote().getBlockNumber()) <= 0 &&
                blockState.isDescendantOf(finalizedBlock.getHash(), vote.getVote().getBlockHash())) {

            BigInteger voterWeight = stateManager.getGrandpaSetState().getAuthorityWeight(
                    vote.getAuthorityPublicKey()).orElse(BigInteger.ZERO);

            if (voterWeight.compareTo(BigInteger.ZERO) > 0) {
                totalWeight = totalWeight.add(voterWeight);
                result.add(vote);
            }
        }
        return totalWeight;
    }
}
