package com.limechain.transaction.dto;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record ValidTransaction(@EqualsAndHashCode.Include Extrinsic extrinsic, @Nullable TransactionValidity transactionValidity)
        implements Comparable<ValidTransaction> {

    public int compareTo(@NotNull ValidTransaction transaction) {
        return this.transactionValidity().getPriority()
                .compareTo(transaction.transactionValidity().getPriority());
    }

}
