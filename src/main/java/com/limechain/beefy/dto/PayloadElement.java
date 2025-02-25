package com.limechain.beefy.dto;

import lombok.Data;

@Data
public class PayloadElement {
    private BeefyPayloadId payloadId;
    private byte[] data;
}
