package com.limechain.sync.warpsync.state;

import com.limechain.network.protocol.lightclient.pb.LightClientMessage;
import com.limechain.sync.warpsync.SyncedState;
import com.limechain.sync.warpsync.WarpSyncMachine;
import com.limechain.utils.StringUtils;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.extern.java.Log;

import java.util.logging.Level;

@Log
public class RuntimeDownloadState implements WarpSyncState {
    private final SyncedState syncedState = SyncedState.getInstance();
    private Exception error;

    @Override
    public void next(WarpSyncMachine sync) {
        if (this.error != null) {
            sync.setWarpSyncState(new RequestFragmentsState(syncedState.getLastFinalizedBlockHash()));
            return;
        }
        // After runtime is downloaded, we have to build the runtime and then build chain information
        sync.setWarpSyncState(new RuntimeBuildState());
    }

    @Override
    public void handle(WarpSyncMachine sync) {
        byte[][] merkleProof = syncedState.loadProof();
        Hash256 stateRoot = syncedState.loadStateRoot();
        if (merkleProof != null && stateRoot != null) {
            log.log(Level.INFO, "Loading saved runtime...");
            try {
                syncedState.setCodeAndHeapPages(merkleProof, stateRoot);
            } catch (RuntimeException e) {
                this.error = e;
            }
            return;
        }
        log.log(Level.INFO, "Downloading runtime...");

        LightClientMessage.Response response = sync.getNetworkService().makeRemoteReadRequest(
                syncedState.getLastFinalizedBlockHash().toString(),
                new String[]{StringUtils.toHex(":code")});

        byte[] proof = response.getRemoteReadResponse().getProof().toByteArray();
        byte[][] decodedProofs = decodeProof(proof);
        
        syncedState.saveProofState(decodedProofs);

        syncedState.setCodeAndHeapPages(decodedProofs, syncedState.getStateRoot());
    }

    private byte[][] decodeProof(byte[] proof) {
        ScaleCodecReader reader = new ScaleCodecReader(proof);
        int size = reader.readCompactInt();
        byte[][] decodedProofs = new byte[size][];

        for (int i = 0; i < size; ++i) {
            decodedProofs[i] = reader.readByteArray();
        }
        return decodedProofs;
    }
}
