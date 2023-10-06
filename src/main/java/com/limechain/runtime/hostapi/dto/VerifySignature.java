package com.limechain.runtime.hostapi.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifySignature {

    private byte[] signatureData;
    private byte[] messageData;
    private byte[] publicKeyData;

    final Key key;

    public VerifySignature(byte[] signatureData, byte[] messageData, byte[] publicKeyData, Key key) {
        this.signatureData = signatureData;
        this.messageData = messageData;
        this.publicKeyData = publicKeyData;
        this.key = key;
    }
}
