package com.limechain.network.protocol.grandpa.scale;

import lombok.Data;

import java.math.BigInteger;

@Data
public class NeighbourMessage {
    private int version;
    private BigInteger round;

    private BigInteger setId;

    private long lastFinalizedBlock;
}
