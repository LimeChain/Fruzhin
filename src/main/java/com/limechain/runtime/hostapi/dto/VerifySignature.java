package com.limechain.runtime.hostapi.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.Objects;

@Getter
@Setter
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VerifySignature verifySig = (VerifySignature) o;
        return Arrays.equals(signatureData, verifySig.signatureData) &&
                Arrays.equals(messageData, verifySig.messageData) &&
                Arrays.equals(publicKeyData, verifySig.publicKeyData) && key == verifySig.key;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(key);
        result = 31 * result + Arrays.hashCode(signatureData);
        result = 31 * result + Arrays.hashCode(messageData);
        result = 31 * result + Arrays.hashCode(publicKeyData);
        return result;
    }
}
