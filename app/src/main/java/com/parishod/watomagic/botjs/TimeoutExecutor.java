package com.parishod.watomagic.botjs;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Utilidad para ejecutar tareas con timeout duro.
 */
public final class TimeoutExecutor {

    private TimeoutExecutor() {
        // Utility class
    }

    public static <T> T executeWithTimeout(Callable<T> task, long timeoutMs)
            throws TimeoutException, ExecutionException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<T> future = executor.submit(task);
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExecutionException(e);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        } finally {
            executor.shutdownNow();
        }
    }
}
