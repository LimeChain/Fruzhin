package com.limechain.network.protocol.state;

import com.limechain.exception.global.ExecutionFailedException;
import com.limechain.exception.global.ThreadInterruptedException;
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
public class StateMessages extends StrictProtocolBinding<StateController> {
    public StateMessages(String protocolId, StateProtocol protocol) {
        super(protocolId, protocol);
    }

    public SyncMessage.StateResponse remoteStateRequest(Host us, AddressBook addrs, PeerId peer,
                                                        String blockHash) {
        try {
            StateController controller = dialPeer(us, peer, addrs);

            return controller
                    .sendStateRequest(StringUtils.remove0xPrefix(blockHash))
                    .get(10, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException | IllegalStateException e) {
            log.log(Level.SEVERE, "Error while sending remote state: ", e);
            throw new ExecutionFailedException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ThreadInterruptedException(e);
        }
    }
}
