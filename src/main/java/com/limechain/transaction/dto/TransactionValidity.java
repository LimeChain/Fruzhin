package com.limechain.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionValidity {

    private BigInteger priority;
    private byte[][] requires;
    private byte[][] provides;
    private BigInteger longevity;
    private Boolean propagate;
}
