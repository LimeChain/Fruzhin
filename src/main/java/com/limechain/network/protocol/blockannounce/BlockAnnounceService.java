package com.limechain.network.protocol.blockannounce;

import com.limechain.network.protocol.NetworkService;
import lombok.extern.java.Log;

@Log
public class BlockAnnounceService extends NetworkService<BlockAnnounce> {
    public BlockAnnounceService(String protocolId) {
        this.protocol = new BlockAnnounce(protocolId, new BlockAnnounceProtocol());
    }

//    public void sendHandshake(Host us, AddressBook addrs, PeerId peer) {
//        try{
//            BlockAnnounceController controller = this.protocol.dialPeer(us, peer, addrs);
//            controller.sendHandshake();
//        } catch (IllegalStateException e){
//            log.warning("Error sending handshake request to peer " + peer);
//        }
//
//    }
}
