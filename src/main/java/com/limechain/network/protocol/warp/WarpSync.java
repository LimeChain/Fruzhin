package com.limechain.network.protocol.warp;

import com.limechain.exception.global.ExecutionFailedException;
import com.limechain.exception.global.ThreadInterruptedException;
import com.limechain.network.StrictProtocolBinding;
import com.limechain.network.protocol.warp.dto.WarpSyncResponse;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import lombok.extern.java.Log;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Log
public class WarpSync extends StrictProtocolBinding<WarpSyncController> {

    public WarpSync(String protocolId, WarpSyncProtocol protocol) {
        super(protocolId, protocol);
    }

    public WarpSyncResponse warpSyncRequest(Host us, PeerId peer, String blockHash) {
        try {
            WarpSyncController controller = dialPeer(us, peer, us.getAddressBook());
            WarpSyncResponse resp = controller.warpSyncRequest(blockHash).get(20, TimeUnit.SECONDS);
            log.info(String.format("Received warp sync response with %d fragments", resp.getFragments().length));
            return resp;
        } catch (ExecutionException | TimeoutException | IllegalStateException e) {
            log.severe(String.format("Error while sending remote call request: %s", e));
            throw new ExecutionFailedException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ThreadInterruptedException(e);
        }
    }
}
