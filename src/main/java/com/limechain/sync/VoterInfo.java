package com.limechain.sync;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

@Getter
@Setter
@AllArgsConstructor
public class VoterInfo {
    int position;
    BigInteger weight;
}
