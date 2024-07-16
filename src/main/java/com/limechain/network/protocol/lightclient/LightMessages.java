package com.limechain.network.protocol.lightclient;

import com.limechain.exception.global.ExecutionFailedException;
import com.limechain.exception.global.ThreadInterruptedException;
import com.limechain.network.StrictProtocolBinding;
import com.limechain.network.protocol.lightclient.pb.LightClientMessage;
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
public class LightMessages extends StrictProtocolBinding<LightMessagesController> {
    private static final String GENERIC_REMOTE_CALL_ERROR_MESSAGE = "Error while sending remote call request: ";

    public LightMessages(String protocolId, LightMessagesProtocol protocol) {
        super(protocolId, protocol);
    }

    public LightClientMessage.Response remoteCallRequest(Host us, AddressBook addrs, PeerId peer,
                                                         String blockHash,
                                                         String method,
                                                         String data) {
        try {
            LightMessagesController controller = dialPeer(us, peer, addrs);
            LightClientMessage.Response resp = controller
                    .remoteCallRequest(StringUtils.remove0xPrefix(blockHash), method, data)
                    .get();
            log.log(Level.INFO, "Received response with length: " + resp.toByteArray().length);
            return resp;
        } catch (ExecutionException | IllegalStateException e) {
            log.log(Level.SEVERE, GENERIC_REMOTE_CALL_ERROR_MESSAGE, e);
            throw new ExecutionFailedException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ThreadInterruptedException(e);
        }
    }

    public LightClientMessage.Response remoteReadRequest(Host us, PeerId peer,
                                                         String blockHash,
                                                         String[] hexKeys) {
        try {
            LightMessagesController controller = dialPeer(us, peer, us.getAddressBook());
            LightClientMessage.Response resp = controller.remoteReadRequest(
                            StringUtils.remove0xPrefix(blockHash),
                            hexKeys)
                    .get(10, TimeUnit.SECONDS);
            log.log(Level.INFO, "Received light client message response with length: " + resp.toByteArray().length);
            return resp;
        } catch (ExecutionException | TimeoutException | IllegalStateException e) {
            log.log(Level.SEVERE, GENERIC_REMOTE_CALL_ERROR_MESSAGE, e);
            throw new ExecutionFailedException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ThreadInterruptedException(e);
        }
    }

    public LightClientMessage.Response remoteReadChildRequest(Host us, AddressBook addrs, PeerId peer,
                                                              String blockHash,
                                                              String childStorageKey,
                                                              String[] keys) {
        try {
            LightMessagesController controller = dialPeer(us, peer, addrs);
            LightClientMessage.Response resp = controller
                    .remoteReadChildRequest(StringUtils.remove0xPrefix(blockHash), childStorageKey, keys)
                    .get();
            log.log(Level.INFO, "Received response: " + resp.toString());
            return resp;
        } catch (ExecutionException | IllegalStateException e) {
            log.log(Level.SEVERE, GENERIC_REMOTE_CALL_ERROR_MESSAGE, e);
            throw new ExecutionFailedException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ThreadInterruptedException(e);
        }
    }
}
