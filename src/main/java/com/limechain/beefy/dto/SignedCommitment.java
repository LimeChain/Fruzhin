package com.limechain.beefy.dto;

import com.limechain.runtime.hostapi.dto.Signature;
import lombok.Data;

import java.util.List;
import java.util.Optional;

@Data
public class SignedCommitment {
    private Commitment commitment;
    private List<Optional<Signature>> signature;
}
