package com.limechain.network.protocol.blockannounce;

import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandShake;
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
    private final Map<PeerId, BlockAnnounceHandShake> peerHandshakes = new HashMap<>();

    private final int HANDSHAKE_LENGTH = 69;

    public void receiveRequest(byte[] msg, PeerId peerId, Stream stream) {
        var hasKey = peerHandshakes.containsKey(peerId);

        System.out.println("Message length:: " + msg.length);

        //Decode handshake from Polkadot
        if (!hasKey && msg.length == HANDSHAKE_LENGTH) {
            System.out.println("Peer opened a notification stream with us?");
            System.out.println("Decoding Handshake");
            ScaleCodecReader reader = new ScaleCodecReader(msg);
            BlockAnnounceHandShake handShake = reader.read(new BlockAnnounceHandshakeScaleReader());
            peerHandshakes.put(peerId, handShake);
            sendHandshake(stream, 0);
            return;
        }

        if (hasKey && msg.length == HANDSHAKE_LENGTH) {
            System.out.println("Handshake already initiated!");
            sendHandshake(stream, 0);
            return;
        }

        if (hasKey) {
            System.out.println("Decoding BlockAnnounce");
            ScaleCodecReader reader = new ScaleCodecReader(msg);
            BlockAnnounceMessage announce = reader.read(new BlockAnnounceMessageScaleReader());
            System.out.println(announce);
            return;
        }

        log.log(Level.WARNING, "No handshake for block announce message");

        // TODO: Send message to network? module
    }

    public void sendHandshake(Stream stream, int hostReciever) {
        //            /* Polkadot handshake */
        var polkadotHandshake = new BlockAnnounceHandShake() {{
            nodeRole = 4;
            bestBlockHash = Hash256.from("0x7b22fc4469863c9671686c189a3238708033d364a77ba8d83e78777e7563f346");
            bestBlock = "0";
            genesisBlockHash = Hash256.from(
                    "0x7b22fc4469863c9671686c189a3238708033d364a77ba8d83e78777e7563f346");
        }};

        /* Gossamer handshake */
        var gossamerHandshake = new BlockAnnounceHandShake() {{
            nodeRole = 4;
            bestBlockHash = Hash256.from("0xb6d36a6766363567d2a385c8b5f9bd93b223b8f42e54aa830270edcf375f4d63");
            bestBlock = "0";
            genesisBlockHash = Hash256.from(
                    "0xb6d36a6766363567d2a385c8b5f9bd93b223b8f42e54aa830270edcf375f4d63");
        }};

        BlockAnnounceHandShake[] handshakes = new BlockAnnounceHandShake[]{polkadotHandshake, gossamerHandshake};

        BlockAnnounceHandShake handshake = handshakes[hostReciever];

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (ScaleCodecWriter writer = new ScaleCodecWriter(buf)) {
            writer.write(new BlockAnnounceHandshakeScaleWriter(), handshake);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Writing handshake to stream from engine");
        stream.writeAndFlush(buf.toByteArray());
    }
}
