package com.limechain.consensus.grandpa.dto;

import com.limechain.consensus.dto.Authority;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GrandpaAuthoritySet {
    BigInteger setId;
    List<Authority> authorities;
}
