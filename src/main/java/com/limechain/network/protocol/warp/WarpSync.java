package com.limechain.network.protocol.warp;

import com.limechain.network.StrictProtocolBinding;
import com.limechain.network.protocol.warp.dto.WarpSyncResponse;
import io.libp2p.core.AddressBook;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import lombok.extern.java.Log;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

@Log
public class WarpSync extends StrictProtocolBinding<WarpSyncController> {

    public WarpSync(String protocolId, WarpSyncProtocol protocol) {
        super(protocolId, protocol);
    }

    public WarpSyncResponse warpSyncRequest(Host us, AddressBook addrs, PeerId peer, String blockHash) {
        WarpSyncController controller = dialPeer(us, peer, addrs);
        try {
            WarpSyncResponse resp = controller.warpSyncRequest(blockHash).get(10, TimeUnit.SECONDS);
            log.log(Level.INFO, "Received warp sync response with " + resp.getFragments().length + " fragments");
            return resp;
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            log.log(Level.SEVERE, "Error while sending remote call request: ", e);
            throw new RuntimeException(e);
        }
    }
}
