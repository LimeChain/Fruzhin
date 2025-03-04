package com.limechain.beefy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PayloadElement {
    private BeefyPayloadId payloadId;
    private byte[] data;
}
