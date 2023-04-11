package com.limechain.network.protocol.sync;

import com.limechain.network.substream.sync.pb.SyncMessage;
import io.libp2p.core.AddressBook;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.core.multiformats.Multiaddr;
import io.libp2p.core.multistream.StrictProtocolBinding;
import lombok.extern.java.Log;

import java.util.logging.Level;

@Log
public class SyncMessages extends StrictProtocolBinding<SyncController> {
    public SyncMessages(SyncProtocol protocol){
        super("/dot/sync/2", protocol);
    }

    public SyncMessage.BlockResponse remoteBlockRequest(Host us, AddressBook addrs, PeerId peer,
                                                       int fields,
                                                       String hash,
                                                       Integer number,
                                                       Integer toBlock,
                                                       SyncMessage.Direction direction,
                                                       int maxBlocks){
        SyncController controller = dialPeer(us,peer,addrs);
        try{
            SyncMessage.BlockResponse response = controller
                    .sendBlockRequest(fields, hash, number, toBlock, direction, maxBlocks)
                    .get();
            log.log(Level.INFO, "Received response: " + response.toString());
            return response;
        } catch (Exception e){
            log.log(Level.SEVERE, "Error while sending remote call request: ", e);
            throw new RuntimeException(e);
        }
    }

    private SyncController dialPeer(Host us, PeerId peer, AddressBook addrs){
        Multiaddr[] addr = addrs.get(peer).join().toArray(new Multiaddr[0]);
        if(addr.length == 0)
            throw new IllegalStateException("No addresses known for peer " + peer);
        return dial(us, peer, addr).getController().join();
    }
}
