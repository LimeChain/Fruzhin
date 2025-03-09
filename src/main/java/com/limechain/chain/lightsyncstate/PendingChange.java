package com.limechain.chain.lightsyncstate;

import io.emeraldpay.polkaj.types.Hash256;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class PendingChange {

    private List<Authority> nextAuthorities;
    private BigInteger delay;
    private BigInteger canonHeight;
    private Hash256 canonHash;
    private DelayKind delayKind;

    public static PendingChange buildForcedAuthoritySetChange(
            List<Authority> nextAuthorities,
            BigInteger delay,
            BigInteger canonHeight,
            Hash256 canonHash,
            BigInteger medianLastFinalized) {

        DelayKind forcedDelayKind = new DelayKind(
                DelayKindEnum.BEST,
                medianLastFinalized
        );

        return new PendingChange(
                nextAuthorities,
                delay,
                canonHeight,
                canonHash,
                forcedDelayKind
        );
    }

    public static PendingChange buildScheduledAuthoritySetChange(
            List<Authority> nextAuthorities,
            BigInteger delay,
            BigInteger canonHeight,
            Hash256 canonHash) {

        DelayKind scheduledDelayKind = new DelayKind(
                DelayKindEnum.FINALIZED,
                null
        );

        return new PendingChange(
                nextAuthorities,
                delay,
                canonHeight,
                canonHash,
                scheduledDelayKind
        );
    }

    public BigInteger getEffectiveNumber() {
        return canonHeight.add(delay);
    }

    public enum DelayKindEnum {
        FINALIZED,
        BEST
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class DelayKind {
        private DelayKindEnum kind;

        // Applies only when `BEST` is selected
        private BigInteger medianLastFinalized;
    }
}