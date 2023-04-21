package com.limechain.network.protocol.warp.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

@Setter
@Getter
public class HeaderDigest {
    private DigestType type;
    private ConsensusEngine id;
    private byte[] message;

    @Override
    public String toString() {
        return "HeaderDigest{" +
                "type=" + type +
                ", id=" + id +
                ", message=" + Arrays.toString(message) +
                '}';
    }
}
