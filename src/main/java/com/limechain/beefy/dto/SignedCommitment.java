package com.limechain.beefy.dto;

import io.libp2p.core.crypto.PubKey;
import lombok.Data;

import java.util.List;
import java.util.Optional;

@Data
public class SignedCommitment {
    private Commitment commitment;
    private List<Optional<PubKey>> signatures;
}
