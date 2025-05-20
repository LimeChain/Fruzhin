package com.limechain.consensus.grandpa.round;

import lombok.extern.java.Log;

@Log
public class CompletedStage implements StageState {

    @Override
    public void start(GrandpaRound round) {
        round.setOnFinalizeHandler(null);
        round.clearOnStageTimerHandler();
        end(round);
    }

    @Override
    public void end(GrandpaRound round) {
        log.fine(String.format("Round %d completed", round.getRoundNumber()));
    }
}

