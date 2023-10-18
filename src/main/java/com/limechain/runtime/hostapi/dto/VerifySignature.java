package com.limechain.runtime.hostapi.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class VerifySignature {

    private byte[] signatureData;
    private byte[] messageData;
    private byte[] publicKeyData;

    private final Key key;

    public VerifySignature(byte[] signatureData, byte[] messageData, byte[] publicKeyData, Key key) {
        this.signatureData = signatureData;
        this.messageData = messageData;
        this.publicKeyData = publicKeyData;
        this.key = key;
    }

}
