package com.limechain.network.protocol.lightclient;

import com.limechain.network.StrictProtocolBinding;
import com.limechain.network.protocol.lightclient.pb.LightClientMessage;
import com.limechain.utils.StringUtils;
import io.libp2p.core.AddressBook;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import lombok.extern.java.Log;

import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

@Log
public class LightMessages extends StrictProtocolBinding<LightMessagesController> {
    public LightMessages(String protocolId, LightMessagesProtocol protocol) {
        super(protocolId, protocol);
    }

    public LightClientMessage.Response remoteCallRequest(Host us, AddressBook addrs, PeerId peer,
                                                         String blockHash,
                                                         String method,
                                                         String data) {
        LightMessagesController controller = dialPeer(us, peer, addrs);
        try {
            LightClientMessage.Response resp = controller.remoteCallRequest(StringUtils.remove0xPrefix(blockHash), method, data).get();
            log.log(Level.INFO, "Received response: " + resp.toString());
            return resp;
        } catch (ExecutionException | InterruptedException e) {
            log.log(Level.SEVERE, "Error while sending remote call request: ", e);
            throw new RuntimeException(e);
        }
    }

    public LightClientMessage.Response remoteReadRequest(Host us, AddressBook addrs, PeerId peer,
                                                         String blockHash,
                                                         String[] hexKeys) {
        LightMessagesController controller = dialPeer(us, peer, addrs);
        try {
            LightClientMessage.Response resp = controller.remoteReadRequest(
                            StringUtils.remove0xPrefix(blockHash),
                            hexKeys)
                    .get();
            log.log(Level.INFO, "Received response: " + resp.toString());
            return resp;
        } catch (ExecutionException | InterruptedException e) {
            log.log(Level.SEVERE, "Error while sending remote call request: ", e);
            throw new RuntimeException(e);
        }
    }

    public LightClientMessage.Response remoteReadChildRequest(Host us, AddressBook addrs, PeerId peer,
                                                              String blockHash,
                                                              String childStorageKey,
                                                              String[] keys) {
        LightMessagesController controller = dialPeer(us, peer, addrs);
        try {
            LightClientMessage.Response resp =
                    controller.remoteReadChildRequest(StringUtils.remove0xPrefix(blockHash), childStorageKey, keys).get();
            log.log(Level.INFO, "Received response: " + resp.toString());
            return resp;
        } catch (ExecutionException | InterruptedException e) {
            log.log(Level.SEVERE, "Error while sending remote call request: ", e);
            throw new RuntimeException(e);
        }
    }
}
