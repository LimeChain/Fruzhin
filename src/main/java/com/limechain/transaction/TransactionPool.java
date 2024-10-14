package com.limechain.transaction;

import com.limechain.transaction.dto.ValidTransaction;
import com.limechain.utils.HashUtils;

import java.util.HashMap;
import java.util.Map;

public class TransactionPool {

    final Map<byte[], ValidTransaction> transactions = new HashMap<>();

    public ValidTransaction get(byte[] extrinsics) {
        byte[] key = HashUtils.hashWithBlake2b(extrinsics);

        return transactions.get(key);
    }

    public ValidTransaction[] transactions() {
        return transactions.values().toArray(ValidTransaction[]::new);
    }

    public byte[] insert(ValidTransaction validTransaction) {
        byte[] key = HashUtils.hashWithBlake2b(validTransaction.extrinsic().getData());
        transactions.put(key, validTransaction);
        return key;
    }

    public void removeExtrinsic(byte[] extrinsic) {
        byte[] key = HashUtils.hashWithBlake2b(extrinsic);
        transactions.remove(key);
    }

    public int length() {
        return transactions.size();
    }
}
