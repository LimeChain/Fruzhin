package com.limechain.network.protocol.blockannounce;

import com.limechain.network.ConnectionManager;
import com.limechain.network.PeerInfo;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandshake;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandshakeScaleReader;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandshakeScaleWriter;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceMessage;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceMessageScaleReader;
import com.limechain.sync.warpsync.SyncedState;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.libp2p.core.PeerId;
import io.libp2p.core.Stream;
import lombok.extern.java.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;

@Log
public class BlockAnnounceEngine {
    private static final int HANDSHAKE_LENGTH = 69;
    private final ConnectionManager connectionManager = ConnectionManager.getInstance();
    private final SyncedState syncedState = SyncedState.getInstance();

    public void receiveRequest(byte[] msg, PeerId peerId, Stream stream) {
        boolean connectedToPeer = connectionManager.isConnected(peerId);
        boolean isHandshake = msg.length == HANDSHAKE_LENGTH;

        if (!connectedToPeer && !isHandshake) {
            log.log(Level.WARNING, "No handshake for block announce message from Peer " + peerId);
            return;
        }

        if (isHandshake) {
            handleHandshake(msg, peerId, stream, connectedToPeer);
        } else {
            handleBlockAnnounce(msg, peerId);
        }

        //TODO: Send message to network? module
    }

    private void handleHandshake(byte[] msg, PeerId peerId, Stream stream, boolean connectedToPeer) {
        /*  We might not need to send a second handshake.
        If we already have stored the key it means that we have processed the handshake once.
        This might be caused by using a different stream for sending and receiving in libp2p.
        */
        if (connectedToPeer) {
            log.log(Level.INFO, "Received existing handshake from " + peerId);
        } else {
            ScaleCodecReader reader = new ScaleCodecReader(msg);
            BlockAnnounceHandshake handshake = reader.read(new BlockAnnounceHandshakeScaleReader());
            connectionManager.addPeer(peerId, new PeerInfo(handshake));
            log.log(Level.INFO, "Received handshake from " + peerId + "\n" +
                    handshake);
            writeHandshakeToStream(stream, peerId);
            //syncedState.grandpaSync(peerId);
        }
    }

    private void handleBlockAnnounce(byte[] msg, PeerId peerId) {
        ScaleCodecReader reader = new ScaleCodecReader(msg);
        BlockAnnounceMessage announce = reader.read(new BlockAnnounceMessageScaleReader());
        connectionManager.updatePeer(peerId, announce);
        log.log(Level.INFO, "Received block announce: \n" + announce);
       // syncedState.syncAnnouncedBlock(peerId, announce);
    }

    public void writeHandshakeToStream(Stream stream, PeerId peerId) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (ScaleCodecWriter writer = new ScaleCodecWriter(buf)) {
            writer.write(new BlockAnnounceHandshakeScaleWriter(), SyncedState.getInstance().getHandshake());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        log.log(Level.INFO, "Sending handshake to " + peerId);
        stream.writeAndFlush(buf.toByteArray());
    }

    public void removePeerHandshake(PeerId peerId) {
        connectionManager.removePeer(peerId);
    }
}
