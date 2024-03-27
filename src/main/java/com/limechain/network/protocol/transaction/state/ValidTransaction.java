package com.limechain.network.protocol.transaction.state;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ValidTransaction implements Comparable<ValidTransaction> {
    @Getter
    @EqualsAndHashCode.Include
    private final byte[] extrinsic;
    @Getter
    private Validity validity;

    public ValidTransaction(byte[] extrinsic) {
        this.extrinsic = extrinsic;
    }

    public ValidTransaction(byte[] extrinsic, Validity validity) {
        this.extrinsic = extrinsic;
        this.validity = validity;
    }

    public int compareTo(@NotNull ValidTransaction transaction) {
        return this.getValidity().getPriority()
                .compareTo(transaction.getValidity().getPriority());
    }

}
