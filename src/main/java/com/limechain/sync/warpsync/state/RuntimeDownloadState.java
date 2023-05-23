package com.limechain.sync.warpsync.state;

import com.limechain.merkle.MerkleProver;
import com.limechain.network.protocol.lightclient.pb.LightClientMessage;
import com.limechain.sync.warpsync.WarpSyncMachine;
import com.limechain.utils.StringUtils;
import lombok.extern.java.Log;

import java.util.logging.Level;

@Log
public class RuntimeDownloadState implements WarpSyncState {
    @Override
    public void next(WarpSyncMachine sync) {
        // After runtime is downloaded, we have to build the chain info
        sync.setState(new ChainInformationDownloadState());
    }

    @Override
    public void handle(WarpSyncMachine sync) {
        // TODO: Implement runtime download which is remoteReadRequest with keys :code and :heappages
        log.log(Level.INFO, "Downloading runtime...");
        LightClientMessage.Response response = sync.getNetworkService().makeRemoteReadRequest(
                sync.getLastFinalizedBlockHash().toString(),
                new String[]{StringUtils.toHex(":code"), StringUtils.toHex(":heappages")});

        log.log(Level.INFO, "Downloaded runtime. Response: " + response.toString());

        MerkleProver.decodeAndVerifyProof(response.getRemoteReadResponse().getProof().toByteArray(), sync.getStateRoot());
    }
}
