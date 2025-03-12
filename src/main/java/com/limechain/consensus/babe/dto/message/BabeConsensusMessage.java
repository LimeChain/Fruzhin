package com.limechain.consensus.babe.dto.message;

import lombok.Data;

import java.math.BigInteger;

@Data
public class BabeConsensusMessage {
    private EpochData nextEpochData;
    private BigInteger disabledAuthority;
    private EpochDescriptor nextEpochDescriptor;
    private BabeConsensusMessageFormat format;
}
