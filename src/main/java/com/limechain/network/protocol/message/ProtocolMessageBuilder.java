package com.limechain.network.protocol.message;

import com.limechain.consensus.grandpa.GrandpaSetState;
import com.limechain.network.protocol.blockannounce.messages.BlockAnnounceMessage;
import com.limechain.network.protocol.grandpa.messages.neighbour.NeighbourMessage;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.rpc.server.AppBean;
import com.limechain.state.StateManager;
import com.limechain.sync.state.SyncState;
import lombok.experimental.UtilityClass;

import java.math.BigInteger;

@UtilityClass
public class ProtocolMessageBuilder {
    private final int NEIGHBOUR_MESSAGE_VERSION = 1;

    public NeighbourMessage buildNeighbourMessage() {
        StateManager stateManager = AppBean.getBean(StateManager.class);
        GrandpaSetState grandpaSetState = stateManager.getGrandpaSetState();
        SyncState syncState = stateManager.getSyncState();

        BigInteger setId = grandpaSetState.getAuthoritySet().getSetId() == null
                ? BigInteger.ZERO
                : grandpaSetState.getAuthoritySet().getSetId();
        BigInteger roundNumber = grandpaSetState.getCurrentGrandpaRound() == null
                ? BigInteger.ONE
                : grandpaSetState.getCurrentGrandpaRound().getRoundNumber();
        BigInteger bestFinalizedBlockNumber = syncState.getLastFinalizedBlockNumber();

        return new NeighbourMessage(
                NEIGHBOUR_MESSAGE_VERSION,
                roundNumber,
                setId,
                bestFinalizedBlockNumber
        );
    }

    public BlockAnnounceMessage buildBlockAnnounceMessage(BlockHeader blockHeader, boolean isBestBlock) {
        return new BlockAnnounceMessage(
                blockHeader,
                isBestBlock
        );
    }
}
