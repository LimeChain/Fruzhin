package com.limechain.network.protocol.blockannounce;

import com.limechain.network.protocol.BaseController;
import io.libp2p.core.Stream;

public class BlockAnnounceController extends BaseController<BlockAnnounceEngine> {

    public BlockAnnounceController(Stream stream) {
        super(stream, new BlockAnnounceEngine());
    }

    /**
     * Sends a block announce message over the controller stream.
     */
    public void sendBlockAnnounceMessage(byte[] encodedBlockAnnounceMessage) {
        engine.writeBlockAnnounceMessage(stream, stream.remotePeerId(), encodedBlockAnnounceMessage);
    }
}
