package com.limechain.network.protocol.blockannounce;

import com.limechain.exception.scale.ScaleEncodingException;
import com.limechain.network.protocol.BaseEngine;
import com.limechain.network.protocol.blockannounce.messages.BlockAnnounceHandshake;
import com.limechain.network.protocol.blockannounce.messages.BlockAnnounceHandshakeBuilder;
import com.limechain.network.protocol.blockannounce.messages.BlockAnnounceMessage;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandshakeScaleReader;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandshakeScaleWriter;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceMessageScaleReader;
import com.limechain.rpc.server.AppBean;
import com.limechain.storage.block.BlockHandler;
import com.limechain.storage.block.state.BlockState;
import com.limechain.sync.warpsync.WarpSyncState;
import com.limechain.utils.scale.ScaleUtils;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.libp2p.core.PeerId;
import io.libp2p.core.Stream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.logging.Level;

@Log
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class BlockAnnounceEngine extends BaseEngine {

    public static final int HANDSHAKE_LENGTH = 69;

    protected WarpSyncState warpSyncState;
    protected BlockAnnounceHandshakeBuilder handshakeBuilder;
    private BlockHandler blockHandler;

    public BlockAnnounceEngine() {
        warpSyncState = AppBean.getBean(WarpSyncState.class);
        blockHandler = AppBean.getBean(BlockHandler.class);
        handshakeBuilder = new BlockAnnounceHandshakeBuilder();
    }

    @Override
    protected void handleHandshake(byte[] message, PeerId peerId, Stream stream) {
        if (connectionManager.isBlockAnnounceConnected(peerId)) {
            log.log(Level.INFO, "Received existing handshake from " + peerId);
            stream.close();
        }

        ScaleCodecReader reader = new ScaleCodecReader(message);
        BlockAnnounceHandshake handshake = reader.read(BlockAnnounceHandshakeScaleReader.getInstance());
        connectionManager.addBlockAnnounceStream(stream);
        connectionManager.updatePeer(peerId, handshake);
        log.log(Level.INFO, "Received handshake from " + peerId + "\n" + handshake);

        writeHandshakeToStream(stream, peerId);
    }

    @Override
    public void receiveRequest(byte[] message, Stream stream) {
        PeerId peerId = stream.remotePeerId();
        boolean connectedToPeer = connectionManager.isBlockAnnounceConnected(peerId);
        boolean isHandshake = message.length == HANDSHAKE_LENGTH;

        if (!connectedToPeer && !isHandshake) {
            log.log(Level.WARNING, "No handshake for block announce message from Peer " + peerId);
            return;
        }

        if (isHandshake) {
            handleHandshake(message, peerId, stream);
        } else {
            handleBlockAnnounce(message, peerId);
        }

        //TODO: Send message to network? module
    }

    @Override
    public void writeHandshakeToStream(Stream stream, PeerId peerId) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (ScaleCodecWriter writer = new ScaleCodecWriter(buf)) {
            writer.write(
                    BlockAnnounceHandshakeScaleWriter.getInstance(),
                    handshakeBuilder.getBlockAnnounceHandshake()
            );
        } catch (IOException e) {
            throw new ScaleEncodingException(e);
        }

        log.log(Level.INFO, "Sending handshake to " + peerId);
        stream.writeAndFlush(buf.toByteArray());
    }

    public void writeBlockAnnounceMessage(Stream stream, PeerId peerId, byte[] encodedBlockAnnounceMessage) {
        log.log(Level.FINE, "Sending Block Announce message to peer " + peerId);
        stream.writeAndFlush(encodedBlockAnnounceMessage);
    }

    private void handleBlockAnnounce(byte[] msg, PeerId peerId) {
        BlockAnnounceMessage announce = ScaleUtils.Decode.decode(msg, BlockAnnounceMessageScaleReader.getInstance());
        connectionManager.updatePeer(peerId, announce);
        //TODO Yordan: Do we actually need this since each block has a runtime?
        warpSyncState.syncBlockAnnounce(announce);
        log.log(Level.FINE, "Received block announce for block #" + announce.getHeader().getBlockNumber() +
                " from " + peerId +
                " with hash:" + announce.getHeader().getHash() +
                " parentHash:" + announce.getHeader().getParentHash() +
                " stateRoot:" + announce.getHeader().getStateRoot());

        if (AppBean.getBean(BlockState.class).isInitialized()) {
            //TODO Network improvements: Block requests should be sent to the peer that announced the block itself.
            blockHandler.handleAnnounced(announce.getHeader(), Instant.now(), peerId);
        }
    }
}
