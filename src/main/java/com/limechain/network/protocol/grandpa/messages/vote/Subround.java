package com.limechain.network.protocol.grandpa.messages.vote;

import java.util.Arrays;

public enum Subround {
    PREVOTE(0), PRECOMMIT(1), PRIMARY_PROPOSAL(2);

    public int getStage() {
        return stage;
    }

    private final int stage;

    Subround(int stage) {
        this.stage = stage;
    }

    public static Subround getByStage(int stage) {
        return Arrays.stream(values()).filter(t -> t.stage == stage).findFirst().orElse(null);
    }
}
