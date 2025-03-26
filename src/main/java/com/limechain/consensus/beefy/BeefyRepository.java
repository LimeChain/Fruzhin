package com.limechain.consensus.beefy;

import com.limechain.consensus.beefy.dto.BeefyAuthoritySet;
import com.limechain.consensus.beefy.dto.BeefySession;
import com.limechain.network.protocol.beefy.messages.justification.SignedCommitment;
import com.limechain.storage.DBConstants;
import com.limechain.storage.KVRepository;
import com.limechain.storage.StateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BeefyRepository {

    private final KVRepository<String, Object> repository;

    public BigInteger fetchAuthoritiesSetId() {
        return repository.find(DBConstants.BEEFY_SET_ID, BigInteger.ZERO);
    }

    public void saveAuthoritiesSetId(BeefyAuthoritySet authoritySet) {
        repository.save(DBConstants.BEEFY_SET_ID, authoritySet.getSetId());
    }

    public List<byte[]> fetchBeefyAuthorities(BigInteger setId) {
        return repository.find(
                StateUtil.generateAuthorityKey(DBConstants.BEEFY_AUTHORITY_SET, setId),
                Collections.emptyList()
        );
    }

    public void saveBeefyAuthorities(BeefyAuthoritySet authoritySet) {
        repository.save(
                StateUtil.generateAuthorityKey(DBConstants.BEEFY_AUTHORITY_SET, authoritySet.getSetId()),
                authoritySet.getPublicKeys()
        );
    }

    public BigInteger fetchBeefyFinalized() {
        return repository.find(DBConstants.BEEFY_FINALIZED, BigInteger.ZERO);
    }

    public void saveBeefyFinalized(BigInteger beefyFinalized) {
        repository.save(DBConstants.BEEFY_FINALIZED, beefyFinalized);
    }

    public BigInteger fetchGrandpaFinalized() {
        return repository.find(DBConstants.BEEFY_GRANDPA_FINALIZED, BigInteger.ZERO);
    }

    public void saveGrandpaFinalized(BigInteger grandpaFinalized) {
        repository.save(DBConstants.BEEFY_GRANDPA_FINALIZED, grandpaFinalized);
    }

    public BigInteger fetchBeefyGenesis() {
        return repository.find(DBConstants.BEEFY_GENESIS, null);
    }

    public void saveBeefyGenesis(BigInteger beefyGenesis) {
        repository.save(DBConstants.BEEFY_GENESIS, beefyGenesis);
    }

    public BigInteger fetchRoundNumber() {
        return repository.find(DBConstants.BEEFY_ROUND, BigInteger.ZERO);
    }

    public void saveRoundNumber(BigInteger roundNumber) {
        repository.save(DBConstants.BEEFY_ROUND, roundNumber);
    }

    public BigInteger fetchLastVoted() {
        return repository.find(DBConstants.BEEFY_LAST_VOTED, BigInteger.ZERO);
    }

    public void saveLastVoted(BigInteger lastVoted) {
        repository.save(DBConstants.BEEFY_LAST_VOTED, lastVoted);
    }

    public Deque<BeefySession> fetchSessions() {
        return repository.find(DBConstants.BEEFY_SESSIONS, new ArrayDeque<>());
    }

    public void saveSessions(Deque<BeefySession> sessions) {
        repository.save(DBConstants.BEEFY_SESSIONS, sessions);
    }

    public SignedCommitment fetchJustification(BigInteger blockNumber) {
        return repository.find(
                StateUtil.generateBeefyJustificationKey(DBConstants.BEEFY_JUSTIFICATION, blockNumber),
                null
        );
    }

    public void saveJustification(BigInteger blockNumber, SignedCommitment justification) {
        repository.save(
                StateUtil.generateBeefyJustificationKey(DBConstants.BEEFY_JUSTIFICATION, blockNumber),
                justification
        );
    }
}
