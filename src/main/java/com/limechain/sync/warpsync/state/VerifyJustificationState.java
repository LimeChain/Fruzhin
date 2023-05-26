package com.limechain.sync.warpsync.state;

import com.limechain.network.protocol.warp.dto.ConsensusEngine;
import com.limechain.network.protocol.warp.dto.HeaderDigest;
import com.limechain.network.protocol.warp.dto.WarpSyncFragment;
import com.limechain.sync.warpsync.dto.AuthoritySetChange;
import com.limechain.sync.warpsync.dto.GrandpaMessageType;
import com.limechain.sync.warpsync.WarpSyncMachine;
import com.limechain.sync.warpsync.scale.ForcedChangeReader;
import com.limechain.sync.warpsync.scale.ScheduledChangeReader;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import lombok.extern.java.Log;
import org.javatuples.Pair;

import java.math.BigInteger;
import java.util.logging.Level;

// VerifyJustificationState is going to be instantiated a lot of times
// Maybe we can make it a singleton in order to reduce performance overhead?
@Log
public class VerifyJustificationState implements WarpSyncState {
    private Exception error;

    @Override
    public void next(WarpSyncMachine sync) {
        if (this.error != null) {
            // Not sure what state we should transition to here.
            sync.setState(new FinishedState());
            return;
        }

        if (!sync.getFragmentsQueue().isEmpty()) {
            sync.setState(new VerifyJustificationState());
        } else if (sync.isFinished()) {
            sync.setState(new RuntimeDownloadState());
        } else {
            sync.setState(new RequestFragmentsState(sync.getLastFinalizedBlockHash()));
        }
    }

    @Override
    public void handle(WarpSyncMachine sync) {
        try {
            handleScheduledEvents(sync);

            WarpSyncFragment fragment = sync.getFragmentsQueue().poll();
            log.log(Level.INFO, "Verifying justification...");
            // TODO: Throw error if not verified!
            boolean verified = fragment.getJustification().verify(sync.getAuthoritySet(), sync.getSetId());
            if (!verified) {
                throw new Exception("Justification could not be verified.");
            }

            // Set the latest finalized header and number
            // TODO: Persist header to DB?
            sync.setLastFinalizedBlockHash(fragment.getJustification().targetHash);
            sync.setLastFinalizedBlockNumber(fragment.getJustification().targetBlock);

            handleAuthorityChanges(sync, fragment);
            log.log(Level.INFO, "Verified justification. Block hash is now at #"
                    + sync.getLastFinalizedBlockNumber() + ": " + sync.getLastFinalizedBlockHash().toString());
        } catch (Exception e) {
            log.log(Level.WARNING, "Error while verifying justification: " + e.getMessage());
            this.error = e;
        }
    }

    public void handleAuthorityChanges(WarpSyncMachine sync, WarpSyncFragment fragment) {
        // Update authority set and set id
        AuthoritySetChange authorityChanges;
        for (HeaderDigest digest : fragment.getHeader().getDigest()) {
            if (digest.getId() == ConsensusEngine.GRANDPA) {
                ScaleCodecReader reader = new ScaleCodecReader(digest.getMessage());
                GrandpaMessageType type = GrandpaMessageType.fromId(reader.readByte());
                if (type == GrandpaMessageType.SCHEDULED_CHANGE) {
                    ScheduledChangeReader authorityChangesReader = new ScheduledChangeReader();
                    authorityChanges = authorityChangesReader.read(reader);

                    sync.getScheduledAuthorityChanges()
                            .add(new Pair<>(authorityChanges.getDelay(), authorityChanges.getAuthorities()));
                    sync.setAuthoritySet(authorityChanges.getAuthorities());
                    return;
                }
                if (type == GrandpaMessageType.FORCED_CHANGE) {
                    ForcedChangeReader authorityChangesReader = new ForcedChangeReader();
                    authorityChanges = authorityChangesReader.read(reader);

                    sync.getScheduledAuthorityChanges()
                            .add(new Pair<>(authorityChanges.getDelay(), authorityChanges.getAuthorities()));
                    return;
                }
                if (type == GrandpaMessageType.ON_DISABLED) {
                    log.log(Level.SEVERE, "On disabled grandpa message not implemented");
                    return;
                }

                if (type == GrandpaMessageType.PAUSE) {
                    log.log(Level.SEVERE, "Pause grandpa message not implemented");
                    return;
                }
                if (type == GrandpaMessageType.RESUME) {
                    log.log(Level.SEVERE, "Resume grandpa message not implemented");
                    return;
                }
            }
        }
    }

    public void handleScheduledEvents(WarpSyncMachine sync) {
        var eventQueue = sync.getScheduledAuthorityChanges();
        var data = eventQueue.peek();
        while (data != null) {
            if (data.getValue0().compareTo(sync.getLastFinalizedBlockNumber()) != 1) {
                sync.setAuthoritySet(data.getValue1());
                sync.setSetId(sync.getSetId().add(BigInteger.ONE));
                sync.getScheduledAuthorityChanges().poll();
            } else break;
            data = eventQueue.peek();
        }
    }

}
