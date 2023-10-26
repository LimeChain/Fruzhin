package com.limechain.network.protocol.transaction.state;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TransactionStateTest {
    @Test
    public void testTransactionState() {
        TransactionState transactionState = new TransactionState();
        ValidTransaction[] validTransactions = new ValidTransaction[]{
                new ValidTransaction(new byte[]{'a'}, new Validity(BigInteger.ONE)),
                new ValidTransaction(new byte[]{'b'}, new Validity(BigInteger.valueOf(4))),
                new ValidTransaction(new byte[]{'c'}, new Validity(BigInteger.valueOf(2))),
                new ValidTransaction(new byte[]{'d'}, new Validity(BigInteger.valueOf(17))),
                new ValidTransaction(new byte[]{'e'}, new Validity(BigInteger.valueOf(2))),
        };
        for (ValidTransaction validTransaction:
             validTransactions) {
            transactionState.addToPool(validTransaction);
        }

        ValidTransaction[] pendingInPool = transactionState.pendingInPool();

        Arrays.sort(pendingInPool, Comparator.comparing(a -> new String(a.getExtrinsic())));

        assertArrayEquals(validTransactions, pendingInPool);
        assertNull(transactionState.peek());
    }
}
