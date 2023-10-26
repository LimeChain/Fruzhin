package com.limechain.network.protocol.transactions.transactionState;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Validity {
    private BigInteger priority;
    private byte[][] requires;
    private byte[][] provides;
    private BigInteger longevity;
    private boolean propagate;

    public Validity(BigInteger priority){
        this.priority = priority;
    }
}
