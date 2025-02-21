package com.limechain.beefy.dto;

import com.limechain.runtime.hostapi.dto.Signature;
import lombok.Data;

import java.util.List;

@Data
public class SignedCommitmentWitness {
    private Commitment commitment;
    private  List<Boolean> signedBy;
    private Signature signature;
}
