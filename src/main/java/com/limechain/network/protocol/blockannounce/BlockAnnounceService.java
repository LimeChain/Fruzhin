package com.limechain.network.protocol.blockannounce;

import com.limechain.network.protocol.NetworkService;
import io.libp2p.core.AddressBook;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;

public class BlockAnnounceService extends NetworkService<BlockAnnounce> {
    public BlockAnnounceService(String protocolId) {
        this.protocol = new BlockAnnounce(protocolId, new BlockAnnounceProtocol());
    }

    public void sendHandshake(Host us, AddressBook addrs, PeerId peer) {
        BlockAnnounceController controller = this.protocol.dialPeer(us, peer, addrs);
        controller.sendHandshake();
    }
}
