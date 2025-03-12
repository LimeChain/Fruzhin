package com.limechain.consensus.beefy.dto.message;

import lombok.Data;

import java.math.BigInteger;
import java.util.List;

@Data
public class BeefyConsensusMessage {
    private BeefyConsensusMessageFormat format;
    private List<byte[]> authorityPublicKeys;
    private BigInteger authoritySetId;
    private BigInteger disabledAuthority;
    // The 32-byte Merkle Mountain Range (MMR) root payload hash.
    private byte[] mmrRootHash;
}
