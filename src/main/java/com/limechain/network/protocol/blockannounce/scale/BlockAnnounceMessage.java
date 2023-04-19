package com.limechain.network.protocol.blockannounce.scale;

public class BlockAnnounceMessage {
    public BlockHeader header;
    public boolean isBestBlock;

    @Override
    public String toString() {
        return "BlockAnnounceMessage{" +
                "header=" + header +
                ", isBestBlock=" + isBestBlock +
                '}';
    }
}
