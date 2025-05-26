package com.limechain.consensus.grandpa.round;

import com.limechain.consensus.grandpa.dto.SubRound;
import com.limechain.consensus.grandpa.dto.Vote;
import lombok.extern.java.Log;

import java.time.Instant;

@Log
public class StartStage implements StageState {

    @Override
    public void start(GrandpaRound round) {

        log.fine(String.format("Round %d started", round.getRoundNumber()));

        round.setStartTime(Instant.now());

        round.getPeerMessageCoordinator().sendNeighborMessageToPeers();

        GrandpaRound previous = round.getPrevious();
        if (round.isPrimaryVoter()) {
            log.fine("We are a primary voter for round " + round.getRoundNumber());

            if (previous != null) {
                round.getPrevious().broadcastCommitMessage();
            }

            if (round.getBestFinalCandidate().getBlockNumber()
                    .compareTo(round.getLastFinalizedBlock().getBlockNumber()) >= 0) {
                doProposal(round);
            }
        }

        end(round);
    }

    @Override
    public void end(GrandpaRound round) {

        log.fine(String.format("Round %d ended start stage", round.getRoundNumber()));
        round.switchStage();
    }

    private void doProposal(GrandpaRound round) {

        if (round.getPrimaryVote() != null) {
            round.broadcastVoteMessage(round.getPrimaryVote(), SubRound.PRIMARY_PROPOSAL);
            return;
        }

        Vote primaryVote = Vote.fromBlockHeader(round.getBestFinalCandidate());
        round.setPrimaryVote(primaryVote);
        round.broadcastVoteMessage(primaryVote, SubRound.PRIMARY_PROPOSAL);
    }
}

