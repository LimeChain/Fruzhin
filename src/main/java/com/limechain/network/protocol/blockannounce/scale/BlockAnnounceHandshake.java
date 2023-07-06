package com.limechain.network.protocol.blockannounce.scale;

import io.emeraldpay.polkaj.types.Hash256;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BlockAnnounceHandshake {
    public int nodeRole;
    public String bestBlock;
    public Hash256 bestBlockHash;
    public Hash256 genesisBlockHash;

    @Override
    public String toString() {
        return "BlockAnnounceHandShake{" +
                "nodeRole=" + nodeRole +
                ", bestBlock='" + bestBlock + '\'' +
                ", bestBlockHash=" + bestBlockHash +
                ", genesisBlockHash=" + genesisBlockHash +
                '}';
    }
}
