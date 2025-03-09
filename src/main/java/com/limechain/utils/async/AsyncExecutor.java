package com.limechain.utils.async;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.annotation.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

public class AsyncExecutor {

    private final ExecutorService executorService;

    private AsyncExecutor(int poolSize, @Nullable String name) {
        if (name != null) {
            ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
                    .setNameFormat(name + "-%d")
                    .build();

            this.executorService = Executors.newFixedThreadPool(poolSize, namedThreadFactory);
        } else
            this.executorService = Executors.newFixedThreadPool(poolSize);
    }

    public static AsyncExecutor withPoolSize(int poolSize) {
        return new AsyncExecutor(poolSize, null);
    }

    public static AsyncExecutor withPoolSize(int poolSize, String name) {
        return new AsyncExecutor(poolSize, name);
    }

    public static AsyncExecutor withSingleThread() {
        return new AsyncExecutor(1, null);
    }

    public static AsyncExecutor withSingleThread(String name) {
        return new AsyncExecutor(1, name);
    }

    public <T> CompletableFuture<T> executeAsync(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, executorService);
    }

    public void executeAndForget(Runnable task) {
        executorService.execute(task);
    }

    //TODO Yordan: Create a centralized retry function here.
}
