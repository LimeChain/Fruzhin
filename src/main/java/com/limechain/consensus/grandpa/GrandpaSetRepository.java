package com.limechain.consensus.grandpa;

import com.limechain.consensus.dto.Authority;
import com.limechain.consensus.grandpa.dto.GrandpaAuthoritySet;
import com.limechain.consensus.grandpa.dto.SignedVote;
import com.limechain.consensus.grandpa.dto.Vote;
import com.limechain.consensus.grandpa.round.GrandpaRound;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.storage.DBConstants;
import com.limechain.storage.KVRepository;
import com.limechain.storage.StateUtil;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GrandpaSetRepository {

    private final KVRepository<String, Object> repository;

    public void saveGrandpaAuthorities(GrandpaAuthoritySet authoritySet) {
        repository.save(StateUtil.generateAuthorityKey(
                DBConstants.GRANDPA_AUTHORITY_SET, authoritySet.getSetId()), authoritySet.getAuthorities()
        );
    }

    public Authority[] fetchGrandpaAuthorities(GrandpaAuthoritySet authoritySet) {
        String key = StateUtil.generateAuthorityKey(
                DBConstants.GRANDPA_AUTHORITY_SET, authoritySet.getSetId());
        List<Authority> authorities = repository.find(key, new ArrayList<>());
        return authorities.toArray(new Authority[0]);
    }

    public void saveAuthoritySetId(GrandpaAuthoritySet authoritySet) {
        repository.save(DBConstants.GRANDPA_SET_ID, authoritySet.getSetId());
    }

    public BigInteger fetchAuthoritiesSetId() {
        return repository.find(DBConstants.GRANDPA_SET_ID, BigInteger.ZERO);
    }

    public void saveLatestRoundNumber(BigInteger roundNumber) {
        repository.save(DBConstants.LATEST_ROUND, roundNumber);
    }

    public BigInteger fetchLatestRoundNumber() {
        return repository.find(DBConstants.LATEST_ROUND, BigInteger.ZERO);
    }

    public void savePreVotes(GrandpaAuthoritySet authoritySet, GrandpaRound round) {
        Map<Hash256, SignedVote> preVotes = round.getPreVotes();
        repository.save(StateUtil.generatePreVotesKey(
                DBConstants.GRANDPA_PREVOTES, round.getRoundNumber(), authoritySet.getSetId()), preVotes
        );
    }

    public Map<Hash256, SignedVote> fetchPreVotes(GrandpaAuthoritySet authoritySet, BigInteger roundNumber) {
        return repository.find(StateUtil.generatePreVotesKey(
                DBConstants.GRANDPA_PREVOTES, roundNumber, authoritySet.getSetId()), Collections.emptyMap()
        );
    }

    public void savePreCommits(GrandpaAuthoritySet authoritySet, GrandpaRound round) {
        Map<Hash256, SignedVote> preCommits = round.getPreCommits();
        repository.save(StateUtil.generatePreCommitsKey(
                DBConstants.GRANDPA_PRECOMMITS, round.getRoundNumber(), authoritySet.getSetId()), preCommits
        );
    }

    public Map<Hash256, SignedVote> fetchPreCommits(GrandpaAuthoritySet authoritySet, BigInteger roundNumber) {
        return repository.find(StateUtil.generatePreCommitsKey(
                DBConstants.GRANDPA_PRECOMMITS, roundNumber, authoritySet.getSetId()), Collections.emptyMap()
        );
    }

    public void savePreVoteEquivocations(GrandpaAuthoritySet authoritySet, GrandpaRound round) {
        Map<Hash256, List<SignedVote>> pvEquivocations = round.getPvEquivocations();
        repository.save(StateUtil.generatePreVotesKey(
                DBConstants.GRANDPA_PREVOTE_EQUIVOCATIONS, round.getRoundNumber(), authoritySet.getSetId()), pvEquivocations
        );
    }

    public Map<Hash256, List<SignedVote>> fetchPreVoteEquivocations(GrandpaAuthoritySet authoritySet, BigInteger roundNumber) {
        return repository.find(StateUtil.generatePreVotesKey(
                DBConstants.GRANDPA_PREVOTE_EQUIVOCATIONS, roundNumber, authoritySet.getSetId()), Collections.emptyMap()
        );
    }

    public void savePreCommitEquivocations(GrandpaAuthoritySet authoritySet, GrandpaRound round) {
        Map<Hash256, List<SignedVote>> pcEquivocations = round.getPcEquivocations();
        repository.save(StateUtil.generatePreCommitsKey(
                DBConstants.GRANDPA_PRECOMMIT_EQUIVOCATIONS, round.getRoundNumber(), authoritySet.getSetId()), pcEquivocations
        );
    }

    public Map<Hash256, List<SignedVote>> fetchPreCommitEquivocations(GrandpaAuthoritySet authoritySet, BigInteger roundNumber) {
        return repository.find(StateUtil.generatePreCommitsKey(
                DBConstants.GRANDPA_PRECOMMIT_EQUIVOCATIONS, roundNumber, authoritySet.getSetId()), Collections.emptyMap()
        );
    }

    public void savePrimaryVote(GrandpaAuthoritySet authoritySet, GrandpaRound round) {
        if (round.getPrimaryVote() != null) {
            repository.save(StateUtil.generatePrimaryVoteKey(
                    DBConstants.GRANDPA_PRIMARY_VOTE, round.getRoundNumber(), authoritySet.getSetId()), round.getPrimaryVote()
            );
        }
    }

    public Vote fetchPrimaryVote(GrandpaAuthoritySet authoritySet, BigInteger roundNumber) {
        return repository.find(StateUtil.generatePrimaryVoteKey(
                DBConstants.GRANDPA_PRIMARY_VOTE, roundNumber, authoritySet.getSetId()), null
        );
    }

    public void saveIsPrimaryVoter(GrandpaAuthoritySet authoritySet, GrandpaRound round) {
        repository.save(StateUtil.generateIsPrimaryVoterKey(
                DBConstants.GRANDPA_IS_PRIMARY_VOTER, round.getRoundNumber(), authoritySet.getSetId()), round.isPrimaryVoter()
        );
    }

    public Boolean fetchIsPrimaryVoter(GrandpaAuthoritySet authoritySet, BigInteger roundNumber) {
        return repository.find(StateUtil.generateIsPrimaryVoterKey(
                DBConstants.GRANDPA_IS_PRIMARY_VOTER, roundNumber, authoritySet.getSetId()), null
        );
    }

    public void saveLastFinalizedBlock(GrandpaAuthoritySet authoritySet, GrandpaRound round) {
        if (round.getLastFinalizedBlock() != null) {
            repository.save(StateUtil.generateLastFinalizedBlockKey(
                            DBConstants.LAST_FINALIZED_BLOCK, round.getRoundNumber(), authoritySet.getSetId()),
                    round.getLastFinalizedBlock()
            );
        }
    }

    public BlockHeader fetchLastFinalizedBlock(GrandpaAuthoritySet authoritySet, BigInteger roundNumber) {
        return repository.find(StateUtil.generateLastFinalizedBlockKey(
                DBConstants.LAST_FINALIZED_BLOCK, roundNumber, authoritySet.getSetId()), null
        );
    }
}
