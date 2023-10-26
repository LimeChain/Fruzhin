package com.limechain.network.protocol.transaction.state;

import lombok.Getter;

import java.util.Comparator;

public class ValidTransaction {
    @Getter
    private byte[] extrinsic;
    @Getter
    private Validity validity;

    public ValidTransaction(byte[] extrinsic){
        this.extrinsic = extrinsic;
    }

    public ValidTransaction(byte[] extrinsic, Validity validity){
        this.extrinsic = extrinsic;
        this.validity = validity;
    }

    static class ValidTransactionComparator implements Comparator<ValidTransaction> {
        public int compare(ValidTransaction validTransaction, ValidTransaction otherValidTransaction) {
            return validTransaction.getValidity().getPriority()
                    .compareTo(otherValidTransaction.getValidity().getPriority());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ValidTransaction other = (ValidTransaction) o;

        if (!extrinsic.equals(other.getExtrinsic())) return false;
        return true;
    }

}
