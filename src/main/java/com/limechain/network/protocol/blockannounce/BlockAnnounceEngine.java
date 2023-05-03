package com.limechain.network.protocol.blockannounce;

import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandshake;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandshakeScaleReader;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandshakeScaleWriter;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceMessage;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceMessageScaleReader;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.types.Hash256;
import io.libp2p.core.PeerId;
import io.libp2p.core.Stream;
import lombok.extern.java.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

@Log
public class BlockAnnounceEngine {
    private final Map<PeerId, BlockAnnounceHandshake> peerHandshakes = new HashMap<>();

    private static final int HANDSHAKE_LENGTH = 69;

    /* Polkadot handshake */
    //TODO Get handshake data from latest snapshot release
    private final BlockAnnounceHandshake handshake = new BlockAnnounceHandshake() {{
        nodeRole = 4;
        bestBlockHash = Hash256.from("0x7b22fc4469863c9671686c189a3238708033d364a77ba8d83e78777e7563f346");
        bestBlock = "0";
        genesisBlockHash = Hash256.from(
                "0x7b22fc4469863c9671686c189a3238708033d364a77ba8d83e78777e7563f346");
    }};


    public void receiveRequest(byte[] msg, PeerId peerId, Stream stream) {
        var hasKey = peerHandshakes.containsKey(peerId);

        //Decode handshake from Polkadot
        if (!hasKey && msg.length == HANDSHAKE_LENGTH) {
            ScaleCodecReader reader = new ScaleCodecReader(msg);
            BlockAnnounceHandshake handshake = reader.read(new BlockAnnounceHandshakeScaleReader());
            peerHandshakes.put(peerId, handshake);
            log.log(Level.INFO, "Received handshake from " + peerId + "\n" +
                    handshake);

            writeHandshakeToStream(stream, peerId);
            return;
        }

        if (hasKey && msg.length == HANDSHAKE_LENGTH) {
            log.log(Level.INFO, "Received existing handshake from " + peerId);

            writeHandshakeToStream(stream, peerId);
            return;
        }

        if (hasKey) {
            ScaleCodecReader reader = new ScaleCodecReader(msg);
            BlockAnnounceMessage announce = reader.read(new BlockAnnounceMessageScaleReader());

            log.log(Level.INFO, "Received block announce: \n" + announce);
            return;
        }

        log.log(Level.WARNING, "No handshake for block announce message");

        // TODO: Send message to network? module
    }

    public void writeHandshakeToStream(Stream stream, PeerId peerId) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (ScaleCodecWriter writer = new ScaleCodecWriter(buf)) {
            writer.write(new BlockAnnounceHandshakeScaleWriter(), handshake);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        log.log(Level.INFO, "Sending handshake to " + peerId);
        stream.writeAndFlush(buf.toByteArray());
    }

    public void removePeerHandshake(PeerId peerId){
        peerHandshakes.remove(peerId);
    }
}
