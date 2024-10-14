package com.limechain.transaction.dto;

import lombok.Data;

import java.math.BigInteger;

@Data
public class TransactionValidity {

    private BigInteger priority;
    private byte[][] requires;
    private byte[][] provides;
    private BigInteger longevity;
    private Boolean propagate;
}
