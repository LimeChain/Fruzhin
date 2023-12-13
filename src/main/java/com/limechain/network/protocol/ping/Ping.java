package com.limechain.network.protocol.ping;

import com.limechain.exception.ExecutionFailedException;
import com.limechain.exception.ThreadInterruptedException;
import com.limechain.network.StrictProtocolBinding;
import io.libp2p.core.AddressBook;
import io.libp2p.core.Host;
import io.libp2p.core.P2PChannelHandler;
import io.libp2p.core.PeerId;
import io.libp2p.protocol.PingController;
import lombok.extern.java.Log;

import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

@Log
public class Ping extends StrictProtocolBinding<PingController> {
    public Ping(String protocolId, P2PChannelHandler<PingController> protocol) {
        super(protocolId, protocol);
    }

    public Long ping(Host us, AddressBook addrs, PeerId peer) {
        PingController controller = dialPeer(us, peer, addrs);
        try {
            Long resp = controller.ping().get();
            log.log(Level.INFO, "Received response: " + resp.toString());
            return resp;
        } catch (ExecutionException e) {
            log.log(Level.SEVERE, "Error while sending ping request: ", e);
            throw new ExecutionFailedException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ThreadInterruptedException(e);
        }
    }

}
