package com.limechain.transaction;

import com.limechain.transaction.dto.Extrinsic;
import com.limechain.transaction.dto.ValidTransaction;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.java.Log;
import org.jetbrains.annotations.NotNull;

import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

@Log
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TransactionState {
    private static final TransactionState INSTANCE = new TransactionState();
    private final TransactionPool transactionPool = new TransactionPool();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    @Getter
    @Setter
    private Queue<ValidTransaction> transactionQueue = new PriorityQueue<>();

    public static TransactionState getInstance() {
        return INSTANCE;
    }

    public void pushTransaction(ValidTransaction validTransaction) {
        transactionQueue.add(validTransaction);
    }

    public ValidTransaction popTransaction() {
        return transactionQueue.poll();
    }

    public ValidTransaction popTransactionWithTimer(long timeout) throws InterruptedException {
        ValidTransaction validTransaction = popTransaction();
        if (validTransaction != null) return validTransaction;

        Future<ValidTransaction> futureTransaction = popFutureTransaction();

        try {
            return futureTransaction.get(timeout, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            log.severe("Error while waiting for transaction: " + e.getMessage());
        } catch (TimeoutException e) {
            futureTransaction.cancel(true);
        } catch (InterruptedException e) {
            if (Thread.interrupted())
                throw new InterruptedException();
        }

        return null;
    }

    @NotNull
    private Future<ValidTransaction> popFutureTransaction() {
        return executor.submit(() -> {
            ValidTransaction transaction = null;
            while (transaction == null) {
                transaction = popTransaction();
                Thread.sleep(50);
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
        ValidTransaction transactionToBeRemoved = new ValidTransaction(new Extrinsic(extrinsic), null);
        transactionQueue.remove(transactionToBeRemoved);
    }

    public void removeExtrinsicFromPool(byte[] extrinsic) {
        transactionPool.removeExtrinsic(extrinsic);
    }

    public byte[] addToPool(ValidTransaction validTransaction) {
        return transactionPool.insert(validTransaction);
    }
}
