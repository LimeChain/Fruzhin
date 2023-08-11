package com.limechain.network;

import io.libp2p.core.AddressBook;
import io.libp2p.core.Host;
import io.libp2p.core.P2PChannelHandler;
import io.libp2p.core.PeerId;
import io.libp2p.core.multiformats.Multiaddr;

public abstract class StrictProtocolBinding<T> extends io.libp2p.core.multistream.StrictProtocolBinding<T> {
    public StrictProtocolBinding(String protocolId, P2PChannelHandler<T> protocol) {
        super(protocolId, protocol);
    }

    public T dialPeer(Host us, PeerId peer, AddressBook addrs) {
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
