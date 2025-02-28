package com.limechain.sync.warpsync.dto;

import com.limechain.chain.lightsyncstate.Authority;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;

@Getter
@Setter
public abstract class AuthoritySetChange {

    private List<Authority> authorities;
    // TODO
    //  current implementation doesn't take in use the delay field
    //  We should probably rename the applicationBlockNumber to originBlockNumber
    //  Then we may also add a method getApplicationBlockNumber which adds the delay to the originBlockNumber
    //  Then we should use this getApplicationBlockNumber in the comparator
    //  Origin block number should be used to check which scheduled authority set changes should be applied after a block
    //  is finalized where the finalizedBlockNumber >= getApplicationBlockNumber() and finalizedBlock is descendant of the origin block
    private BigInteger delay;
    private BigInteger applicationBlockNumber;

    protected AuthoritySetChange(List<Authority> authorities, BigInteger delay, BigInteger announceBlockNumber) {
        this.authorities = authorities;
        this.delay = delay;
        this.applicationBlockNumber = announceBlockNumber.add(delay);
    }

    // TODO: change the comparator to use newly created getApplicationBlockNumber
    public static Comparator<AuthoritySetChange> getComparator() {
        return Comparator.comparing(AuthoritySetChange::getApplicationBlockNumber);
    }
}
