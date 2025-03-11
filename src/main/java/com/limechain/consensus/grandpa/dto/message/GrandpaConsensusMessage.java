package com.limechain.consensus.grandpa.dto.message;

import com.limechain.consensus.dto.Authority;
import lombok.Data;

import java.math.BigInteger;
import java.util.List;

@Data
public class GrandpaConsensusMessage {
    private GrandpaConsensusMessageFormat format;
    private List<Authority> authorities;
    private BigInteger disabledAuthority;
    private BigInteger delay;
    // this is denoted as 'm' in the polkadot spec
    private BigInteger additionalOffset;
}
