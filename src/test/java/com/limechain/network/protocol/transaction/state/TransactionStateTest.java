package com.limechain.network.protocol.transaction.state;

import com.limechain.transaction.TransactionState;
import com.limechain.transaction.dto.Extrinsic;
import com.limechain.transaction.dto.ValidTransaction;
import com.limechain.transaction.dto.TransactionValidity;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TransactionStateTest {
    @Test
    void testTransactionState() {
        TransactionState transactionState = TransactionState.getInstance();
        ValidTransaction[] validTransactions = new ValidTransaction[]{
                new ValidTransaction(new Extrinsic(new byte[]{'a'}), new TransactionValidity()),
                new ValidTransaction(new Extrinsic(new byte[]{'b'}), new TransactionValidity()),
                new ValidTransaction(new Extrinsic(new byte[]{'c'}), new TransactionValidity()),
                new ValidTransaction(new Extrinsic(new byte[]{'d'}), new TransactionValidity()),
                new ValidTransaction(new Extrinsic(new byte[]{'e'}), new TransactionValidity())
        };
        for (ValidTransaction validTransaction :
                validTransactions) {
            transactionState.addToPool(validTransaction);
        }

        ValidTransaction[] pendingInPool = transactionState.pendingInPool();

        Arrays.sort(pendingInPool, Comparator.comparing(a -> new String(a.extrinsic().getData())));

        assertArrayEquals(validTransactions, pendingInPool);
        assertNull(transactionState.peek());
    }
}
