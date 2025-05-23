package com.limechain.network.protocol.ping;

import com.limechain.exception.global.ExecutionFailedException;
import com.limechain.exception.global.ThreadInterruptedException;
import com.limechain.network.StrictProtocolBinding;
import io.libp2p.core.AddressBook;
import io.libp2p.core.Host;
import io.libp2p.core.P2PChannelHandler;
import io.libp2p.core.PeerId;
import io.libp2p.protocol.PingController;
import lombok.extern.java.Log;

import java.util.concurrent.ExecutionException;

@Log
public class Ping extends StrictProtocolBinding<PingController> {
    public Ping(String protocolId, P2PChannelHandler<PingController> protocol) {
        super(protocolId, protocol);
    }

    public Long ping(Host us, AddressBook addrs, PeerId peer) {
        try {
            PingController controller = dialPeer(us, peer, addrs);
            Long resp = controller.ping().get();
            log.finest(String.format("Received response: %s ms", resp.toString()));
            return resp;
        } catch (ExecutionException | IllegalStateException e) {
            log.severe(String.format("Error while sending ping request: %s", e.getMessage()));
            throw new ExecutionFailedException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ThreadInterruptedException(e);
        }
    }
}
