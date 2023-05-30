package com.limechain.sync.warpsync.dto;

import com.limechain.chain.lightsyncstate.Authority;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuthoritySetChange {
    Authority[] authorities;
    BigInteger delay;
}
