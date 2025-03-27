package com.limechain.consensus.beefy.dto;

import lombok.Value;

import java.io.Serializable;

@Value
public class PayloadElement implements Serializable {
    BeefyPayloadId payloadId;
    byte[] data;
}
