package com.limechain.consensus.beefy.dto;

import com.limechain.exception.beefy.BeefyGenericException;
import com.limechain.network.protocol.beefy.messages.justification.SignedCommitment;
import com.limechain.network.protocol.beefy.messages.vote.BeefyVoteMessage;
import io.emeraldpay.polkaj.types.Hash264;
import jakarta.annotation.Nullable;
import lombok.Data;
import lombok.Setter;
import lombok.extern.java.Log;
import org.javatuples.Pair;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Data
@Log
public class BeefySession implements Serializable {

    private static final BigInteger THRESHOLD_DENOMINATOR = BigInteger.valueOf(3);

    private final BeefyAuthoritySet authoritySet;

    private Map<Commitment, BeefyRound> rounds = new ConcurrentHashMap<>();

    private Map<Pair<Hash264, BigInteger>, BeefyVoteMessage> previousVotes = new ConcurrentHashMap<>();

    private final BigInteger mandatoryBlock;

    @Setter
    private boolean isMandatoryBlockFinalized;

    @Setter
    @Nullable
    private BigInteger highestFinalized = BigInteger.ZERO;

    @Nullable
    private final Pair<byte[], byte[]> beefyKeyPair;


    public VoteImportResult addVote(BeefyVoteMessage voteMessage) {
        Commitment commitment = voteMessage.getCommitment();
        byte[] authorityId = voteMessage.getAuthorityId();
        BigInteger blockNumber = commitment.getBlockNumber();

        if (blockNumber.compareTo(mandatoryBlock) < 0 ||
                blockNumber.compareTo(highestFinalized) <= 0) {
            log.fine(String.format("addVote: received vote for old stale round {%s}, ignoring",
                    blockNumber));
            return new VoteImportResult.Invalid();
        } else if (!Objects.equals(commitment.getAuthoritySetId(), authoritySet.getSetId())) {
            log.fine(String.format("addVote: expected set_id {%s}, ignoring vote {%s}",
                    authoritySet.getSetId(), voteMessage));
            return new VoteImportResult.Invalid();
        } else if (!authoritySet.getPublicKeys().contains(authorityId)) {
            log.fine(String.format("addVote: received vote {%s} from validator that is not in the" +
                    " validator set, ignoring", voteMessage));
            return new VoteImportResult.Invalid();
        }

        Hash264 authorityIdHash = new Hash264(authorityId);
        Pair<Hash264, BigInteger> voteKey = new Pair<>(authorityIdHash, blockNumber);

        if (previousVotes.containsKey(voteKey)) {

            BeefyVoteMessage previousVote = previousVotes.get(voteKey);
            if (!previousVote.getCommitment().getPayload().equals(commitment.getPayload())) {

                log.info(String.format(
                        "addVote: Detected equivocated vote: 1st: {%s}, 2nd: {%s}", previousVote, voteMessage)
                );
                return new VoteImportResult.DoubleVoting(new DoubleVotingProof(previousVote, voteMessage));
            }
        } else {
            previousVotes.put(voteKey, voteMessage);
        }

        BeefyRound round = rounds.computeIfAbsent(commitment, _ -> new BeefyRound());
        if (round.addVote(authorityIdHash, voteMessage) &&
                round.isDone(getThreshold())) {

            rounds.remove(commitment);
            log.fine(String.format("addVote: Beefy round #{%d} concluded", blockNumber));
            return new VoteImportResult.RoundConcluded(createSignedCommitment(round, commitment));
        }

        return new VoteImportResult.Ok();
    }

    public SignedCommitment createSignedCommitment(BeefyRound round, Commitment commitment) {

        Map<Hash264, BeefyVoteMessage> signedVotes = round.getSignedVotes();

        List<Optional<byte[]>> signatures = authoritySet.getPublicKeys().stream()
                .map(key -> Optional.ofNullable(signedVotes.get(new Hash264(key)))
                        .map(BeefyVoteMessage::getSignature))
                .toList();

        return new SignedCommitment(commitment, signatures);
    }

    /**
     * The threshold is determined as the numOfValidators - (numOfValidators - 1) / 3
     *
     * @return minimum required validators for finality.
     */
    public BigInteger getThreshold() {

        if (authoritySet == null) {
            throw new BeefyGenericException("getThreshold: No authoritySet in BeefySession.");
        }

        var validatorSize = authoritySet.getPublicKeys().size();

        if (validatorSize == 0) {
            log.warning("getThreshold: Validator set is empty.");
            return BigInteger.ZERO;
        }

        var numOfValidators = BigInteger.valueOf(validatorSize);
        var faulty = (numOfValidators.subtract(BigInteger.ONE)).divide(THRESHOLD_DENOMINATOR);

        return numOfValidators.subtract(faulty);
    }

    public void update(BigInteger blockNumber) {
        // remove rounds <= block number(round number)
        rounds.keySet().removeIf(commitment -> commitment.getBlockNumber().compareTo(blockNumber) <= 0);

        highestFinalized = (highestFinalized == null)
                ? blockNumber
                : highestFinalized.max(blockNumber);

        if (blockNumber.equals(mandatoryBlock)) {
            isMandatoryBlockFinalized = true;
            log.fine(String.format("finalizeJustification: Finalize mandatory round: %d.", blockNumber));
        } else {
            log.fine(String.format("finalizeJustification: Finalize non-mandatory round: %d.", blockNumber));
        }
    }
}
