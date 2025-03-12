package com.limechain.network.protocol.beefy.messages.justification;

import com.limechain.consensus.beefy.dto.Commitment;
import lombok.Data;

import java.util.List;
import java.util.Optional;

@Data
public class SignedCommitment {
    private Commitment commitment;
    private List<Optional<byte[]>> signatures;
}
