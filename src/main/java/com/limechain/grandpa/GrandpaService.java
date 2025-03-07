package com.limechain.grandpa;

import com.limechain.exception.grandpa.GrandpaGenericException;
import com.limechain.grandpa.round.GrandpaRound;
import com.limechain.grandpa.state.AuthoritySet;
import com.limechain.grandpa.state.GrandpaSetState;
import com.limechain.grandpa.state.RoundState;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.network.protocol.warp.dto.Justification;
import com.limechain.state.AbstractState;
import com.limechain.state.StateManager;
import com.limechain.storage.block.state.BlockState;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@Log
@Component
@RequiredArgsConstructor
public class GrandpaService {

    private final StateManager stateManager;

    public void start() {

        try {
            tryStartFromLastFinalizedBlock();

            log.info(String.format("Grandpa service started with round #%d",
                    stateManager.getGrandpaSetState().getCurrentGrandpaRound().getRoundNumber()));
        } catch (RuntimeException e) {
            log.warning(String.format("start: There was an error when starting grandpa: %s", e.getMessage()));
        }
    }

    public void tryStartFromPreviousRound(GrandpaRound prevRound) {

        GrandpaSetState state = stateManager.getGrandpaSetState();
        GrandpaRound currentRound = state.getCurrentGrandpaRound();

        if (currentRound != prevRound) {
            log.fine("tryStartNextRound: We should only start a next round from the current one.");
        }

        try {
            GrandpaRound nextRound = createNextRound(prevRound);
            state.addNewGrandpaRound(nextRound);
            playCurrentRound();
        } catch (GrandpaGenericException e) {
            log.warning(String.format("tryStartNextRound: Cannot start next round: %s", e.getMessage()));
        }
    }

    public GrandpaRound createInitialRound(RoundState roundState) {

        GrandpaSetState grandpaSetState = stateManager.getGrandpaSetState();
        AuthoritySet authoritySet = roundState.getAuthoritySet();

        return new GrandpaRound(roundState,
                grandpaSetState.getThreshold(authoritySet.getAuthorities()),
                isPrimary(roundState.getRoundNumber(), authoritySet));
    }

    public Optional<AuthoritySet> getAuthoritiesForBlock(BigInteger blockNumber) {
        AuthoritySet authorities = null;

        GrandpaSetState grandpaSetState = stateManager.getGrandpaSetState();

        for (Map.Entry<BigInteger, AuthoritySet> entry : grandpaSetState.getPastSetChanges().entrySet()) {

            if (entry.getKey().compareTo(blockNumber) <= 0) {
                authorities = entry.getValue();
            } else {
                break;
            }
        }

        return Optional.ofNullable(authorities);
    }

    private void tryStartFromLastFinalizedBlock() {

        BlockState blockState = stateManager.getBlockState();
        GrandpaSetState grandpaSetState = stateManager.getGrandpaSetState();

        BlockHeader lastFinalized = blockState.getHighestFinalizedHeader();
        Optional<AuthoritySet> authSetOpt = getAuthoritiesForBlock(lastFinalized.getBlockNumber());

        if (authSetOpt.isEmpty()) {
            log.fine(String.format("createRoundFromFinalizedBlock: No authority set found for block %d",
                    lastFinalized.getBlockNumber()));
            throw new GrandpaGenericException("No authority set found for block " + lastFinalized.getBlockNumber());
        }

        RoundState.RoundStateBuilder stateBuilder = RoundState.builder()
                .roundNumber(BigInteger.ONE)
                .lastFinalizedBlock(lastFinalized)
                .authoritySet(authSetOpt.get());

        if (lastFinalized.getBlockNumber().compareTo(BigInteger.ZERO) > 0) {
            Optional<Justification> justificationOpt = blockState.getJustification(lastFinalized.getHash());
            justificationOpt.ifPresent(justification -> {
                if (!isFirstBlockOfSet(lastFinalized.getBlockNumber())) {
                    stateBuilder.roundNumber(justification.getRoundNumber().add(BigInteger.ONE));
                }
            });
        }

        GrandpaRound currentRound = createInitialRound(stateBuilder.build());
        grandpaSetState.addNewGrandpaRound(currentRound);
        playCurrentRound();
    }

    private void playCurrentRound() {
        if (AbstractState.isActiveAuthority()) {
            stateManager.getGrandpaSetState().getCurrentGrandpaRound().play();
        }
    }

    private GrandpaRound createNextRound(GrandpaRound round) {

        GrandpaSetState grandpaSetState = stateManager.getGrandpaSetState();

        BlockHeader lastFinalized = round.getFinalizedBlock() == null
                ? round.getLastFinalizedBlock()
                : round.getFinalizedBlock();

        Optional<AuthoritySet> authSetOpt = getAuthoritiesForBlock(lastFinalized.getBlockNumber());
        if (authSetOpt.isEmpty()) {
            log.fine(String.format("createNextRound: No authority set found for block %d",
                    lastFinalized.getBlockNumber()));
            throw new GrandpaGenericException("No authority set found for block " + lastFinalized.getBlockNumber());
        }

        AuthoritySet authSetAtBlock = authSetOpt.get();
        BigInteger newRoundNumber = round.getSetId().equals(authSetAtBlock.getSetId())
                ? round.getRoundNumber().add(BigInteger.ONE)
                : BigInteger.ONE;

        return new GrandpaRound(round,
                newRoundNumber,
                authSetAtBlock.getSetId(),
                authSetAtBlock.getAuthorities(),
                grandpaSetState.getThreshold(authSetAtBlock.getAuthorities()),
                isPrimary(newRoundNumber, authSetAtBlock),
                lastFinalized
        );
    }

    private boolean isPrimary(BigInteger roundState, AuthoritySet authoritySet) {

        var keyPair = AbstractState.getGrandpaKeyPair();
        if (keyPair == null) {
            log.fine(("isPrimary: KeyPair is null."));
            return false;
        }

        GrandpaSetState grandpaSetState = stateManager.getGrandpaSetState();
        BigInteger primaryIndex = grandpaSetState.derivePrimary(roundState);

        return Arrays.equals(authoritySet.getAuthorities()
                        .get(primaryIndex.intValueExact())
                        .getPublicKey(),
                keyPair.getValue0()
        );
    }

    private boolean isFirstBlockOfSet(BigInteger blockNumber) {

        var pastSetChanges = stateManager.getGrandpaSetState().getPastSetChanges();

        for (Map.Entry<BigInteger, AuthoritySet> entry : pastSetChanges.reversed().entrySet()) {

            if (entry.getKey().compareTo(blockNumber) == 0) {
                return true;
            }
        }

        return false;
    }
}
