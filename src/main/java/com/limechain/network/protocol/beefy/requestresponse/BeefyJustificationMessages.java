package com.limechain.network.protocol.beefy.requestresponse;

import com.limechain.exception.global.ExecutionFailedException;
import com.limechain.exception.global.ThreadInterruptedException;
import com.limechain.network.StrictProtocolBinding;
import com.limechain.network.protocol.beefy.messages.justification.SignedCommitment;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import lombok.extern.java.Log;

import java.math.BigInteger;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Log
public class BeefyJustificationMessages extends StrictProtocolBinding<BeefyJustificationController> {
    public BeefyJustificationMessages(String protocolId, BeefyJustificationProtocol protocol) {
        super(protocolId, protocol);
    }

    public SignedCommitment remoteJustificationRequest(Host us, PeerId peer, BigInteger from) {
        try {
            if (BeefyJustificationProtocol.Sender.isRequestOngoing) {
                throw new IllegalArgumentException("BeefyJustificationProtocol.Sender is already ongoing.");
            }

            BeefyJustificationController controller = dialPeer(us, peer, us.getAddressBook());

            return controller
                    .sendJustificationRequest(from)
                    .get(1, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException | IllegalStateException e) {
            log.warning(String.format("Error while sending remote beefy justification request: %s", e));
            throw new ExecutionFailedException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ThreadInterruptedException(e);
        }
    }
}
