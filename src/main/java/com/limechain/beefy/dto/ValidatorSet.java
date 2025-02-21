package com.limechain.beefy.dto;

import io.libp2p.core.crypto.PubKey;
import lombok.Data;

import java.util.List;

@Data
public class ValidatorSet {
    List<PubKey> validators;
    private  long id;
}
