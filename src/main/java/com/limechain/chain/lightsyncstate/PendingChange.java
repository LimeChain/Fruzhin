package com.limechain.chain.lightsyncstate;

import io.emeraldpay.polkaj.types.Hash256;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;
import java.util.List;

@Getter
@Setter
public class PendingChange {
    private List<Authority> nextAuthorities;
    private BigInteger delay;
    private BigInteger canonHeight;
    private Hash256 canonHash;
    private DelayKind delayKind;

    public enum DelayKindEnum {
        FINALIZED,
        BEST
    }

    @Getter
    @Setter
    public static class DelayKind {
        private DelayKindEnum kind;

        // Applies only when `BEST` is selected
        private BigInteger medianLastFinalized;
    }

}