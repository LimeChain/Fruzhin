package com.limechain.network.protocol.blockannounce;

import com.limechain.network.ConnectionManager;
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
import org.apache.tomcat.util.buf.HexUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;

@Log
public class BlockAnnounceEngine {
    public static final int HANDSHAKE_LENGTH = 69;
    protected ConnectionManager connectionManager = ConnectionManager.getInstance();
    protected SyncedState syncedState = SyncedState.getInstance();

    public void receiveRequest(byte[] msg, Stream stream) {
        PeerId peerId = stream.remotePeerId();
        boolean connectedToPeer = connectionManager.isBlockAnnounceConnected(peerId);
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
            stream.close();
        } else {
            ScaleCodecReader reader = new ScaleCodecReader(msg);
            BlockAnnounceHandshake handshake = reader.read(new BlockAnnounceHandshakeScaleReader());
            connectionManager.addBlockAnnounceStream(stream);
            connectionManager.updatePeer(peerId, handshake);
            log.log(Level.INFO, "Received handshake from " + peerId + "\n" +
                    handshake);
            writeHandshakeToStream(stream, peerId);
        }
    }

    private void handleBlockAnnounce(byte[] msg, PeerId peerId) {
        ScaleCodecReader reader = new ScaleCodecReader(msg);
        BlockAnnounceMessage announce = reader.read(new BlockAnnounceMessageScaleReader());
        connectionManager.updatePeer(peerId, announce);
        log.log(Level.FINE, "Received block announce for block #" + announce.getHeader().getBlockNumber() +
                " from " + peerId +
                " with hash:0x" + HexUtils.toHexString(announce.getHeader().getHash()) +
                " parentHash:" + announce.getHeader().getParentHash() +
                " stateRoot:" + announce.getHeader().getStateRoot());
        //TODO: Should update Trie (and merkle proofs?) and save them in db
    }

    public void writeHandshakeToStream(Stream stream, PeerId peerId) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (ScaleCodecWriter writer = new ScaleCodecWriter(buf)) {
            writer.write(new BlockAnnounceHandshakeScaleWriter(), syncedState.getHandshake());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        log.log(Level.INFO, "Sending handshake to " + peerId);
        stream.writeAndFlush(buf.toByteArray());
    }
}
