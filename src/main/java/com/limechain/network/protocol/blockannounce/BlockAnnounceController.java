package com.limechain.network.protocol.blockannounce;

public class BlockAnnounceController {
    protected BlockAnnounceEngine engine = new BlockAnnounceEngine();
//    protected final Stream stream;

//    public BlockAnnounceController(Stream stream) {
//        this.stream = stream;
//    }

    public void sendHandshake() {
        /*engine.writeHandshakeToStream(stream, stream.remotePeerId())*/;
    }
}
