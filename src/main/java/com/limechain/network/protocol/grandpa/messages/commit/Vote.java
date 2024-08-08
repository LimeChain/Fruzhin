package com.limechain.network.protocol.grandpa.messages.commit;

import com.limechain.polkaj.Hash256;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Vote {
    private Hash256 blockHash;
    private BigInteger blockNumber;
}
