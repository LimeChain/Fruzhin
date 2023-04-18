package com.limechain.network.protocol.blockannounce.scale;

import io.emeraldpay.polkaj.types.Hash256;

public class BlockAnnounceHandShake {
    public int nodeRole;
    public String bestBlock;
    public Hash256 bestBlockHash;
    public Hash256 genesisBlockHash;
}
