package com.limechain.network.protocol.beefy.messages.justification;

import com.limechain.consensus.beefy.dto.Commitment;
import lombok.Value;

import java.util.List;
import java.util.Optional;

@Value
public class SignedCommitment {
    Commitment commitment;
    List<Optional<byte[]>> signatures;
}
