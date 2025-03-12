package com.limechain.consensus.grandpa.dto;

import com.limechain.consensus.dto.Authority;
import lombok.Value;

import java.math.BigInteger;
import java.util.List;

@Value
public class GrandpaAuthoritySet {

    BigInteger setId;
    List<Authority> authorities;
}
