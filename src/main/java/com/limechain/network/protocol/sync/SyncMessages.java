package com.limechain.network.protocol.sync;

import com.limechain.network.StrictProtocolBinding;
import com.limechain.network.protocol.sync.pb.SyncMessage;
import com.limechain.utils.StringUtils;
import io.libp2p.core.AddressBook;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import lombok.extern.java.Log;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

@Log
public class SyncMessages extends StrictProtocolBinding<SyncController> {
    public SyncMessages(String protocolId, SyncProtocol protocol) {
        super(protocolId, protocol);
    }

    public SyncMessage.BlockResponse remoteBlockRequest(Host us, AddressBook addrs, PeerId peer,
                                                        Integer fields,
                                                        String hash,
                                                        Integer number,
                                                        SyncMessage.Direction direction,
                                                        int maxBlocks) {
        SyncController controller = dialPeer(us, peer, addrs);
        try {
            SyncMessage.BlockResponse response = controller
                    .sendBlockRequest(fields, hash, number, direction, maxBlocks)
                    .get(2, TimeUnit.SECONDS);
            log.log(Level.INFO, "Received response: " + response.toString());
            return response;
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error while sending remote call request: ", e);
            throw new RuntimeException(e);
        }
    }

    public SyncMessage.StateResponse warpSyncRequest(Host us, AddressBook addrs, PeerId peer, String blockHash) {
        SyncController controller = dialPeer(us, peer, addrs);
        try {
            SyncMessage.StateResponse resp = controller.sendStateRequest(StringUtils.remove0xPrefix(blockHash))
                    .get(30, TimeUnit.SECONDS);
            log.log(Level.INFO, "Received warp sync response with " +
                    resp.getProof().toByteArray().length + " fragments");
            return resp;
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            log.log(Level.SEVERE, "Error while sending remote call request: ", e);
            throw new RuntimeException(e);
        }
    }
}
