package com.limechain.network.protocol.blockannounce.scale;

import io.emeraldpay.polkaj.types.Hash256;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
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
