package com.limechain.network.protocol.beefy.messages.justification;

import com.limechain.consensus.beefy.dto.Commitment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Optional;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignedCommitment {
    private Commitment commitment;
    private List<Optional<byte[]>> signatures;
}
