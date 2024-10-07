package com.limechain.network.protocol.sync;

import com.limechain.exception.global.ExecutionFailedException;
import com.limechain.exception.global.ThreadInterruptedException;
import com.limechain.network.StrictProtocolBinding;
import com.limechain.network.protocol.sync.pb.SyncMessage;
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

    public SyncMessage.BlockResponse remoteBlockRequest(Host us, PeerId peer,
                                                        BlockRequestDto blockRequest) {
        try {
            SyncController controller = dialPeer(us, peer, us.getAddressBook());
            SyncMessage.BlockResponse response = controller
                    .sendBlockRequest(blockRequest.getFields(), blockRequest.getHash(), blockRequest.getNumber(),
                            blockRequest.getDirection(), blockRequest.getMaxBlocks())
                    .get(2, TimeUnit.SECONDS);
            log.log(Level.FINE, "Received blocks: " + response.getBlocksCount());
            return response;
        } catch (ExecutionException | TimeoutException | IllegalStateException e) {
            log.log(Level.SEVERE, "Error while sending remote block request: ", e);
            throw new ExecutionFailedException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ThreadInterruptedException(e);
        }
    }
}
