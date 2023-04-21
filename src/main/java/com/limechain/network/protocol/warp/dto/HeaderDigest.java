package com.limechain.network.protocol.warp.dto;

import lombok.Setter;

@Setter
public class HeaderDigest {
    public DigestType type;
    public ConsensusEngineId id;
    public byte[] message;

}
