package com.limechain.sync.warpsync.dto;

import com.limechain.chain.lightsyncstate.Authority;
import io.emeraldpay.polkaj.types.Hash256;

import java.math.BigInteger;
import java.util.List;

//TODO: maybe remove
public class ScheduledAuthoritySetChange extends AuthoritySetChange {
    public ScheduledAuthoritySetChange(List<Authority> authorities,
                                       Hash256 originBlockHash,
                                       BigInteger originBlockNumber,
                                       BigInteger delay) {

        super(authorities, originBlockHash, originBlockNumber, delay);
    }
}
