package com.limechain.network.protocol.transaction.state;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransactionState {
    private static final TransactionState INSTANCE = new TransactionState();
    private final Pool transactionPool = new Pool();
    @Getter
    @Setter
    private Queue<ValidTransaction> transactionQueue = new PriorityQueue<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static TransactionState getInstance() {
        return INSTANCE;
    }

    public void pushTransaction(ValidTransaction validTransaction) {
        transactionQueue.add(validTransaction);
    }

    public ValidTransaction popTransaction() {
        return transactionQueue.poll();
    }

    public ValidTransaction popTransactionWithTimer(long timeout) {
        ValidTransaction validTransaction = popTransaction();
        if (validTransaction != null) return validTransaction;

        Future<ValidTransaction> futureTransaction = popFutureTransaction();

        try {
            return futureTransaction.get(timeout, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            futureTransaction.cancel(true);
        }

        return null;
    }

    @NotNull
    private Future<ValidTransaction> popFutureTransaction() {
        return executor.submit(() -> {
            ValidTransaction transaction = null;
            while (transaction == null) {
                transaction = popTransaction();
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    return null;
                }
            }
            return transaction;
        });
    }

    public ValidTransaction peek() {
        return transactionQueue.peek();
    }

    public ValidTransaction[] pending() {
        return transactionQueue.toArray(new ValidTransaction[0]);
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
