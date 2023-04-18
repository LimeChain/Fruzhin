package com.limechain.network.protocol.blockannounce;

import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandShake;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandshakeScaleReader;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.libp2p.core.PeerId;
import io.libp2p.core.Stream;

public class BlockAnnounceEngine {
    public void receiveRequest(byte[] msg, PeerId peerId, Stream stream) {
        System.out.println("Decoding");
        ScaleCodecReader reader = new ScaleCodecReader(msg);
        BlockAnnounceHandShake handShake = reader.read(new BlockAnnounceHandshakeScaleReader());
        System.out.println("Decoded");

        // TODO: Send message to network? module
    }
}
