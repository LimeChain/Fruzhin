package com.limechain.network.protocol.transactions.transactionState;

import lombok.Getter;
import lombok.Setter;

import java.util.PriorityQueue;
import java.util.Queue;

public class TransactionState {
    private static final TransactionState INSTANCE = new TransactionState();

    public static TransactionState getInstance() {
        return INSTANCE;
    }

    @Getter
    @Setter
    private Queue<ValidTransaction> transactionQueue = new PriorityQueue<>();

    private Pool transactionPool = new Pool();

    public void pushTransaction(ValidTransaction validTransaction) {
        transactionQueue.add(validTransaction);
    }

    public ValidTransaction popTransaction() {
        return transactionQueue.poll();
    }

    //TODO Pop with timer

    public ValidTransaction peek() {
        //return s.queue.Peek()
        return transactionQueue.peek();
    }

    public ValidTransaction[] pending() {
        return (ValidTransaction[]) transactionQueue.toArray();
    }

    public ValidTransaction[] pendingInPool() {
        return transactionPool.transactions();
    }

    public boolean exists(ValidTransaction validTransaction) {
        return transactionQueue.contains(validTransaction);
    }

    public void removeExtrinsic(byte[] extrinsic) {
        transactionPool.removeExtrinsic(extrinsic);
        ValidTransaction transactionToBeRemoved = new ValidTransaction(extrinsic);
        transactionQueue.remove(transactionToBeRemoved);
    }

    public void removeExtrinsicFromPool(byte[] extrinsic) {
        transactionPool.removeExtrinsic(extrinsic);
    }

    public byte[] addToPool(ValidTransaction validTransaction) {
        return transactionPool.insert(validTransaction);
    }

    //public void notifyStatus
}
