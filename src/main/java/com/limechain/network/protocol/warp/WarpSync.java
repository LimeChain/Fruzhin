package com.limechain.network.protocol.warp;

import com.limechain.network.StrictProtocolBinding;
import lombok.extern.java.Log;

@Log
public class WarpSync extends StrictProtocolBinding<WarpSyncController> {

    public WarpSync(String protocolId, WarpSyncProtocol protocol) {
        super(protocolId/*, protocol*/);
    }

//    public WarpSyncResponse warpSyncRequest(Host us, PeerId peer, String blockHash) {
//        try {
//            WarpSyncController controller = dialPeer(us, peer, us.getAddressBook());
//            WarpSyncResponse resp = controller.warpSyncRequest(blockHash).get(10, TimeUnit.SECONDS);
//            log.log(Level.INFO, "Received warp sync response with " + resp.getFragments().length + " fragments");
//            return resp;
//        } catch (ExecutionException | TimeoutException | IllegalStateException e) {
//            log.log(Level.SEVERE, "Error while sending remote call request: ", e);
//            throw new ExecutionFailedException(e);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            throw new ThreadInterruptedException(e);
//        }
//    }
}
