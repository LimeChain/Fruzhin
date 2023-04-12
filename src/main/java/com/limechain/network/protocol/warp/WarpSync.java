package com.limechain.network.protocol.warp;

import com.limechain.network.protocol.warp.dto.WarpSyncResponse;
import io.libp2p.core.AddressBook;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.core.multiformats.Multiaddr;
import io.libp2p.core.multistream.StrictProtocolBinding;
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
            log.log(Level.INFO, "Received response: " + resp.toString());
            return resp;
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            log.log(Level.SEVERE, "Error while sending remote call request: ", e);
            throw new RuntimeException(e);
        }
    }

    private WarpSyncController dialPeer(Host us, PeerId peer, AddressBook addrs) {
        Multiaddr[] addr = addrs.get(peer).join().toArray(new Multiaddr[0]);
        if (addr.length == 0)
            throw new IllegalStateException("No addresses known for peer " + peer);

        return dial(us, peer, addr).getController().join();
    }
}
