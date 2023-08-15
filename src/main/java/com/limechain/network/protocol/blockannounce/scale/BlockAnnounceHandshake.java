package com.limechain.network.protocol.blockannounce.scale;

import io.emeraldpay.polkaj.types.Hash256;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class BlockAnnounceHandshake {
    private int nodeRole;
    private BigInteger bestBlock;
    private Hash256 bestBlockHash;
    private Hash256 genesisBlockHash;

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
