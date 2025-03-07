package com.limechain.sync.warpsync.dto;

import com.limechain.chain.lightsyncstate.Authority;
import io.emeraldpay.polkaj.types.Hash256;

import java.math.BigInteger;
import java.util.List;

public class ForcedAuthoritySetChange extends AuthoritySetChange {

    public ForcedAuthoritySetChange(List<Authority> authorities,
                                    Hash256 originBlockHash,
                                    BigInteger originBlockNumber,
                                    BigInteger additionalOffset,
                                    BigInteger delay) {

        super(authorities, originBlockHash, originBlockNumber, delay.add(additionalOffset));
    }
}
