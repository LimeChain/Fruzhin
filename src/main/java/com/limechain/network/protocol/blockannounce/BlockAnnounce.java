package com.limechain.network.protocol.blockannounce;

import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandshake;
import io.libp2p.core.AddressBook;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.core.multiformats.Multiaddr;
import io.libp2p.core.multistream.StrictProtocolBinding;
import lombok.extern.java.Log;

import java.util.logging.Level;

@Log
public class BlockAnnounce extends StrictProtocolBinding<BlockAnnounceController> {
    public BlockAnnounce(String protocolId, BlockAnnounceProtocol protocol) {
        super(protocolId, protocol);
    }

    public void sendHandshake(Host us, AddressBook addrs, PeerId peer, BlockAnnounceHandshake handshake) {
        BlockAnnounceController controller = dialPeer(us, peer, addrs);
        controller.sendHandshake(handshake);
        log.log(Level.INFO, "Sent block announce handshake");
    }

    private BlockAnnounceController dialPeer(Host us, PeerId peer, AddressBook addrs) {
        Multiaddr[] addr = addrs.get(peer)
                .join().stream()
                .filter(address -> !address.toString().contains("/ws") && !address.toString().contains("/wss"))
                .toList()
                .toArray(new Multiaddr[0]);
        if (addr.length == 0)
            throw new IllegalStateException("No addresses known for peer " + peer);

        return dial(us, peer, addr).getController().join();
    }
}
