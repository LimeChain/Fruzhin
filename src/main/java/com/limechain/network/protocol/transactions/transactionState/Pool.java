package com.limechain.network.protocol.transactions.transactionState;

import com.limechain.utils.HashUtils;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
public class Pool {
    Map<byte[], ValidTransaction> transactions = new HashMap<>();

    public ValidTransaction get(byte[] extrinisics) {
        byte[] key = HashUtils.hashWithBlake2b(extrinisics);
        return transactions.get(key);
    }

    public ValidTransaction[] transactions() {
        return transactions.values()
                .stream()
                .filter(value -> value instanceof ValidTransaction)
                .toArray(ValidTransaction[]::new);
    }

    public byte[] insert(ValidTransaction validTransaction){
        byte[] key = HashUtils.hashWithBlake2b(validTransaction.getExtrinsic());
        transactions.put(key, validTransaction);
        return key;
    }

    public void removeExtrinsic(byte[] extrinsic){
        byte[] key = HashUtils.hashWithBlake2b(extrinsic);
        transactions.remove(key);
    }

    public int length(){
        return transactions.size();
    }
}
