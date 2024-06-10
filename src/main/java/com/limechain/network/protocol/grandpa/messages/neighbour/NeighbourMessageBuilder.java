package com.limechain.network.protocol.grandpa.messages.neighbour;

import com.limechain.rpc.server.AppBean;
import com.limechain.storage.block.SyncState;


public class NeighbourMessageBuilder {
    private static final int NEIGHBOUR_MESSAGE_VERSION = 1;

    public NeighbourMessage getNeighbourMessage() {
        SyncState syncState = AppBean.getBean(SyncState.class);

        return new NeighbourMessage(
                NEIGHBOUR_MESSAGE_VERSION,
                syncState.getLatestRound(),
                syncState.getSetId(),
                syncState.getLastFinalizedBlockNumber()
        );
    }
}
