package com.limechain.consensus.grandpa;

import com.limechain.consensus.dto.Authority;
import com.limechain.consensus.grandpa.dto.GrandpaAuthoritySet;
import com.limechain.consensus.grandpa.dto.SignedVote;
import com.limechain.consensus.grandpa.dto.Vote;
import com.limechain.consensus.grandpa.round.GrandpaRound;
import com.limechain.storage.DBConstants;
import com.limechain.storage.KVRepository;
import com.limechain.storage.StateUtil;
import io.emeraldpay.polkaj.types.Hash256;
import io.libp2p.core.crypto.PubKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Collections;
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
        return repository.find(StateUtil.generateAuthorityKey(
                DBConstants.GRANDPA_AUTHORITY_SET, authoritySet.getSetId()), new Authority[0]
        );
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

    public Map<PubKey, Vote> fetchPreVotes(GrandpaAuthoritySet authoritySet, BigInteger roundNumber) {
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

    public Map<PubKey, Vote> fetchPreCommits(GrandpaAuthoritySet authoritySet, BigInteger roundNumber) {
        return repository.find(StateUtil.generatePreCommitsKey(
                DBConstants.GRANDPA_PRECOMMITS, roundNumber, authoritySet.getSetId()), Collections.emptyMap()
        );
    }
}
