package com.limechain.network.protocol.blockannounce;

import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandShake;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandshakeScaleReader;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.libp2p.core.PeerId;
import io.libp2p.core.Stream;

import java.util.HashMap;
import java.util.Map;

public class BlockAnnounceEngine {
    private final Map<PeerId, BlockAnnounceHandShake> peerHandshakes = new HashMap<>();

    public void receiveRequest(byte[] msg, PeerId peerId, Stream stream) {
        var hasKey = peerHandshakes.containsKey(peerId);
        System.out.println("Message length: " + msg.length);
        if (!hasKey && msg.length != 1) {
            System.out.println("Peer opened a notification stream with us?");
            System.out.println("Decoding Handshake");
            ScaleCodecReader reader = new ScaleCodecReader(msg);
            BlockAnnounceHandShake handShake = reader.read(new BlockAnnounceHandshakeScaleReader());
            peerHandshakes.put(peerId, handShake);
        } else {
            System.out.println("Decoding BlockAnnounce");
//            ScaleCodecReader reader = new ScaleCodecReader(msg);
//            BlockAnnounceMessage announce = reader.read(new BlockAnnounceMessageScaleReader());
        }

        // TODO: Send message to network? module
    }
}
