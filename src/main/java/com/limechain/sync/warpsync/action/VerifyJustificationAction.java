package com.limechain.sync.warpsync.action;

import com.limechain.exception.sync.JustificationVerificationException;
import com.limechain.network.protocol.warp.DigestHelper;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.network.protocol.warp.dto.WarpSyncFragment;
import com.limechain.rpc.server.AppBean;
import com.limechain.state.StateManager;
import com.limechain.sync.JustificationVerifier;
import com.limechain.sync.state.SyncState;
import com.limechain.sync.warpsync.WarpSyncMachine;
import com.limechain.sync.warpsync.WarpSyncState;
import com.limechain.utils.HashUtils;
import lombok.extern.java.Log;

// VerifyJustificationState is going to be instantiated a lot of times
// Maybe we can make it a singleton in order to reduce performance overhead?
@Log
public class VerifyJustificationAction implements WarpSyncAction {

    private final WarpSyncState warpSyncState;
    private final StateManager stateManager;
    private Exception error;

    public VerifyJustificationAction() {
        this.stateManager = AppBean.getBean(StateManager.class);
        this.warpSyncState = AppBean.getBean(WarpSyncState.class);
    }

    @Override
    public void next(WarpSyncMachine sync) {
        if (this.error != null) {
            // Not sure what state we should transition to here.
            sync.setWarpSyncAction(new FinishedAction());
            return;
        }

        if (!sync.getFragmentsQueue().isEmpty()) {
            sync.setWarpSyncAction(new VerifyJustificationAction());
        } else if (warpSyncState.isWarpSyncFragmentsFinished()) {
            sync.setWarpSyncAction(new RuntimeDownloadAction());
        } else {
            sync.setWarpSyncAction(new RequestFragmentsAction(stateManager.getSyncState().getLastFinalizedBlockHash()));
        }
    }

    @Override
    public void handle(WarpSyncMachine sync) {
        try {

            SyncState syncState = stateManager.getSyncState();
            // Executes scheduled or forced authority changes for the last finalized block.
            stateManager.getGrandpaSetState()
                    .applyAuthoritySetChange(
                            syncState.getLastFinalizedBlockHash(),
                            syncState.getLastFinalizedBlockNumber()
                    );

            WarpSyncFragment fragment = sync.getFragmentsQueue().poll();
            log.info("Verifying justification...");

            if (fragment == null) {
                throw new JustificationVerificationException("No such fragment");
            }

            boolean verified = JustificationVerifier.verify(fragment.getJustification());

            if (!verified) {
                throw new JustificationVerificationException("Justification could not be verified.");
            }

            syncState.finalizeBlock(fragment.getHeader());
            handleConsensusMessages(fragment);

        } catch (Exception e) {
            log.warning(String.format("Error while verifying justification: %s", e.getMessage()));
            this.error = e;
        }
    }

    private void handleConsensusMessages(WarpSyncFragment fragment) {
        BlockHeader header = fragment.getHeader();

        DigestHelper.getGrandpaConsensusMessages(header.getDigest())
                .forEach(cm -> stateManager.getGrandpaSetState().handleGrandpaConsensusMessage(
                        cm, header)
                );

        DigestHelper.getBeefyConsensusMessages(header.getDigest())
                .forEach(cm -> stateManager.getBeefyState().handleBeefyConsensusMessage(
                        cm, header.getBlockNumber())
                );

        DigestHelper.getBabeConsensusMessages(header.getDigest())
                .forEach(cm -> stateManager.getEpochState().updateNextEpochConfig(cm));

        SyncState syncState = stateManager.getSyncState();
        log.info(String.format("Verified justification. Block #%d (%s) with state root %s",
                syncState.getLastFinalizedBlockNumber(),
                HashUtils.getPrintableHash(syncState.getLastFinalizedBlockHash()),
                syncState.getStateRoot()));
    }
}
