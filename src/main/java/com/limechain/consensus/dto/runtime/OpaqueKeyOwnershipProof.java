package com.limechain.consensus.dto.runtime;

import lombok.Data;

@Data
public class OpaqueKeyOwnershipProof {
    private byte[] proof;
}
