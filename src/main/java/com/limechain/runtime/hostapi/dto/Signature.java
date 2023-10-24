package com.limechain.runtime.hostapi.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Signature {
    private byte[] publicKeyData;
    private byte[] messageData;
    private byte[] privateKey;
    public Signature(byte[] publicKeyData, byte[] messageData, byte[] privateKey) {
        this.publicKeyData = publicKeyData;
        this.messageData = messageData;
        this.privateKey = privateKey;
    }
}
