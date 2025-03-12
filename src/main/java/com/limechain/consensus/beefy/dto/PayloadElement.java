package com.limechain.consensus.beefy.dto;

import lombok.Value;

@Value
public class PayloadElement {
    BeefyPayloadId payloadId;
    byte[] data;
}
