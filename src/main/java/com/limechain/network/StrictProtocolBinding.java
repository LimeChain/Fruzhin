package com.limechain.network;

public abstract class StrictProtocolBinding<T> {
    protected StrictProtocolBinding(String protocolId/*, T protocol*/) {
    }

//    public T dialPeer(Host us, PeerId peer, AddressBook addrs) {
//        Multiaddr[] addr = addrs.get(peer)
//                .join().stream()
//                .filter(address -> !address.toString().contains("/ws") && !address.toString().contains("/wss"))
//                .toList()
//                .toArray(new Multiaddr[0]);
//        if (addr.length == 0)
//            throw new IllegalStateException("No addresses known for peer " + peer);
//
//        return dial(us, peer, addr).getController().join();
//    }
}
