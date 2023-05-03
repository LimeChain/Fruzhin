package com.limechain.network.protocol.blockannounce;

import com.limechain.network.StrictProtocolBinding;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandshake;
import io.libp2p.core.AddressBook;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
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
}
