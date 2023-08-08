package com.limechain.network.protocol.grandpa.messages.commit;

import io.emeraldpay.polkaj.types.Hash256;
import lombok.Data;

@Data
public class Vote {
    private Hash256 blockHash;
    private long blockNumber;
}
