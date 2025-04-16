package com.limechain.consensus.grandpa;

import com.limechain.consensus.grandpa.dto.GrandpaAuthoritySet;
import com.limechain.consensus.grandpa.dto.RoundState;
import com.limechain.consensus.grandpa.round.GrandpaRound;
import com.limechain.exception.grandpa.GrandpaGenericException;
import com.limechain.exception.grandpa.GrandpaJustificationException;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.network.protocol.warp.dto.Justification;
import com.limechain.state.AbstractState;
import com.limechain.state.StateManager;
import com.limechain.storage.block.state.BlockState;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.javatuples.Pair;
import org.jetbrains.annotations.NotNull;
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

            log.info(String.format("start: Grandpa service started with round #%d",
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
        GrandpaAuthoritySet authoritySet = roundState.getAuthoritySet();

        return new GrandpaRound(roundState,
                grandpaSetState.getThreshold(authoritySet.getAuthorities()),
                isPrimary(roundState.getRoundNumber(), authoritySet));
    }

    public Optional<GrandpaAuthoritySet> getAuthoritiesForBlock(BigInteger blockNumber) {
        GrandpaAuthoritySet authorities = null;

        GrandpaSetState grandpaSetState = stateManager.getGrandpaSetState();

        for (Map.Entry<Pair<Hash256, BigInteger>, GrandpaAuthoritySet> entry :
                grandpaSetState.getSetChanges().entrySet()) {

            if (entry.getKey().getValue1().compareTo(blockNumber) <= 0) {
                authorities = entry.getValue();
            } else {
                break;
            }
        }

        return Optional.ofNullable(authorities);
    }

    public void finalizeJustification(Justification justification) {

        GrandpaSetState grandpaSetState = stateManager.getGrandpaSetState();
        BlockState blockState = stateManager.getBlockState();

        Optional<GrandpaAuthoritySet> authoritiesOptional = getAuthoritiesForBlock(justification.getTargetBlock());
        if (authoritiesOptional.isEmpty()) {
            throw new GrandpaJustificationException(String.format("No grandpa authorities found for block: %d",
                    justification.getTargetBlock()));
        }

        GrandpaAuthoritySet authoritiesForBlock = authoritiesOptional.get();
        GrandpaRound justificationRound = grandpaSetState.getGrandpaRound(justification.getRoundNumber());

        // If there is an ongoing round for the received justification we can directly proceed to finalizing it.
        // Otherwise, we have 2 options:
        // We have an ongoing round whose number is equal to the received justification's number - 1.
        // We don't find a round that corresponds to the justification, nor it's previous number.
        boolean shouldUpdateCurrentRound = false;
        if (justificationRound == null) {
            BlockHeader lastFinalized = blockState.getHighestFinalizedHeader();
            if (lastFinalized.getBlockNumber().compareTo(justification.getTargetBlock()) > 0) {
                throw new GrandpaJustificationException("Trying to apply grandpa justification for past block.");
            }

            GrandpaRound previousRound = grandpaSetState.getGrandpaRound(justification.getRoundNumber()
                    .subtract(BigInteger.ONE));

            if (previousRound != null) {

                // If we find the previous round we can use it to create the round corresponding to the justification.
                justificationRound = createNextRound(previousRound);

                log.fine(String.format("finalizeJustification: Created new round #%d for set %d from previous.",
                        justificationRound.getRoundNumber(),
                        justificationRound.getAuthoritySet().getSetId()));
            } else {

                // If we do not have a previous/nor a corresponding round we have to create one from the justification.
                // In other words, we jump to the justification.
                justificationRound = initRoundFromJustification(justification, lastFinalized, authoritiesForBlock);

                log.fine(String.format("finalizeJustification: Created new initial round #%d for set %d.",
                        justificationRound.getRoundNumber(),
                        justificationRound.getAuthoritySet().getSetId()));
            }

            shouldUpdateCurrentRound = true;
        }

        if (shouldUpdateCurrentRound) {

            // Finalize currently ongoing round since it should be before the justification round.
            GrandpaRound current = grandpaSetState.getCurrentGrandpaRound();
            if (current != null && current.getRoundNumber().compareTo(justificationRound.getRoundNumber()) < 0) {
                grandpaSetState.getCurrentGrandpaRound().complete();
            }
            grandpaSetState.addNewGrandpaRound(justificationRound);
        }

        // Finalize the round corresponding to the justification.
        justificationRound.finalizeJustification(justification);
        // Start next round from the justification round.
        tryStartFromPreviousRound(justificationRound);
    }

    @NotNull
    private GrandpaRound initRoundFromJustification(Justification justification,
                                                    BlockHeader lastFinalized,
                                                    GrandpaAuthoritySet authoritiesForBlock) {

        GrandpaSetState grandpaSetState = stateManager.getGrandpaSetState();
        BlockState blockState = stateManager.getBlockState();

        RoundState roundState = RoundState.builder()
                .roundNumber(justification.getRoundNumber())
                .lastFinalizedBlock(lastFinalized)
                .finalizedBlock(blockState.getHeader(justification.getTargetHash()))
                .authoritySet(authoritiesForBlock)
                .build();

        GrandpaAuthoritySet currentAuthSet = grandpaSetState.getAuthoritySet();
        if (roundState.getAuthoritySet().getSetId().compareTo(currentAuthSet.getSetId()) < 0) {
            throw new GrandpaJustificationException("Trying to apply grandpa justification for past set.");
        }
        Pair<BigInteger, BigInteger> roundSetIdPair = blockState.getHighestRoundAndSetID();
        if (roundState.getRoundNumber().compareTo(roundSetIdPair.getValue0()) < 0) {
            throw new GrandpaJustificationException("Trying to apply grandpa justification for past round.");
        }

        return createInitialRound(roundState);
    }

    private void tryStartFromLastFinalizedBlock() {

        BlockState blockState = stateManager.getBlockState();
        GrandpaSetState grandpaSetState = stateManager.getGrandpaSetState();

        BlockHeader lastFinalized = blockState.getHighestFinalizedHeader();
        Optional<GrandpaAuthoritySet> authSetOpt = getAuthoritiesForBlock(lastFinalized.getBlockNumber());

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

            if (justificationOpt.isEmpty()) {
                playCurrentRound();
                return;
            }

            Justification justification = justificationOpt.get();
            if (!isFirstBlockOfSet(lastFinalized.getBlockNumber())) {
                stateBuilder.roundNumber(justification.getRoundNumber().add(BigInteger.ONE));
            }
        }

        RoundState roundState = stateBuilder.build();
        GrandpaRound currentRound = grandpaSetState.getCurrentGrandpaRound();
        if (currentRound != null && currentRound.getRoundNumber().compareTo(roundState.getRoundNumber()) == 0) {
            return;
        }

        GrandpaRound initialRound = createInitialRound(stateBuilder.build());
        grandpaSetState.addNewGrandpaRound(initialRound);
        playCurrentRound();
    }

    private void playCurrentRound() {
        if (AbstractState.isActiveAuthority()) {
            stateManager.getGrandpaSetState().getCurrentGrandpaRound().play();
        }
    }

    private GrandpaRound createNextRound(GrandpaRound previousRound) {

        GrandpaSetState grandpaSetState = stateManager.getGrandpaSetState();


        BlockHeader lastFinalized;
        try {
            lastFinalized = previousRound.getFinalizedBlock();
        } catch (GrandpaGenericException e) {
            lastFinalized = previousRound.getLastFinalizedBlock();
        }

        Optional<GrandpaAuthoritySet> authSetOpt = getAuthoritiesForBlock(lastFinalized.getBlockNumber());
        if (authSetOpt.isEmpty()) {
            log.fine(String.format("createNextRound: No authority set found for block %d",
                    lastFinalized.getBlockNumber()));
            throw new GrandpaGenericException("No authority set found for block " + lastFinalized.getBlockNumber());
        }

        GrandpaAuthoritySet authSetAtBlock = authSetOpt.get();
        BigInteger newRoundNumber = previousRound.getAuthoritySet().getSetId().equals(authSetAtBlock.getSetId())
                ? previousRound.getRoundNumber().add(BigInteger.ONE)
                : BigInteger.ONE;

        return new GrandpaRound(previousRound,
                newRoundNumber,
                authSetAtBlock.getSetId(),
                authSetAtBlock.getAuthorities(),
                grandpaSetState.getThreshold(authSetAtBlock.getAuthorities()),
                isPrimary(newRoundNumber, authSetAtBlock),
                lastFinalized
        );
    }

    private boolean isPrimary(BigInteger roundState, GrandpaAuthoritySet authoritySet) {

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

        var pastSetChanges = stateManager.getGrandpaSetState().getSetChanges();

        for (Map.Entry<Pair<Hash256, BigInteger>, GrandpaAuthoritySet> entry : pastSetChanges.reversed().entrySet()) {

            if (entry.getKey().getValue1().compareTo(blockNumber) == 0) {
                return true;
            }
        }

        return false;
    }
}
