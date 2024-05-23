package com.limechain.network.protocol.blockannounce;

import com.limechain.exception.scale.ScaleEncodingException;
import com.limechain.exception.storage.BlockNodeNotFoundException;
import com.limechain.network.ConnectionManager;
import com.limechain.network.protocol.blockannounce.messages.BlockAnnounceHandshake;
import com.limechain.network.protocol.blockannounce.messages.BlockAnnounceHandshakeBuilder;
import com.limechain.network.protocol.blockannounce.messages.BlockAnnounceMessage;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandshakeScaleReader;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandshakeScaleWriter;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceMessageScaleReader;
import com.limechain.network.protocol.warp.dto.Block;
import com.limechain.network.protocol.warp.dto.BlockBody;
import com.limechain.storage.block.BlockState;
import com.limechain.sync.warpsync.WarpSyncState;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.libp2p.core.PeerId;
import io.libp2p.core.Stream;
import lombok.extern.java.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;

@Log
public class BlockAnnounceEngine {
    public static final int HANDSHAKE_LENGTH = 69;
    protected ConnectionManager connectionManager = ConnectionManager.getInstance();
    protected WarpSyncState warpSyncState = WarpSyncState.getInstance();
    protected BlockAnnounceHandshakeBuilder handshakeBuilder = new BlockAnnounceHandshakeBuilder();

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
        warpSyncState.syncBlockAnnounce(announce);
        log.log(Level.FINE, "Received block announce for block #" + announce.getHeader().getBlockNumber() +
                            " from " + peerId +
                            " with hash:0x" + announce.getHeader().getHash() +
                            " parentHash:" + announce.getHeader().getParentHash() +
                            " stateRoot:" + announce.getHeader().getStateRoot());

        if (BlockState.getInstance().isInitialized()) {
            try {
                BlockState.getInstance().addBlock(new Block(announce.getHeader(), new BlockBody(new ArrayList<>())));
            } catch (BlockNodeNotFoundException ignored) {
                // Currently we ignore this exception, because our syncing strategy as full node is not implemented yet.
                // And thus when we receive a block announce and try to add it in the BlockState we will get this
                // exception because the parent block of the received one is not found in the BlockState.
            }
        }
    }

    public void writeHandshakeToStream(Stream stream, PeerId peerId) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (ScaleCodecWriter writer = new ScaleCodecWriter(buf)) {
            writer.write(new BlockAnnounceHandshakeScaleWriter(), handshakeBuilder.getBlockAnnounceHandshake());
        } catch (IOException e) {
            throw new ScaleEncodingException(e);
        }

        log.log(Level.INFO, "Sending handshake to " + peerId);
        stream.writeAndFlush(buf.toByteArray());
    }
}
