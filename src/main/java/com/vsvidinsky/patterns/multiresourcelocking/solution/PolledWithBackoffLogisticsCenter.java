package com.vsvidinsky.patterns.multiresourcelocking.solution;

import com.vsvidinsky.patterns.multiresourcelocking.model.Item;
import com.vsvidinsky.patterns.multiresourcelocking.LogisticsCenter;
import com.vsvidinsky.patterns.multiresourcelocking.model.TransferResult;
import com.vsvidinsky.patterns.multiresourcelocking.Warehouse;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class PolledWithBackoffLogisticsCenter implements LogisticsCenter {
    private static final long OPERATION_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(5);
    private static final long INITIAL_BACKOFF_MS = 1;
    private static final long MAX_BACKOFF_MS = 9;

    @Override
    public TransferResult transfer(Warehouse source, Warehouse destination, Item item) {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(OPERATION_TIMEOUT_MS);
        long backoff = INITIAL_BACKOFF_MS;

        while (System.nanoTime() < deadline) {
            if (source.getLock().tryLock()) {
                try {
                    if (destination.getLock().tryLock()) {
                        try {
                            if (!source.hasItem(item)) {
                                return TransferResult.ITEM_NOT_FOUND;
                            }
                            source.remove(item);
                            destination.add(item);
                            return TransferResult.SUCCESS;
                        } finally {
                            destination.getLock().unlock();
                        }
                    }
                } finally {
                    source.getLock().unlock();
                }
            }
            try {
                Thread.sleep(ThreadLocalRandom.current().nextLong(INITIAL_BACKOFF_MS, backoff + 1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return TransferResult.INTERRUPTED;
            }
            backoff = Math.min(backoff * 2, MAX_BACKOFF_MS);
        }

        return TransferResult.TIMEOUT;
    }
}
