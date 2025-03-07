package com.limechain.beefy.dto;

import lombok.Value;

@Value
public class PayloadElement {
    BeefyPayloadId payloadId;
    byte[] data;
}
