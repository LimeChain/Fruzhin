package com.limechain.grandpa.state;

import com.limechain.chain.lightsyncstate.Authority;
import lombok.Value;

import java.math.BigInteger;
import java.util.List;

@Value
public class AuthoritySet {

    BigInteger setId;
    List<Authority> authorities;
}
