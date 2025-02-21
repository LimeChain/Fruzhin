package com.limechain.beefy.dto;

import lombok.Data;

@Data
public class PayloadElement {
    private  byte[] payloadId;
    private  byte[] data;
}
