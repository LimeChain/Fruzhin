package com.limechain.network.protocol.grandpa.messages.commit;

import io.emeraldpay.polkaj.types.Hash256;
import lombok.Data;

import java.math.BigInteger;

@Data
public class Vote {
    private Hash256 blockHash;
    private BigInteger blockNumber;
}
